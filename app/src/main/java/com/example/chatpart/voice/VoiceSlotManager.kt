package com.example.chatpart.voice

import android.content.Context
import android.util.Log
import com.example.chatpart.api.MiniMaxVoiceCloneManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Voice Slot Manager - 核心管理类
 *
 * 负责管理用户的语音槽位，包括：
 * - 克隆声音到槽位
 * - 删除槽位声音 (同时删除 MiniMax 服务器上的 voice)
 * - 获取槽位使用情况
 * - 同步本地与云端状态
 */
class VoiceSlotManager(
    private val context: Context,
    private val miniMaxClient: MiniMaxVoiceCloneManager
) {
    companion object {
        private const val TAG = "VoiceSlotManager"
        private const val PREFS_NAME = "voice_slots"
        private const val KEY_SLOTS = "slots"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    // ========== 用户标识 ==========

    /**
     * 获取用户唯一标识 (需要外部提供 userIdentifier)
     * 外部调用时传入 GoogleAuthManager 的用户信息
     */
    fun getUserIdentifier(googleUserId: String, firebaseUid: String): UserIdentifier {
        return UserIdentifier(
            dbUserId = PlaceholderDbUserId.PENDING,  // TODO: 后端提供后替换
            googleUserId = googleUserId,
            firebaseUid = firebaseUid
        )
    }

    // ========== 槽位存储 (本地 SharedPreferences) ==========

    /**
     * 保存槽位列表到本地
     */
    private fun saveSlots(slots: List<VoiceSlot>) {
        val json = gson.toJson(slots)
        prefs.edit().putString(KEY_SLOTS, json).apply()
        Log.d(TAG, "Saved ${slots.size} slots to local storage")
    }

    /**
     * 从本地加载槽位列表
     */
    private fun loadSlots(): List<VoiceSlot> {
        val json = prefs.getString(KEY_SLOTS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<VoiceSlot>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load slots: ${e.message}")
            emptyList()
        }
    }

    // ========== 槽位查询 ==========

    /**
     * 获取用户所有槽位
     *
     * @param userIdentifier 用户标识
     * @return 槽位列表
     */
    suspend fun getUserSlots(userIdentifier: UserIdentifier): List<VoiceSlot> =
        withContext(Dispatchers.IO) {
            val allSlots = loadSlots()
            // 过滤当前用户的槽位
            allSlots.filter { it.userId == userIdentifier.effectiveUserId }
        }

    /**
     * 获取槽位使用情况
     *
     * @param userIdentifier 用户标识
     * @param totalSlots 总槽位数 (默认 1)
     * @return SlotUsageSummary
     */
    suspend fun getSlotUsage(
        userIdentifier: UserIdentifier,
        totalSlots: Int = 1
    ): SlotUsageSummary = withContext(Dispatchers.IO) {
        val userSlots = getUserSlots(userIdentifier)
        val usedSlots = userSlots.count { it.isActive }
        val allSlots = (0 until totalSlots).map { index ->
            userSlots.find { it.slotIndex == index }
                ?: createEmptySlot(userIdentifier.effectiveUserId, index)
        }

        SlotUsageSummary(
            usedSlots = usedSlots,
            totalSlots = totalSlots,
            slots = allSlots
        )
    }

    /**
     * 获取已使用的槽位数
     */
    suspend fun getUsedSlotCount(userIdentifier: UserIdentifier): Int =
        withContext(Dispatchers.IO) {
            getUserSlots(userIdentifier).count { it.isActive }
        }

    /**
     * 检查是否可以创建新槽位
     *
     * @param userIdentifier 用户标识
     * @param totalSlots 总槽位数
     * @return true 如果可以创建新槽位
     */
    suspend fun canCreateNewSlot(
        userIdentifier: UserIdentifier,
        totalSlots: Int
    ): Boolean {
        val used = getUsedSlotCount(userIdentifier)
        return used < totalSlots
    }

    /**
     * 获取下一个可用槽位索引
     *
     * @param userIdentifier 用户标识
     * @param totalSlots 总槽位数
     * @return 可用槽位索引，null 如果没有可用槽位
     */
    suspend fun getNextAvailableSlotIndex(
        userIdentifier: UserIdentifier,
        totalSlots: Int
    ): Int? = withContext(Dispatchers.IO) {
        val userSlots = getUserSlots(userIdentifier)
        val usedIndices = userSlots.filter { it.isActive }.map { it.slotIndex }.toSet()

        for (i in 0 until totalSlots) {
            if (i !in usedIndices) {
                return@withContext i
            }
        }
        return@withContext null
    }

    // ========== 槽位操作 ==========

    /**
     * 克隆声音到下一个可用槽位
     *
     * @param audioFile 录音文件
     * @param userIdentifier 用户标识
     * @param totalSlots 总槽位数 (默认 1)
     * @return Result<VoiceSlot>
     */
    suspend fun cloneVoiceToNextSlot(
        audioFile: File,
        userIdentifier: UserIdentifier,
        totalSlots: Int = 1
    ): Result<VoiceSlot> = withContext(Dispatchers.IO) {
        // 1. 检查是否有可用槽位
        val slotIndex = getNextAvailableSlotIndex(userIdentifier, totalSlots)
            ?: return@withContext Result.failure(Exception("声音slot已满，请购买更多slot"))

        // 2. 调用 MiniMax API 克隆
        val voiceIdResult = miniMaxClient.cloneVoice(
            audioFile = audioFile,
            userIdentifier = userIdentifier,
            slotIndex = slotIndex
        )

        voiceIdResult.fold(
            onSuccess = { voiceId ->
                // 3. 创建或更新槽位
                val slot = VoiceSlot(
                    slotId = "slot_${userIdentifier.effectiveUserId}_${slotIndex}",
                    userId = userIdentifier.effectiveUserId,
                    voiceId = voiceId,
                    status = VoiceSlotStatus.ACTIVE,
                    slotIndex = slotIndex,
                    createdAt = System.currentTimeMillis(),
                    lastUsedAt = System.currentTimeMillis()
                )

                // 4. 保存到本地
                val allSlots = loadSlots().toMutableList()
                val existingIndex = allSlots.indexOfFirst {
                    it.slotId == slot.slotId && it.userId == userIdentifier.effectiveUserId
                }

                if (existingIndex >= 0) {
                    allSlots[existingIndex] = slot
                } else {
                    allSlots.add(slot)
                }

                saveSlots(allSlots)
                Log.d(TAG, "Cloned voice to slot ${slot.slotId}: $voiceId")

                Result.success(slot)
            },
            onFailure = { error ->
                Log.e(TAG, "Clone failed: ${error.message}")
                Result.failure(error)
            }
        )
    }

    /**
     * 删除槽位中的声音
     * 同时删除 MiniMax 服务器上的 voice
     *
     * @param slotId 槽位 ID
     * @param userIdentifier 用户标识
     * @return Result<Unit>
     */
    suspend fun deleteSlotVoice(
        slotId: String,
        userIdentifier: UserIdentifier
    ): Result<Unit> = withContext(Dispatchers.IO) {
        // 1. 找到槽位
        val allSlots = loadSlots()
        val slot = allSlots.find {
            it.slotId == slotId && it.userId == userIdentifier.effectiveUserId
        } ?: return@withContext Result.failure(Exception("槽位不存在"))

        // 2. 如果有 voiceId，先删除 MiniMax 服务器上的 voice
        if (slot.voiceId != null) {
            val deleteResult = miniMaxClient.deleteMiniMaxVoice(slot.voiceId)
            deleteResult.fold(
                onSuccess = {
                    Log.d(TAG, "Deleted MiniMax voice: ${slot.voiceId}")
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to delete MiniMax voice: ${error.message}")
                    // 继续删除本地槽位，即使服务器删除失败
                }
            )
        }

        // 3. 更新本地槽位为空
        val updatedSlot = slot.copy(
            voiceId = null,
            characterId = null,
            status = VoiceSlotStatus.EMPTY,
            lastUsedAt = System.currentTimeMillis()
        )

        val updatedSlots = allSlots.map {
            if (it.slotId == slotId && it.userId == userIdentifier.effectiveUserId) {
                updatedSlot
            } else {
                it
            }
        }

        saveSlots(updatedSlots)
        Log.d(TAG, "Deleted voice from slot: ${slot.slotId}")

        Result.success(Unit)
    }

    /**
     * 绑定角色到槽位
     *
     * @param slotId 槽位 ID
     * @param userIdentifier 用户标识
     * @param characterId 角色 ID
     * @return Result<VoiceSlot>
     */
    suspend fun bindCharacterToSlot(
        slotId: String,
        userIdentifier: UserIdentifier,
        characterId: String
    ): Result<VoiceSlot> = withContext(Dispatchers.IO) {
        val allSlots = loadSlots()
        val slot = allSlots.find {
            it.slotId == slotId && it.userId == userIdentifier.effectiveUserId
        } ?: return@withContext Result.failure(Exception("槽位不存在"))

        val updatedSlot = slot.copy(
            characterId = characterId,
            lastUsedAt = System.currentTimeMillis()
        )

        val updatedSlots = allSlots.map {
            if (it.slotId == slotId && it.userId == userIdentifier.effectiveUserId) {
                updatedSlot
            } else {
                it
            }
        }

        saveSlots(updatedSlots)
        Result.success(updatedSlot)
    }

    /**
     * 初始化用户的槽位 (如果没有的话创建空槽位)
     *
     * @param userIdentifier 用户标识
     * @param totalSlots 总槽位数
     */
    suspend fun initializeSlotsIfNeeded(
        userIdentifier: UserIdentifier,
        totalSlots: Int = 1
    ) = withContext(Dispatchers.IO) {
        val allSlots = loadSlots()
        val userSlots = allSlots.filter { it.userId == userIdentifier.effectiveUserId }

        if (userSlots.isEmpty()) {
            // 创建空槽位
            val newSlots = (0 until totalSlots).map { index ->
                createEmptySlot(userIdentifier.effectiveUserId, index)
            }
            saveSlots(allSlots + newSlots)
            Log.d(TAG, "Initialized ${newSlots.size} empty slots for user ${userIdentifier.effectiveUserId}")
        }
    }

    // ========== 同步 ==========

    /**
     * 同步 MiniMax API 的 voice 列表
     * 更新本地槽位状态
     *
     * @param userIdentifier 用户标识
     * @return Result<List<String>> MiniMax 上的 voice_id 列表
     */
    suspend fun syncWithMiniMax(userIdentifier: UserIdentifier): Result<List<String>> =
        withContext(Dispatchers.IO) {
            miniMaxClient.fetchClonedVoices().map { voiceIds ->
                Log.d(TAG, "MiniMax has ${voiceIds.size} voices")

                // 标记本地槽位中哪些 voice 还在 MiniMax 上
                val allSlots = loadSlots()
                val userSlots = allSlots.filter { it.userId == userIdentifier.effectiveUserId }

                val updatedSlots = userSlots.map { slot ->
                    if (slot.voiceId != null && slot.voiceId !in voiceIds) {
                        // voice 在 MiniMax 上不存在了，可能已过期
                        slot.copy(status = VoiceSlotStatus.EXPIRED)
                    } else {
                        slot
                    }
                }

                // 保存更新
                val nonUserSlots = allSlots.filter { it.userId != userIdentifier.effectiveUserId }
                saveSlots(nonUserSlots + updatedSlots)

                voiceIds
            }
        }
}

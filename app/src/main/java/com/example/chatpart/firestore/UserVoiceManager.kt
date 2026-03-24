package com.example.chatpart.firestore

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.chatpart.data.ClonedVoice
import com.example.chatpart.data.SlotStatus
import com.example.chatpart.data.UserVoiceData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.IOException

/**
 * Firestore 操作异常类型
 */
sealed class FirestoreError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class NetworkError(message: String, cause: Throwable? = null) : FirestoreError("Network error: $message", cause)
    class PermissionError(message: String, cause: Throwable? = null) : FirestoreError("Permission denied: $message", cause)
    class NotFoundError(message: String, cause: Throwable? = null) : FirestoreError("Not found: $message", cause)
    class ServerError(message: String, cause: Throwable? = null) : FirestoreError("Server error: $message", cause)
    class QuotaExceededError(message: String, cause: Throwable? = null) : FirestoreError("Quota exceeded: $message", cause)
    class UnknownError(message: String, cause: Throwable? = null) : FirestoreError("Unknown error: $message", cause)
    class SlotFullError : FirestoreError("Voice slot is full. Please delete an existing voice first.")
}

/**
 * 用户声音数据管理器
 * 处理所有与用户声音相关的 Firestore 操作
 * 包含重试逻辑和全面的错误处理
 *
 * SECURITY:
 * - 使用 Transaction 确保操作的原子性
 * - 使用 FieldValue.increment() 避免客户端信任问题
 * - uid 用于账户隔离和所有权验证
 */
class UserVoiceManager(val uid: String) { // SECURITY: uid is public for ownership verification

    companion object {
        private const val TAG = "UserVoiceManager"

        // 重试配置
        private const val MAX_RETRIES = 3
        private const val INITIAL_DELAY_MS = 1000L
        private const val MAX_DELAY_MS = 4000L
        private const val TIMEOUT_MS = 30000L

        /**
         * 带指数退避的重试机制
         * 适用于网络不稳定的场景
         */
        suspend fun <T> withRetry(
            maxRetries: Int = MAX_RETRIES,
            initialDelay: Long = INITIAL_DELAY_MS,
            maxDelay: Long = MAX_DELAY_MS,
            operationName: String = "operation",
            block: suspend () -> T
        ): Result<T> = runCatching {
            var lastException: Throwable? = null
            var currentDelay = initialDelay

            repeat(maxRetries) { attempt ->
                try {
                    return@runCatching block()
                } catch (e: Exception) {
                    lastException = e
                    Log.w(TAG, "$operationName failed (attempt ${attempt + 1}/$maxRetries): ${e.message}")

                    // 检查是否应该重试
                    if (shouldRetry(e) && attempt < maxRetries - 1) {
                        delay(currentDelay)
                        currentDelay = (currentDelay * 2).coerceAtMost(maxDelay)
                    } else {
                        throw e
                    }
                }
            }

            // 理论上不会到达这里，但为了类型安全
            block()
        }

        /**
         * 判断异常是否应该重试
         */
        private fun shouldRetry(exception: Throwable): Boolean {
            return when (exception) {
                is IOException -> true  // 网络IO错误，可重试
                is FirebaseFirestoreException -> {
                    // 只有临时性错误才重试
                    exception.code == FirebaseFirestoreException.Code.UNAVAILABLE ||
                    exception.code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ||
                    exception.code == FirebaseFirestoreException.Code.INTERNAL
                }
                else -> false
            }
        }

        /**
         * 将 Firebase 异常转换为更友好的错误类型
         */
        fun parseFirestoreException(e: Throwable): FirestoreError {
            Log.e(TAG, "parseFirestoreException: ${e::class.simpleName} - ${e.message}")
            if (e is FirebaseFirestoreException) {
                Log.e(TAG, "Firebase error code: ${e.code}, message: ${e.message}")
            }
            return when (e) {
                is FirebaseFirestoreException -> when (e.code) {
                    FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                        FirestoreError.PermissionError("Access denied", e)
                    FirebaseFirestoreException.Code.NOT_FOUND ->
                        FirestoreError.NotFoundError("Document not found", e)
                    FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED ->
                        FirestoreError.QuotaExceededError("Quota exceeded", e)
                    FirebaseFirestoreException.Code.UNAVAILABLE,
                    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                        FirestoreError.NetworkError("Service temporarily unavailable", e)
                    FirebaseFirestoreException.Code.ABORTED,
                    FirebaseFirestoreException.Code.INTERNAL,
                    FirebaseFirestoreException.Code.UNKNOWN ->
                        FirestoreError.ServerError("Server error: ${e.code}", e)
                    FirebaseFirestoreException.Code.ALREADY_EXISTS ->
                        FirestoreError.UnknownError("Voice already exists", e)
                    FirebaseFirestoreException.Code.FAILED_PRECONDITION ->
                        FirestoreError.UnknownError("Operation failed: ${e.message}", e)
                    FirebaseFirestoreException.Code.CANCELLED ->
                        FirestoreError.UnknownError("Operation cancelled", e)
                    else -> FirestoreError.UnknownError("Error (${e.code}): ${e.message}", e)
                }
                is IOException -> FirestoreError.NetworkError("Network connection failed", e)
                else -> FirestoreError.UnknownError("Error: ${e::class.simpleName} - ${e.message}", e)
            }
        }
    }

    private val userDoc get() = FirestoreManager.userDocument(uid)

    init {
        Log.d(TAG, "UserVoiceManager initialized for uid: $uid")
    }

    // ==================== 用户初始化 ====================

    /**
     * 确保用户文档存在（首次登录时创建）
     * 如果文档已存在，则不会覆盖
     * 包含重试逻辑，网络不稳定时会自动重试
     */
    suspend fun ensureUserExists(email: String): Result<UserVoiceData> {
        return withRetry(
            maxRetries = MAX_RETRIES,
            operationName = "ensureUserExists"
        ) {
            val snapshot = userDoc.get().await()
            if (snapshot.exists()) {
                snapshot.toObject<UserVoiceData>()!!
            } else {
                // 创建新用户文档
                val newUser = UserVoiceData(email = email)
                userDoc.set(newUser).await()
                Log.d(TAG, "Created new user document for: $email")
                newUser
            }
        }.onFailure { e ->
            Log.e(TAG, "ensureUserExists failed: ${e.message}")
        }
    }

    // ==================== Slot 管理 ====================

    /**
     * 获取用户 slot 状态
     * 包含重试逻辑，网络不稳定时会自动重试
     */
    suspend fun getSlotStatus(): Result<SlotStatus> {
        return withRetry(
            maxRetries = MAX_RETRIES,
            operationName = "getSlotStatus"
        ) {
            val doc = userDoc.get().await()
            val userData = doc.toObject<UserVoiceData>()
                ?: return@withRetry SlotStatus(used = 0, limit = 1)

            SlotStatus(
                used = userData.slotUsed,
                limit = userData.slotLimit
            )
        }
    }

    /**
     * 获取用户 slot 状态（带缓存的快速版本）
     * 用于 UI 初始加载，不进行重试
     */
    suspend fun getSlotStatusQuick(): SlotStatus {
        return try {
            val doc = userDoc.get().await()
            val userData = doc.toObject<UserVoiceData>()
            SlotStatus(
                used = userData?.slotUsed ?: 0,
                limit = userData?.slotLimit ?: 1
            )
        } catch (e: Exception) {
            Log.w(TAG, "getSlotStatusQuick failed: ${e.message}")
            SlotStatus(used = 0, limit = 1)  // 返回默认值
        }
    }

    /**
     * 检查是否可以克隆新声音
     */
    suspend fun canCloneVoice(): Boolean {
        return getSlotStatus().getOrNull()?.let { !it.isFull } ?: false
    }

    /**
     * 检查网络连接状态
     * @param context Android Context
     * @return true if network is available
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // ==================== 声音 CRUD ====================

    /**
     * 添加克隆声音
     * SECURITY (P2): 使用原子操作防止竞态条件
     * - 使用 Transaction 确保 slot 检查和添加是原子的
     * - 使用 FieldValue.increment() 避免客户端计算的 slotUsed
     * - 使用 FieldValue.arrayUnion() 添加 voice 而非读取-修改-写入
     */
    suspend fun addClonedVoice(voice: ClonedVoice): Result<Unit> {
        return withRetry(
            maxRetries = MAX_RETRIES,
            operationName = "addClonedVoice"
        ) {
            Log.d(TAG, "addClonedVoice: using atomic transaction for voiceId=${voice.voiceId}, id=${voice.id}")

            // 使用 Firestore Transaction 进行原子性操作
            FirebaseFirestore.getInstance().runTransaction { transaction ->
                val snapshot = transaction.get(userDoc)
                val userData = snapshot.toObject<UserVoiceData>()
                    ?: throw FirebaseFirestoreException(
                        "User document not found",
                        FirebaseFirestoreException.Code.NOT_FOUND
                    )

                // 在 Transaction 内检查 slot（原子性）
                if (userData.slotUsed >= userData.slotLimit) {
                    throw FirebaseFirestoreException(
                        "Voice slot is full",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION
                    )
                }

                // 使用 arrayUnion 添加 voice（原子操作）
                // 使用 increment 更新 slotUsed（服务端计算，避免客户端信任问题）
                transaction.update(
                    userDoc,
                    "voices", FieldValue.arrayUnion(voice),
                    "slotUsed", FieldValue.increment(1),
                    "updatedAt", System.currentTimeMillis()
                )

                Log.d(TAG, "addClonedVoice: transaction success for voiceId=${voice.voiceId}")
                null
            }.await()

            Log.d(TAG, "addClonedVoice: success for voiceId=${voice.voiceId}")
        }.mapCatching { }
          .recoverCatching { e ->
            Log.e(TAG, "addClonedVoice: failed with ${e::class.simpleName} - ${e.message}")
            // 如果是 slot 满的错误，返回特定的 SlotFullError
            if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                return Result.failure(FirestoreError.SlotFullError())
            }
            throw parseFirestoreException(e)
          }
    }

    /**
     * 删除克隆声音
     * SECURITY (P2): 使用原子操作防止竞态条件
     * - 使用 Transaction 确保声音存在检查和删除是原子的
     * - 使用 FieldValue.arrayRemove() 和 FieldValue.increment(-1) 进行原子更新
     */
    suspend fun deleteClonedVoice(voiceId: String): Result<Unit> {
        return withRetry(
            maxRetries = MAX_RETRIES,
            operationName = "deleteClonedVoice"
        ) {
            Log.d(TAG, "deleteClonedVoice: using atomic transaction for voiceId=$voiceId")

            // 使用 Firestore Transaction 进行原子性操作
            FirebaseFirestore.getInstance().runTransaction { transaction ->
                val snapshot = transaction.get(userDoc)
                val userData = snapshot.toObject<UserVoiceData>()
                    ?: throw FirebaseFirestoreException(
                        "User not found",
                        FirebaseFirestoreException.Code.NOT_FOUND
                    )

                // 在 Transaction 内检查声音是否存在（原子性）
                val voiceExists = userData.voices.any { it.voiceId == voiceId }
                if (!voiceExists) {
                    throw FirebaseFirestoreException(
                        "Voice not found: $voiceId",
                        FirebaseFirestoreException.Code.NOT_FOUND
                    )
                }

                // ⚠️ DO NOT use FieldValue.arrayRemove(voiceToDelete) here.
                // arrayRemove requires deep-equality object matching. Firestore
                // deserialization can produce slightly different objects (Long vs Int,
                // missing fields, etc.) causing arrayRemove to silently do nothing.
                //
                // Instead: filter the list manually and overwrite the whole field.
                val updatedVoices = userData.voices.filter { it.voiceId != voiceId }
                transaction.update(
                    userDoc,
                    "voices", updatedVoices,
                    "slotUsed", FieldValue.increment(-1),
                    "updatedAt", System.currentTimeMillis()
                )

                Log.d(TAG, "deleteClonedVoice: transaction success for voiceId=$voiceId")
                Unit
            }.await()

            Log.d(TAG, "deleteClonedVoice: success for voiceId=$voiceId")
        }.mapCatching { }
          .recoverCatching { e ->
            Log.e(TAG, "deleteClonedVoice failed: ${e::class.simpleName} - ${e.message}")
            throw parseFirestoreException(e)
          }
    }

    /**
     * 获取用户所有克隆声音
     * 包含重试逻辑
     */
    suspend fun getClonedVoices(): Result<List<ClonedVoice>> {
        return withRetry(
            maxRetries = MAX_RETRIES,
            operationName = "getClonedVoices"
        ) {
            val doc = userDoc.get().await()
            val userData = doc.toObject<UserVoiceData>()
            userData?.voices ?: emptyList()
        }.onFailure { e ->
            Log.e(TAG, "getClonedVoices failed: ${e.message}")
        }
    }

    /**
     * 获取特定角色的声音
     */
    suspend fun getVoiceByCharacterId(characterId: String): ClonedVoice? {
        return getClonedVoices().getOrNull()?.find { it.characterId == characterId }
    }

    /**
     * 更新声音的最后使用时间
     * 包含重试逻辑
     */
    suspend fun updateLastUsedTime(voiceId: String): Result<Unit> {
        return withRetry(
            maxRetries = MAX_RETRIES,
            operationName = "updateLastUsedTime"
        ) {
            val doc = userDoc.get().await()
            val userData = doc.toObject<UserVoiceData>()
                ?: throw FirebaseFirestoreException(
                    "User not found",
                    FirebaseFirestoreException.Code.NOT_FOUND
                )

            val updatedVoices = userData.voices.map { voice ->
                if (voice.voiceId == voiceId) {
                    voice.copy(lastUsedAt = System.currentTimeMillis())
                } else {
                    voice
                }
            }

            userDoc.update(
                "voices", updatedVoices,
                "updatedAt", System.currentTimeMillis()
            ).await()
            Unit
        }.onFailure { e ->
            Log.e(TAG, "updateLastUsedTime failed: ${e.message}")
        }
    }

    // ==================== 实时监听 ====================

    /**
     * 监听用户数据变化
     * 返回 Flow，用户数据变化时会自动推送新数据
     * 包含错误重连机制
     */
    fun observeUserData(): Flow<Result<UserVoiceData?>> = callbackFlow {
        var retryCount = 0
        val maxRetries = MAX_RETRIES

        fun subscribe() {
            val listener = userDoc.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "observeUserData error: ${error.message}")
                    // 尝试自动重连
                    if (retryCount < maxRetries) {
                        retryCount++
                        Log.d(TAG, "Retrying observeUserData (attempt $retryCount)")
                        kotlinx.coroutines.MainScope().launch {
                            delay(INITIAL_DELAY_MS * retryCount)
                            subscribe()
                        }
                    } else {
                        trySend(Result.failure(parseFirestoreException(error)))
                        close(error)
                    }
                    return@addSnapshotListener
                }

                retryCount = 0  // 重置重试计数
                val userData = snapshot?.toObject<UserVoiceData>()
                trySend(Result.success(userData))
            }
        }

        subscribe()
        awaitClose { /* listener会自动清理 */ }
    }

    /**
     * 监听 slot 状态变化
     * 返回 Flow，包含错误处理
     */
    fun observeSlotStatus(): Flow<Result<SlotStatus>> = callbackFlow {
        var retryCount = 0
        val maxRetries = MAX_RETRIES

        fun subscribe() {
            val listener = userDoc.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "observeSlotStatus error: ${error.message}")
                    // 尝试自动重连
                    if (retryCount < maxRetries) {
                        retryCount++
                        Log.d(TAG, "Retrying observeSlotStatus (attempt $retryCount)")
                        kotlinx.coroutines.MainScope().launch {
                            delay(INITIAL_DELAY_MS * retryCount)
                            subscribe()
                        }
                    } else {
                        trySend(Result.failure(parseFirestoreException(error)))
                        close(error)
                    }
                    return@addSnapshotListener
                }

                retryCount = 0  // 重置重试计数
                val userData = snapshot?.toObject<UserVoiceData>()
                val status = SlotStatus(
                    used = userData?.slotUsed ?: 0,
                    limit = userData?.slotLimit ?: 1
                )
                trySend(Result.success(status))
            }
        }

        subscribe()
        awaitClose { /* listener会自动清理 */ }
    }

    // ==================== 管理员功能 (预留) ====================

    /**
     * 增加用户 slot 配额（管理员功能）
     * 实际应用中应在服务端实现此逻辑
     */
    suspend fun increaseSlotLimit(additional: Int): Result<Unit> {
        return withRetry(
            maxRetries = MAX_RETRIES,
            operationName = "increaseSlotLimit"
        ) {
            userDoc.update(
                "slotLimit", FieldValue.increment(additional.toLong()),
                "updatedAt", System.currentTimeMillis()
            ).await()
            Unit
        }.onFailure { e ->
            Log.e(TAG, "increaseSlotLimit failed: ${e.message}")
        }
    }

    /**
     * 将 FirestoreError 转换为用户友好的中文错误消息
     */
    fun getUserFriendlyErrorMessage(error: Throwable): String {
        return when (error) {
            is FirestoreError.NetworkError -> "网络连接失败，请检查网络后重试"
            is FirestoreError.PermissionError -> "权限不足，无法访问数据"
            is FirestoreError.NotFoundError -> "数据不存在"
            is FirestoreError.ServerError -> "服务器繁忙，请稍后重试"
            is FirestoreError.QuotaExceededError -> "配额已用完，请稍后重试"
            is FirestoreError.SlotFullError -> "声音槽位已满，请先删除一个声音"
            is FirestoreError.UnknownError -> "操作失败: ${error.message}，请重试"
            else -> "操作失败，请重试"
        }
    }
}

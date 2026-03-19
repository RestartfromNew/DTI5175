package com.example.chatpart.voice

/**
 * Voice Slot 系统 - 用户语音槽位数据模型
 *
 * 每个用户有独立的 voice slot，可以克隆自己的声音
 * 槽位状态: EMPTY -> CLONING -> ACTIVE -> (可能) EXPIRED
 */

/**
 * 槽位状态
 */
enum class VoiceSlotStatus {
    EMPTY,      // 空槽位，可克隆
    CLONING,    // 克隆中
    ACTIVE,     // 已激活
    EXPIRED     // 已过期 (MiniMax 7天不使用会自动删除)
}

/**
 * VoiceSlot - 语音槽位
 *
 * @param slotId 唯一标识: "slot_${userHash}_${slotIndex}"
 * @param userId 用户 ID (dbUserId 或 userHash)
 * @param voiceId MiniMax voice_id (null = 空槽位)
 * @param characterId 绑定的角色 ID (可选)
 * @param status 槽位状态
 * @param createdAt 创建时间戳
 * @param lastUsedAt 最后使用时间戳
 * @param slotIndex 槽位索引 (0, 1, 2...)
 */
data class VoiceSlot(
    val slotId: String,
    val userId: String,
    val voiceId: String? = null,
    val characterId: String? = null,
    val status: VoiceSlotStatus = VoiceSlotStatus.EMPTY,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = System.currentTimeMillis(),
    val slotIndex: Int = 0
) {
    /**
     * 是否为空槽位
     */
    val isEmpty: Boolean
        get() = status == VoiceSlotStatus.EMPTY || voiceId == null

    /**
     * 是否可用 (可克隆)
     */
    val isAvailable: Boolean
        get() = status == VoiceSlotStatus.EMPTY && voiceId == null

    /**
     * 是否已激活
     */
    val isActive: Boolean
        get() = status == VoiceSlotStatus.ACTIVE && voiceId != null
}

/**
 * 创建空槽位
 */
fun createEmptySlot(userId: String, slotIndex: Int): VoiceSlot {
    return VoiceSlot(
        slotId = "slot_${userId}_${slotIndex}",
        userId = userId,
        status = VoiceSlotStatus.EMPTY,
        slotIndex = slotIndex
    )
}

/**
 * 槽位使用情况摘要
 */
data class SlotUsageSummary(
    val usedSlots: Int,
    val totalSlots: Int,
    val slots: List<VoiceSlot>
) {
    val remainingSlots: Int
        get() = totalSlots - usedSlots

    val isFull: Boolean
        get() = usedSlots >= totalSlots

    val canClone: Boolean
        get() = usedSlots < totalSlots
}

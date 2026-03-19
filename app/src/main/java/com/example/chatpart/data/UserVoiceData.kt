package com.example.chatpart.data

/**
 * Firestore 用户声音数据结构
 */
data class UserVoiceData(
    val email: String = "",
    val slotLimit: Int = 1,
    val slotUsed: Int = 0,
    val voices: List<ClonedVoice> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 单个克隆声音记录
 */
data class ClonedVoice(
    val id: String = "",
    val voiceId: String = "",
    val characterId: String = "",
    val characterName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = System.currentTimeMillis()
)

/**
 * Slot 状态
 */
data class SlotStatus(
    val used: Int,
    val limit: Int,
    val available: Int = limit - used
) {
    val isFull: Boolean get() = used >= limit
    val percentage: Float get() = if (limit > 0) used.toFloat() / limit else 0f
}

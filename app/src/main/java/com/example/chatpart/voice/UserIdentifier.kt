package com.example.chatpart.voice

import java.security.MessageDigest

/**
 * 用户标识符
 *
 * ⚠️ 后端尚未提供 dbUserId，暂时使用以下方案：
 * - 方案 A (后端完成后): 使用后端提供的 dbUserId
 * - 方案 B (过渡期): 使用 googleUserId + firebaseUid 组合作为 userHash
 *
 * TODO: 后端写好后替换为实际的用户 ID
 *
 * @param dbUserId 后端用户 ID (后端提供后替换)
 * @param googleUserId Google Sign-In ID
 * @param firebaseUid Firebase UID
 */
data class UserIdentifier(
    val dbUserId: String,         // ⭐ 后端用户 ID (后端提供后替换)
    val googleUserId: String,     // Google Sign-In ID
    val firebaseUid: String       // Firebase UID
) {
    /**
     * 获取用于 voice_id 生成的 userHash
     * 后端提供 dbUserId 后改为: dbUserId
     * 过渡期使用: googleUserId + firebaseUid
     */
    val userHash: String
        get() = "$googleUserId$firebaseUid".hash256().take(8).lowercase()

    /**
     * 获取实际使用的用户 ID
     * 后端提供 dbUserId 后直接使用 dbUserId
     * 过渡期使用 userHash
     */
    val effectiveUserId: String
        get() = if (dbUserId == PlaceholderDbUserId.PENDING) {
            userHash
        } else {
            dbUserId
        }
}

/**
 * 过渡期占位符
 */
object PlaceholderDbUserId {
    const val PENDING = "pending_db_id"
    // TODO: 后端写好后删除此占位符
}

/**
 * 用户语音配额
 *
 * @param userId 用户 ID (dbUserId 或 userHash)
 * @param googleUserId Google Sign-In ID
 * @param firebaseUid Firebase UID
 * @param maxSlots 免费槽位数 (默认 1)
 * @param purchasedSlots 额外购买的槽位数
 */
data class UserVoiceEntitlement(
    val userId: String,
    val googleUserId: String,
    val firebaseUid: String,
    val maxSlots: Int = 1,           // 免费槽位数
    val purchasedSlots: Int = 0       // 额外购买的槽位数
) {
    /**
     * 总槽位数 = 免费 + 购买
     */
    val totalSlots: Int
        get() = maxSlots + purchasedSlots

    /**
     * 是否可以创建新槽位
     */
    fun canCreateNewSlot(usedSlots: Int): Boolean {
        return usedSlots < totalSlots
    }
}

/**
 * 创建默认配额 (免费用户 1 slot)
 */
fun createDefaultEntitlement(userIdentifier: UserIdentifier): UserVoiceEntitlement {
    return UserVoiceEntitlement(
        userId = userIdentifier.effectiveUserId,
        googleUserId = userIdentifier.googleUserId,
        firebaseUid = userIdentifier.firebaseUid,
        maxSlots = 1,
        purchasedSlots = 0
    )
}

// ========== 工具函数 ==========

/**
 * 计算字符串的 SHA-256 哈希
 */
private fun String.hash256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(this.toByteArray())
    return hash.joinToString("") { "%02x".format(it) }
}

/**
 * 获取用户唯一标识字符串
 * 用于 Firestore 文档 ID
 */
fun UserIdentifier.toFirestoreId(): String {
    return "user_${effectiveUserId}"
}

package com.example.chatpart.firestore

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

/**
 * Firestore 全局管理器
 * 单例模式，全局只需一个实例
 */
object FirestoreManager {

    val instance: FirebaseFirestore by lazy {
        // 启用离线持久化（可选，适合弱网环境）
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()

        FirebaseFirestore.getInstance().apply {
            firestoreSettings = settings
        }
    }

    // 集合路径常量
    const val USERS_COLLECTION = "users"

    // 文档路径：users/{uid}
    fun userDocument(uid: String) = instance.collection(USERS_COLLECTION).document(uid)
}

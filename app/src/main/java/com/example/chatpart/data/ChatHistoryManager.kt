package com.example.chatpart.data

import android.content.Context

/**
 * Simple chat history persistence using SharedPreferences
 */
class ChatHistoryManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("chat_history", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_MESSAGES = "messages"
        private const val MAX_MESSAGES = 100 // Limit stored messages
    }

    fun saveMessages(messages: List<ChatMessageData>) {
        // Only save the last MAX_MESSAGES
        val trimmedMessages = messages.takeLast(MAX_MESSAGES)
        val json = trimmedMessages.joinToString(";;") { msg ->
            "${msg.text};;${msg.isFromUser};;${msg.isVoice};;${msg.voiceDurationSec}"
        }
        prefs.edit().putString(KEY_MESSAGES, json).apply()
    }

    fun loadMessages(): List<ChatMessageData> {
        val json = prefs.getString(KEY_MESSAGES, "") ?: ""
        if (json.isEmpty()) return emptyList()
        return json.split(";;").mapNotNull { parts ->
            val arr = parts.split(";;")
            if (arr.size >= 2) {
                try {
                    ChatMessageData(
                        text = arr[0],
                        isFromUser = arr[1].toBoolean(),
                        isVoice = arr.getOrNull(2)?.toBoolean() ?: false,
                        voiceDurationSec = arr.getOrNull(3)?.toIntOrNull() ?: 0
                    )
                } catch (e: Exception) {
                    null
                }
            } else null
        }
    }

    fun clearMessages() {
        prefs.edit().clear().apply()
    }
}

/**
 * Data class for chat message serialization
 */
data class ChatMessageData(
    val text: String,
    val isFromUser: Boolean,
    val isVoice: Boolean = false,
    val voiceDurationSec: Int = 0
)

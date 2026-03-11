package com.example.chatpart.data

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Chat history persistence using SharedPreferences
 * Supports saving messages per character/session
 */
class ChatHistoryManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("chat_history", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_MESSAGE_COUNT = "chat_count"
        private const val MAX_MESSAGES = 100 // Limit stored messages per chat
    }

    // Save messages for a specific character
    fun saveMessages(characterId: String, messages: List<ChatMessageData>) {
        val trimmedMessages = messages.takeLast(MAX_MESSAGES)
        // Use \u0001 (SOH) between messages, \u0002 (STX) between fields — never appears in user text
        val json = trimmedMessages.joinToString("\u0001") { msg ->
            "${msg.text}\u0002${msg.isFromUser}\u0002${msg.isVoice}\u0002${msg.voiceDurationSec}"
        }
        prefs.edit().putString("messages_$characterId", json).apply()

        // Update chat count and timestamp
        val count = prefs.getInt(KEY_MESSAGE_COUNT, 0) + 1
        prefs.edit()
            .putInt("count_$characterId", trimmedMessages.size)
            .putLong("timestamp_$characterId", System.currentTimeMillis())
            .putString("last_message_$characterId", trimmedMessages.lastOrNull()?.text ?: "")
            .apply()
    }

    // Load messages for a specific character
    fun loadMessages(characterId: String): List<ChatMessageData> {
        val json = prefs.getString("messages_$characterId", "") ?: ""
        if (json.isEmpty()) return emptyList()
        return json.split("\u0001").mapNotNull { parts ->
            val arr = parts.split("\u0002")
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

    // Get all chat sessions (characters that have chat history)
    fun getAllChatSessions(): List<ChatSession> {
        val sessions = mutableListOf<ChatSession>()
        val count = prefs.getInt(KEY_MESSAGE_COUNT, 0)

        // Get all keys that start with "messages_"
        prefs.all.forEach { (key, _) ->
            if (key.startsWith("messages_")) {
                val characterId = key.removePrefix("messages_")
                val messageCount = prefs.getInt("count_$characterId", 0)
                val timestamp = prefs.getLong("timestamp_$characterId", 0)
                val lastMessage = prefs.getString("last_message_$characterId", "") ?: ""

                if (messageCount > 0) {
                    sessions.add(
                        ChatSession(
                            characterId = characterId,
                            lastMessage = lastMessage,
                            messageCount = messageCount,
                            timestamp = timestamp
                        )
                    )
                }
            }
        }

        // Sort by timestamp (most recent first)
        return sessions.sortedByDescending { it.timestamp }
    }

    // Clear messages for a specific character
    fun clearMessages(characterId: String) {
        prefs.edit()
            .remove("messages_$characterId")
            .remove("count_$characterId")
            .remove("timestamp_$characterId")
            .remove("last_message_$characterId")
            .apply()
    }

    // Clear all chat history
    fun clearAllMessages() {
        val keysToRemove = mutableListOf<String>()
        prefs.all.forEach { (key, _) ->
            if (key.startsWith("messages_") || key.startsWith("count_") ||
                key.startsWith("timestamp_") || key.startsWith("last_message_")) {
                keysToRemove.add(key)
            }
        }
        prefs.edit().apply {
            keysToRemove.forEach { remove(it) }
            apply()
        }
    }

    // Legacy method for backward compatibility
    fun saveMessages(messages: List<ChatMessageData>) {
        saveMessages("default", messages)
    }

    fun loadMessages(): List<ChatMessageData> {
        return loadMessages("default")
    }

    fun clearMessages() {
        clearMessages("default")
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

/**
 * Data class for chat session (used in History screen)
 */
data class ChatSession(
    val characterId: String,
    val lastMessage: String,
    val messageCount: Int,
    val timestamp: Long
) {
    fun getFormattedTime(): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60 * 1000 -> "Just now"
            diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)} min ago"
            diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)} hours ago"
            diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)} days ago"
            else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
        }
    }
}

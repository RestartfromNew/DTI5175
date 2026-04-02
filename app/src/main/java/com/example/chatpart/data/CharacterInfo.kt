package com.example.chatpart.data

/**
 * Lightweight character descriptor used by VoiceManagementScreen.
 * Avoids importing Profile or DefaultChatbots, keeping the screen decoupled.
 */
data class CharacterInfo(
    /** "assistant" / "teacher" / "coding" / profile.id */
    val id: String,
    val name: String,
    /** Emoji for display in the assign dialog */
    val emoji: String,
    /** True for the 3 built-in bots */
    val isDefault: Boolean
)

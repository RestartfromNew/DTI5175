package com.example.chatpart.data

import com.example.chatpart.domain.Profile
import com.example.chatpart.domain.Message
import com.example.chatpart.domain.Role

object Prompt {
    fun buildPrompt (
        p: Profile,
        memorySummary: String?
    ): String {
        val coreInstruction = "You are not an AI assistant, you are the character describe below. Stay in character at all times."
        val doRules = p.doRules.joinToString("\n-", prefix = "- ")
        val dontRules = p.dontRules.joinToString("\n-", prefix = "- ")
        val speakStyle = p.speakStyle.joinToString(separator = ", ")
        val systemRules = """
            - Reply in the SAME language as the user uses.
            - If user writes in English, reply in English.
            - If user writes in Chinese, reply in Chinese.
            - If user writes in French, reply in French.
            - When user mentions personal preferences, habits, or important info,
              add a hidden tag at the end: [MEM: brief fact in the SAME language as user's message]
            - Tags are invisible to users but important for memory.
            - If new info contradicts previous memory, prioritize new info.
        """.trimIndent()
        val memoryBlock = if (!memorySummary.isNullOrBlank()) {
            "### Shared Memories with User:\n$memorySummary\n"
        } else ""

        return """
            $coreInstruction
            
            # Character Profile
            - Name: ${p.name}
            - Gender: ${p.gender}
            - Relationship to User: ${p.relationship}
            - Personality: ${p.personality}
            - Speak Style: $speakStyle
            
            # Background
            ${p.background}
            
            # Rules to Follow
            $doRules
            
            # Constraints (NEVER DO THESE)
            $dontRules
            
            # System Rules
            $systemRules
            
            $memoryBlock
            
            Please respond as ${p.name} based on the context above.
        """.trimIndent()
    }

    fun buildDialog (history: List<Message>, userText: String): List<Pair<String, String>> {
        val turns = history.takeLast(20).map {
            val role = if (it.role == Role.USER ) "user"
                       else "assistant"
            role to it.content
        }
        return turns + ("user" to userText)

    }
}
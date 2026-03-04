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
            - Follow the user's using language.
            - Whenever the user mentions their personal preferences, living habits, significant 
              experiences or specific dietary restrictions, you must add a tag at the end of your reply.：[MEM: 简短的中文事实]。
            - Tags should follow Chinese.
            - Tags are only for record, which are not shown to users.
            - The tags are invisible to the user, but they are crucial for your memory system. Please make sure to follow through.
            - If the new information provided by the user contradicts the previous memory (Summary), 
              then the new information should take precedence and this change should be confirmed in the reply.
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
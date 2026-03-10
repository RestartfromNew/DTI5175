package com.example.chatpart.domain

import com.example.chatpart.llm.LlmClient

class ChatEngine (
    private val llmClient: LlmClient
) {
    suspend fun chat(
        systemPrompt: String,
        userText: String
    ): Result {
        return llmClient.reply(systemPrompt, userText)
    }
}
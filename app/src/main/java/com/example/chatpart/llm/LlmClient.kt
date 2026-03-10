package com.example.chatpart.llm

import com.example.chatpart.domain.Result

interface LlmClient {
    suspend fun reply(
        systemPrompt: String,
        userText: String
    ): Result
}
package com.example.chatpart.llm

import android.util.Log
import com.example.chatpart.BuildConfig
import com.example.chatpart.domain.Result
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content

class AITest : LlmClient {

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.API_Key
    )

    override suspend fun reply(systemPrompt: String, userText: String): Result {
        return try {
            val finalPrompt = "$systemPrompt \nPlease show the reply including [EMOTION: 状态] tag. Select one from: HAPPY, SAD, ANGRY, SHY, NEUTRAL. Reply in the SAME language as the user's message."

            val response = generativeModel.generateContent(finalPrompt + userText)
            val fullText = response.text ?: ""


            val emotionRegex = "\\[EMOTION: (.*?)\\]".toRegex()
            val match = emotionRegex.find(fullText)

            val emotion = match?.groupValues?.get(1) ?: "NEUTRAL"

            val cleanText = fullText.replace(emotionRegex, "").trim()

            Result(replyText = cleanText, emotion = emotion)
        } catch (e: Exception) {
            Result(replyText = "Sorry, I loss you... $e", emotion = "SAD")
        }
    }
}


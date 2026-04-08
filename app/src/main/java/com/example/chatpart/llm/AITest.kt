package com.example.chatpart.llm

import android.util.Log
import com.example.chatpart.BuildConfig
import com.example.chatpart.domain.Result
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import okhttp3.MediaType.Companion.toMediaTypeOrNull
class AITest : LlmClient {

    /*
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.API_Key
    )
    */

    private val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    private val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()

    override suspend fun reply(systemPrompt: String, userText: String): Result {
        return try {
            val finalPrompt = "$systemPrompt \nPlease include [EMOTION: emotion] tag at the end of your reply. Select emotion from: HAPPY, SAD, ANGRY, SHY, NEUTRAL. Reply in the SAME language as the user's message."

            // --- MiniMax Global Integration ---
            val json = org.json.JSONObject()
            json.put("model", "MiniMax-M2.7")
            val messages = org.json.JSONArray()
            val systemMsg = org.json.JSONObject().put("role", "system").put("content", finalPrompt)
            val userMsg = org.json.JSONObject().put("role", "user").put("content", userText)
            messages.put(systemMsg).put(userMsg)
            json.put("messages", messages)

            val body = okhttp3.RequestBody.create(mediaType, json.toString())
            val request = okhttp3.Request.Builder()
                .url("https://api.minimax.io/v1/chat/completions")
                .header("Authorization", "Bearer ${BuildConfig.MINIMAX_CHAT_API_KEY}")
                .post(body)
                .build()

            val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                client.newCall(request).execute()
            }
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) return Result(replyText = "Error: ${response.code}", emotion = "SAD")

            val respJson = org.json.JSONObject(responseBody)
            var fullText = respJson.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
            
            // Clean out reasoning tags from MiniMax-M2.7
            val thinkRegex = "<think>.*?</think>".toRegex(RegexOption.DOT_MATCHES_ALL)
            fullText = fullText.replace(thinkRegex, "").trim()

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


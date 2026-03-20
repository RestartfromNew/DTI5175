package com.example.chatpart.llm

import com.example.chatpart.BuildConfig
import com.example.chatpart.domain.Result
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class MiniMaxLlmClient : LlmClient {

    // 从 BuildConfig 获取配置
    private val apiKey = BuildConfig.MINIMAX_API_KEY
    private val groupId = BuildConfig.MINIMAX_GROUP_ID

    // 自定义 API 端点 - 支持自定义部署
    // 如果配置了 MINIMAX_BASE_URL，则使用自定义端点，否则使用官方 API
    private val baseUrl: String = if (BuildConfig.MINIMAX_BASE_URL.isNotEmpty()) {
        BuildConfig.MINIMAX_BASE_URL
    } else {
        "https://api.minimax.chat"
    }

    // 模型名称
    private val modelName = "MiniMax-M2.1"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    override suspend fun reply(
        systemPrompt: String,
        userText: String
    ): Result = withContext(Dispatchers.IO) {
        try {
            val requestBody = buildRequestBody(systemPrompt, userText)
            val request = Request.Builder()
                .url("$baseUrl/v1/text/chatcompletion_v2?GroupId=$groupId")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result(
                        replyText = "API Error: ${response.code} - ${response.message}",
                        emotion = "neutral"
                    )
                }

                val responseBody = response.body?.string() ?: ""
                parseResponse(responseBody)
            }
        } catch (e: Exception) {
            Result(
                replyText = "Error: ${e.message}",
                emotion = "neutral"
            )
        }
    }

    /**
     * 流式响应 - 用于实时显示
     */
    fun replyStream(
        systemPrompt: String,
        userText: String
    ): Flow<PartialResult> = flow {
        try {
            val requestBody = buildRequestBody(systemPrompt, userText, stream = true)
            val request = Request.Builder()
                .url("$baseUrl/v1/text/chatcompletion_v2?GroupId=$groupId")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    emit(PartialResult.Error("API Error: ${response.code}"))
                    return@flow
                }

                response.body?.charStream()?.buffered()?.use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        if (line?.startsWith("data:") == true) {
                            val jsonStr = line?.removePrefix("data:")?.trim() ?: continue
                            try {
                                val json = gson.fromJson(jsonStr, JsonObject::class.java)
                                emit(PartialResult.Token(jsonStr))
                            } catch (e: Exception) {
                                // Skip malformed JSON
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            emit(PartialResult.Error(e.message ?: "Unknown error"))
        }
    }

    private fun buildRequestBody(
        systemPrompt: String,
        userText: String,
        stream: Boolean = false
    ): okhttp3.RequestBody {
        val requestJson = JsonObject().apply {
            addProperty("model", modelName)
            addProperty("stream", stream)

            val messages = JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("role", "system")
                    addProperty("content", systemPrompt)
                })
                add(JsonObject().apply {
                    addProperty("role", "user")
                    addProperty("content", userText)
                })
            }
            add("messages", messages)

            // 添加温度和其他参数
            addProperty("temperature", 0.7)
            addProperty("max_tokens", 2048)
        }

        return gson.toJson(requestJson).toRequestBody(mediaType)
    }

    private fun parseResponse(responseBody: String): Result {
        return try {
            val json = gson.fromJson(responseBody, JsonObject::class.java)

            // 检查是否有错误
            if (json.has("base_resp")) {
                val baseResp = json.getAsJsonObject("base_resp")
                val statusCode = baseResp.get("status_code").asInt
                if (statusCode != 0) {
                    val errorMsg = baseResp.get("status_msg").asString
                    return Result(replyText = "Error: $errorMsg", emotion = "neutral")
                }
            }

            // 解析响应
            val choices = json.getAsJsonArray("choices")
            if (choices != null && choices.size() > 0) {
                val firstChoice = choices[0].asJsonObject
                val message = firstChoice.getAsJsonObject("message")
                val content = message.get("content").asString

                // 尝试提取情感（如果有）
                val emotion = extractEmotion(content)

                Result(replyText = content, emotion = emotion)
            } else {
                Result(replyText = "No response from API", emotion = "neutral")
            }
        } catch (e: Exception) {
            Result(replyText = "Parse Error: ${e.message}", emotion = "neutral")
        }
    }

    /**
     * 从回复中提取情感标签
     * 可以根据实际需求自定义解析逻辑
     */
    private fun extractEmotion(text: String): String {
        // 简单实现：如果文本包含特定情感词则提取
        // 可以根据实际需求改进
        // MiniMax TTS 只接受这几个值: happy / sad / angry / fearful / disgusted / surprised
        // "excited" 不是合法值会报 invalid params，映射到 "happy"
        // "neutral" 留空（不传 emotion 参数）让 TTS 自动处理
        val emotionPatterns = mapOf(
            "happy"     to listOf("😊", "😄", "happy", "excited", "great", "wonderful", "amazing", "太好了", "开心", "兴奋", "激动"),
            "sad"       to listOf("😢", "sad", "sorry", "unfortunately", "遗憾", "难过"),
            "surprised" to listOf("😮", "surprised", "wow", "unexpected", "惊讶", "意外"),
            "angry"     to listOf("😠", "angry", "frustrated", "annoyed", "愤怒", "生气")
        )

        for ((emotion, patterns) in emotionPatterns) {
            for (pattern in patterns) {
                if (text.contains(pattern, ignoreCase = true)) {
                    return emotion
                }
            }
        }

        return ""  // 空字符串 → VoiceSetting.emotion = null → TTS 自动决定
    }
}

/**
 * 流式响应的部分结果
 */
sealed class PartialResult {
    data class Token(val token: String) : PartialResult()
    data class Error(val message: String) : PartialResult()
    data class Done(val emotion: String) : PartialResult()
}

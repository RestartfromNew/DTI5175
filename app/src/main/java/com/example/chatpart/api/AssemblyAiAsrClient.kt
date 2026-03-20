package com.example.chatpart.api

import android.util.Log
import com.example.chatpart.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * AssemblyAI Speech-to-Text client.
 *
 * Flow:
 *   1. POST /v2/upload  — upload raw WAV bytes → get upload_url
 *   2. POST /v2/transcript { audio_url, language_detection: true } → get transcript id
 *   3. GET  /v2/transcript/{id}  — poll every second until status=completed → return text
 */
class AssemblyAiAsrClient {

    companion object {
        private const val TAG = "AssemblyAiAsrClient"
        private const val BASE_URL = "https://api.assemblyai.com"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Transcribe a WAV file.
     * Returns the recognized text on success, or throws an Exception with a user-visible message.
     */
    suspend fun transcribe(wavFile: File): String = withContext(Dispatchers.IO) {
        val uploadUrl = uploadAudio(wavFile)
            ?: throw Exception("上传音频失败，请检查网络")
        Log.d(TAG, "Uploaded → $uploadUrl")

        val transcriptId = submitTranscript(uploadUrl)
            ?: throw Exception("提交转写请求失败（API Key 或网络问题）")
        Log.d(TAG, "Transcript job id=$transcriptId")

        pollForResult(transcriptId)
    }

    // ── Step 1: Upload ────────────────────────────────────────────────────────

    private fun uploadAudio(wavFile: File): String? {
        val body = wavFile.readBytes().toRequestBody("application/octet-stream".toMediaType())
        val request = Request.Builder()
            .url("$BASE_URL/v2/upload")
            .header("authorization", BuildConfig.ASSEMBLYAI_API_KEY)
            .post(body)
            .build()

        val (code, bodyStr) = try {
            client.newCall(request).execute().use { resp ->
                Pair(resp.code, resp.body?.string() ?: "")
            }
        } catch (e: Exception) {
            Log.e(TAG, "uploadAudio network error: ${e.message}")
            Pair(0, "")
        }

        Log.d(TAG, "Upload response ($code): $bodyStr")
        if (code !in 200..299 || bodyStr.isEmpty()) return null

        return try {
            JSONObject(bodyStr).optString("upload_url").ifEmpty { null }
        } catch (e: Exception) {
            Log.e(TAG, "uploadAudio JSON error: ${e.message}")
            null
        }
    }

    // ── Step 2: Submit transcript ─────────────────────────────────────────────

    private fun submitTranscript(audioUrl: String): String? {
        // Match the working Python script exactly:
        // speech_models: universal-3-pro for common languages, universal-2 fallback for everything else (incl. Chinese)
        val json = JSONObject().apply {
            put("audio_url", audioUrl)
            put("language_detection", true)
            put("speech_models", org.json.JSONArray().apply {
                put("universal-3-pro")
                put("universal-2")
            })
        }.toString()

        val body = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$BASE_URL/v2/transcript")
            .header("authorization", BuildConfig.ASSEMBLYAI_API_KEY)
            .header("Content-Type", "application/json")
            .post(body)
            .build()

        val (code, bodyStr) = try {
            client.newCall(request).execute().use { resp ->
                Pair(resp.code, resp.body?.string() ?: "")
            }
        } catch (e: Exception) {
            Log.e(TAG, "submitTranscript network error: ${e.message}")
            Pair(0, "")
        }

        Log.d(TAG, "Transcript submit ($code): $bodyStr")
        if (code !in 200..299 || bodyStr.isEmpty()) return null

        return try {
            JSONObject(bodyStr).optString("id").ifEmpty { null }
        } catch (e: Exception) {
            Log.e(TAG, "submitTranscript JSON error: ${e.message}")
            null
        }
    }

    // ── Step 3: Poll ──────────────────────────────────────────────────────────

    private suspend fun pollForResult(transcriptId: String): String {
        val url = "$BASE_URL/v2/transcript/$transcriptId"

        repeat(30) { attempt ->
            delay(1000)
            val (code, bodyStr) = withContext(Dispatchers.IO) {
                val req = Request.Builder()
                    .url(url)
                    .header("authorization", BuildConfig.ASSEMBLYAI_API_KEY)
                    .get()
                    .build()
                client.newCall(req).execute().use { resp ->
                    Pair(resp.code, resp.body?.string() ?: "")
                }
            }
            if (bodyStr.isEmpty()) return@repeat

            val json = try {
                JSONObject(bodyStr)
            } catch (e: Exception) {
                Log.e(TAG, "Poll result JSON error: ${e.message}")
                return@repeat
            }

            val status = json.optString("status")
            Log.d(TAG, "Poll #${attempt + 1} ($code) status=$status")

            when (status) {
                "completed" -> {
                    val text = json.optString("text", "")
                    Log.d(TAG, "Transcription result: \"$text\"")
                    return text
                }
                "error" -> {
                    Log.e(TAG, "Transcription error: ${json.optString("error")}")
                    return ""
                }
                // "queued" / "processing" → keep polling
            }
        }

        throw Exception("转写超时（30s），请重试")
    }
}

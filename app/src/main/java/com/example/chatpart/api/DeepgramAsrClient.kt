package com.example.chatpart.api

import android.util.Log
import com.example.chatpart.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Deepgram Speech-to-Text client.
 * Much faster than AssemblyAI because it returns results in a single POST request.
 */
class DeepgramAsrClient {

    companion object {
        private const val TAG = "DeepgramAsrClient"
        private const val BASE_URL = "https://api.deepgram.com/v1/listen"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Transcribe a WAV file directly to text.
     */
    suspend fun transcribe(wavFile: File): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$BASE_URL?model=nova-3&language=multi&smart_format=true")
            .header("Authorization", "Token ${BuildConfig.DEEPGRAM_API_KEY}")
            .header("Content-Type", "audio/wav")
            .post(wavFile.readBytes().toRequestBody("audio/wav".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { resp ->
                val bodyStr = resp.body?.string() ?: ""
                Log.d(TAG, "Deepgram Response (${resp.code}): $bodyStr")
                
                if (!resp.isSuccessful) {
                    Log.e(TAG, "Deepgram request failed: ${resp.message}")
                    return@withContext ""
                }
                
                val json = JSONObject(bodyStr)
                // Dig into Deepgram's nested response structure
                val results = json.getJSONObject("results")
                val channels = results.getJSONArray("channels")
                val firstChannel = channels.getJSONObject(0)
                val alternatives = firstChannel.getJSONArray("alternatives")
                val bestAlt = alternatives.getJSONObject(0)
                
                bestAlt.optString("transcript", "")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Deepgram Exception: ${e.message}")
            ""
        }
    }
}

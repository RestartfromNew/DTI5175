package com.example.chatpart.api

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.example.chatpart.BuildConfig
import com.example.chatpart.audio.AudioClient
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * MiniMax Audio Client implementation
 * Part 3.2 - Implements AudioClient interface for TTS and STT
 */
class MiniMaxAudioClient(private val context: Context) : AudioClient {

    companion object {
        private const val TAG = "MiniMaxAudioClient"
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("Authorization", "Bearer ${BuildConfig.MINIMAX_API_KEY}")
                    .header("Content-Type", "application/json")
                    .build()
            )
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson: Gson = GsonBuilder().create()
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Converts text to audio. Returns absolute path of saved .mp3 file.
     *
     * @param text The text to convert to speech
     * @param voiceId The voice ID to use. If blank, uses a default English preset voice.
     * @param emotion Emotion setting: "happy", "sad", "calm", "angry", "whisper", or null for auto
     * @return Absolute path of the saved .mp3 file
     */
    override suspend fun textToVoice(text: String, voiceId: String, emotion: String, languageBoost: String): String =
        withContext(Dispatchers.IO) {
            val effectiveVoiceId = voiceId.ifBlank { "English_expressive_narrator" }

            val requestBody = MiniMaxTtsRequest(
                model = MiniMaxConfig.TTS_MODEL,
                text = text,
                voice_setting = VoiceSetting(
                    voice_id = effectiveVoiceId,
                    emotion = emotion.ifBlank { null },
                    language_boost = languageBoost.ifBlank { null }
                ),
                audio_setting = AudioSetting(
                    format = MiniMaxConfig.AUDIO_FORMAT,
                    sample_rate = MiniMaxConfig.AUDIO_SAMPLE_RATE,
                    bitrate = MiniMaxConfig.AUDIO_BITRATE,
                    channel = MiniMaxConfig.AUDIO_CHANNEL
                )
            )

            val json = gson.toJson(requestBody)
            Log.d(TAG, "TTS Request: $json")

            // Build URL with optional group_id
            val urlBuilder = StringBuilder("${MiniMaxConfig.BASE_URL}/v1/t2a_v2")
            if (BuildConfig.MINIMAX_GROUP_ID.isNotBlank()) {
                urlBuilder.append("?GroupId=${BuildConfig.MINIMAX_GROUP_ID}")
            }

            val request = Request.Builder()
                .url(urlBuilder.toString())
                .post(json.toRequestBody(mediaType))
                .build()

            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: throw IOException("Empty TTS response")

                Log.d(TAG, "TTS Response: $body")

                if (!response.isSuccessful) {
                    throw IOException("TTS failed: ${response.code} - $body")
                }

                val ttsResponse = gson.fromJson(body, MiniMaxTtsResponse::class.java)

                if (ttsResponse.base_resp.status_code != 0) {
                    throw IOException("TTS error: ${ttsResponse.base_resp.status_msg}")
                }

                // Decode hex string → ByteArray → save as .mp3 in cache
                val hexAudio = ttsResponse.data?.audio
                    ?: throw IOException("No audio data in TTS response")

                val audioBytes = hexToByteArray(hexAudio)

                val outputFile = File(context.cacheDir, "tts_${System.currentTimeMillis()}.mp3")
                outputFile.writeBytes(audioBytes)
                Log.d(TAG, "TTS saved to: ${outputFile.absolutePath}")
                outputFile.absolutePath
            } catch (e: Exception) {
                Log.e(TAG, "TTS error: ${e.message}")
                throw e
            }
        }

    /**
     * Converts speech to text using Android SpeechRecognizer.
     *
     * NOTE: MiniMax does NOT currently offer an STT/ASR endpoint.
     * This implementation uses Android's built-in recognizer to directly capture
     * and transcribe microphone input.
     *
     * @param onResult Callback with the transcribed text
     */
    override suspend fun voiceToText(onResult: (String) -> Unit) {
        return suspendCoroutine { cont ->
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                Log.w(TAG, "Speech recognition not available on this device")
                onResult("")
                cont.resume(Unit)
                return@suspendCoroutine
            }

            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle?) {
                    val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = list?.firstOrNull() ?: ""
                    Log.d(TAG, "STT result: $text")
                    recognizer.destroy()
                    onResult(text)
                    cont.resume(Unit)
                }

                override fun onError(error: Int) {
                    val errorMessage = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                        SpeechRecognizer.ERROR_SERVER -> "Server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                        else -> "Unknown error"
                    }
                    Log.e(TAG, "STT error: $errorMessage (code: $error)")
                    recognizer.destroy()
                    onResult("")  // graceful fallback
                    cont.resume(Unit)
                }

                override fun onReadyForSpeech(params: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })

            recognizer.startListening(intent)
        }
    }

    /**
     * Helper: Convert hex string to byte array
     */
    private fun hexToByteArray(hex: String): ByteArray {
        if (hex.isEmpty()) return ByteArray(0)
        return hex.chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }
}

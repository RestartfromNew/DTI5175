package com.example.chatpart.api

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.os.Bundle
import android.os.ParcelFileDescriptor
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
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileInputStream
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
        private const val SAMPLE_RATE = 16000
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            // Multipart 请求不能覆盖 Content-Type，OkHttp 会自动设置带 boundary 的 multipart/form-data
            val builder = original.newBuilder()
                .header("Authorization", "Bearer ${BuildConfig.MINIMAX_API_KEY}")
            if (original.body !is MultipartBody) {
                builder.header("Content-Type", "application/json")
            }
            chain.proceed(builder.build())
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
            
            // Normalize emotion to values supported by MiniMax V2:
            // happy, sad, angry, surprised, fearful, disgusted, or null for neutral
            val validEmotions = setOf("happy", "sad", "angry", "surprised", "fearful", "disgusted")
            val effectiveEmotion = when {
                emotion.lowercase() in validEmotions -> emotion.lowercase()
                emotion.contains("高兴", true) || emotion.contains("喜", true) -> "happy"
                emotion.contains("伤心", true) || emotion.contains("悲", true) -> "sad"
                emotion.contains("生气", true) || emotion.contains("怒", true) -> "angry"
                else -> null // Default to neutral if unrecognized
            }

            val requestBody = MiniMaxTtsRequest(
                model = MiniMaxConfig.TTS_MODEL,
                text = text,
                voice_setting = VoiceSetting(
                    voice_id = effectiveVoiceId,
                    emotion = effectiveEmotion,
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
                // 用 .use {} 确保 response 一定被关闭，防止连接泄漏
                val body = client.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string()
                        ?: throw IOException("Empty TTS response")
                    Log.d(TAG, "TTS Response: $bodyStr")
                    if (!response.isSuccessful) {
                        throw IOException("TTS failed: ${response.code} - $bodyStr")
                    }
                    bodyStr
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
                Log.e(TAG, "TTS error", e)
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

    private var currentRecognizer: SpeechRecognizer? = null
    private var pendingResultCallback: ((String) -> Unit)? = null

    // Push-to-talk state
    @Volatile private var stopListeningCalled = false
    private var lastPartialResult = ""

    override fun startListeningWithCallbacks(
        onPartialResult: ((String) -> Unit)?,
        onRmsChanged: ((Float) -> Unit)?,
        onResult: ((String) -> Unit)?
    ) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(TAG, "Speech recognition not available on this device")
            onResult?.invoke("")
            return
        }

        // Destroy previous recognizer if exists
        currentRecognizer?.destroy()
        currentRecognizer = null

        // Reset push-to-talk state
        stopListeningCalled = false
        lastPartialResult = ""

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        currentRecognizer = recognizer
        pendingResultCallback = onResult

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // Keep recognizer alive while user holds mic button — prevent auto-stop on silence
            putExtra("android.speech.extra.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS", 30000L)
            putExtra("android.speech.extra.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS", 30000L)
            putExtra("android.speech.extra.SPEECH_INPUT_MINIMUM_LENGTH_MILLIS", 0L)
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = list?.firstOrNull() ?: ""
                Log.d(TAG, "STT final result: $text")
                // If final result is empty but we have a partial, use it as fallback
                val effective = if (text.isNotEmpty()) text else lastPartialResult
                Log.d(TAG, "STT effective result: \"$effective\" (raw=\"$text\", partial=\"$lastPartialResult\")")
                currentRecognizer?.destroy()
                currentRecognizer = null
                pendingResultCallback = null
                onResult?.invoke(effective)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val list = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partialText = list?.firstOrNull() ?: ""
                if (partialText.isNotEmpty()) {
                    Log.d(TAG, "STT partial result: $partialText")
                    lastPartialResult = partialText
                    onPartialResult?.invoke(partialText)
                }
            }

            override fun onRmsChanged(rmsdB: Float) {
                onRmsChanged?.invoke(rmsdB)
            }

            override fun onError(error: Int) {
                Log.e(TAG, "STT error code: $error (stopListeningCalled=$stopListeningCalled, lastPartial=\"$lastPartialResult\")")
                currentRecognizer?.destroy()
                currentRecognizer = null
                // Known Android bug: stopListening() sometimes fires onError instead of onResults.
                // If we called stopListening() and have a partial result, use it as the final text.
                val fallback = if (stopListeningCalled && lastPartialResult.isNotEmpty()) {
                    Log.d(TAG, "Using lastPartialResult as fallback: \"$lastPartialResult\"")
                    lastPartialResult
                } else {
                    ""
                }
                onResult?.invoke(fallback)
            }

            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        recognizer.startListening(intent)
        Log.d(TAG, "Speech recognition started")
    }

    override fun stopListening() {
        Log.d(TAG, "Stopping speech recognition... (lastPartial=\"$lastPartialResult\")")
        // Mark that user released the button — enables partial result fallback in onError
        stopListeningCalled = true
        // Only signal end of speech — do NOT destroy here.
        // onResults (or onError with fallback) will fire asynchronously and handle cleanup.
        currentRecognizer?.stopListening()
    }

    /**
     * 将录好的 WAV 文件上传至 MiniMax STT API 进行转录（Push-to-Talk 用）
     *
     * 流程：POST /v1/stt/create → 拿到 generation_id → 轮询 GET /v1/stt/{id} → 返回全文
     * 在 IO 线程调用，内部已切换到 Dispatchers.IO。
     */
    suspend fun speechToText(wavFile: File): String = withContext(Dispatchers.IO) {
        try {
            // ── Step 1: 创建 STT 任务 ─────────────────────────────────────
            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("model", "speech-01-turbo")
                .addFormDataPart(
                    "audio", wavFile.name,
                    wavFile.asRequestBody("audio/wav".toMediaType())
                )
                .build()

            val urlBuilder = StringBuilder("${MiniMaxConfig.BASE_URL}/v1/stt/create")
            if (BuildConfig.MINIMAX_GROUP_ID.isNotBlank()) {
                urlBuilder.append("?GroupId=${BuildConfig.MINIMAX_GROUP_ID}")
            }

            val createRequest = Request.Builder()
                .url(urlBuilder.toString())
                .post(multipartBody)
                .build()

            val createBody = client.newCall(createRequest).execute().use { resp ->
                resp.body?.string() ?: return@withContext ""
            }
            Log.d(TAG, "STT create response: $createBody")

            val createResp = gson.fromJson(createBody, MiniMaxSttCreateResponse::class.java)
            if (createResp.base_resp?.status_code != 0) {
                Log.e(TAG, "STT create failed: ${createResp.base_resp?.status_msg}")
                return@withContext ""
            }

            val jobId = createResp.generation_id ?: return@withContext ""
            Log.d(TAG, "STT job created: $jobId")

            // ── Step 2: 轮询结果（每 500ms 一次，最多 30 次 = 15 秒）──────
            val pollUrlBase = "${MiniMaxConfig.BASE_URL}/v1/stt/$jobId"
            for (attempt in 0 until 30) {
                delay(500)
                val pollBody = client.newCall(
                    Request.Builder().url(pollUrlBase).get().build()
                ).execute().use { resp ->
                    resp.body?.string() ?: ""
                }
                if (pollBody.isEmpty()) continue

                Log.d(TAG, "STT poll #$attempt: $pollBody")

                val pollResp = gson.fromJson(pollBody, MiniMaxSttPollResponse::class.java)
                when (pollResp.status?.lowercase()) {
                    "succeeded" -> {
                        val text = pollResp.file?.transcription?.full_text ?: ""
                        Log.d(TAG, "STT result: \"$text\"")
                        return@withContext text
                    }
                    "failed" -> {
                        Log.e(TAG, "STT job failed")
                        return@withContext ""
                    }
                    // "processing" → continue polling
                }
            }

            Log.e(TAG, "STT timed out after 15s")
            ""
        } catch (e: Exception) {
            Log.e(TAG, "speechToText error: ${e.message}")
            ""
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

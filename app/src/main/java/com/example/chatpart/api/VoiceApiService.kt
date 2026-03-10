package com.example.chatpart.api

import android.util.Log
import java.io.File

/**
 * Voice processing API interface.
 * 
 * This provides a clean interface for the backend voice processing service.
 * Replace the mock implementation with actual API calls when the backend is ready.
 * 
 * Expected backend API:
 *   POST /api/voice/transcribe
 *   - Body: multipart/form-data with audio file
 *   - Response: { "text": "transcribed text", "confidence": 0.95 }
 * 
 *   POST /api/voice/respond
 *   - Body: { "audioUrl": "...", "chatbotId": "..." }
 *   - Response: { "text": "AI response text", "audioUrl": "response_audio_url" }
 */
interface VoiceApiService {

    /**
     * Transcribe an audio file to text.
     * @param audioFile The recorded audio file (PCM/WAV format)
     * @param language The language code (e.g., "en-US", "zh-CN")
     * @return Result containing transcribed text
     */
    suspend fun transcribeAudio(audioFile: File, language: String = "en-US"): Result<String>

    /**
     * Send a voice message to the AI and get a text response.
     * @param audioFile The recorded audio file
     * @param chatbotId The selected chatbot persona ID
     * @return Result containing the AI response text
     */
    suspend fun sendVoiceMessage(audioFile: File, chatbotId: String): Result<VoiceResponse>
}

data class VoiceResponse(
    val text: String,
    val audioUrl: String? = null,  // Optional: AI voice response URL
    val durationMs: Long = 0
)

/**
 * Mock implementation for development/testing.
 * Replace with real API client (Retrofit/Ktor) when backend is ready.
 */
class MockVoiceApiService : VoiceApiService {

    companion object {
        private const val TAG = "VoiceApiService"
    }

    override suspend fun transcribeAudio(audioFile: File, language: String): Result<String> {
        Log.d(TAG, "📤 Transcribing audio: ${audioFile.name} (${audioFile.length()} bytes, lang=$language)")

        // TODO: Replace with actual API call:
        // val response = retrofit.create(VoiceApi::class.java)
        //     .transcribe(audioFile.toMultipartBody(), language)
        // return Result.success(response.text)

        // Mock: simulate network delay and return fake transcription
        kotlinx.coroutines.delay(1500)
        return Result.success("[Voice message - ${audioFile.length() / 1024}KB]")
    }

    override suspend fun sendVoiceMessage(audioFile: File, chatbotId: String): Result<VoiceResponse> {
        Log.d(TAG, "📤 Sending voice to chatbot '$chatbotId': ${audioFile.name}")

        // TODO: Replace with actual API call:
        // val response = retrofit.create(VoiceApi::class.java)
        //     .sendVoice(audioFile.toMultipartBody(), chatbotId)
        // return Result.success(response)

        // Mock: simulate processing
        kotlinx.coroutines.delay(2000)
        return Result.success(
            VoiceResponse(
                text = "I received your voice message! (mock response from $chatbotId)",
                durationMs = 2000
            )
        )
    }
}

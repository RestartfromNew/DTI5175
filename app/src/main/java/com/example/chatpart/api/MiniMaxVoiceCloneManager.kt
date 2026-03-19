package com.example.chatpart.api

import android.content.Context
import com.example.chatpart.BuildConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * MiniMax Voice Clone Manager implementation
 * Part 4 - Replaces mock VoiceCloneManager with real MiniMax API
 *
 * 3-step flow:
 * 1. Upload WAV → POST /v1/files/upload → get file_id
 * 2. Register voice clone → POST /v1/voice_clone with file_id
 * 3. Return voice_id to be stored on Profile
 *
 * Audio requirements:
 * - Format: mp3, m4a, wav
 * - Duration: 10 seconds – 5 minutes
 * - Max size: 20 MB
 *
 * Note: MiniMax cloned voices are deleted after 7 days of non-use.
 * The app should call TTS with this voice_id at least once every 7 days.
 */
class MiniMaxVoiceCloneManager(private val context: Context) {

    companion object {
        private const val TAG = "MiniMaxVoiceCloneManager"
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("Authorization", "Bearer ${BuildConfig.MINIMAX_API_KEY}")
                    .build()
            )
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS) // clone processing can take time
        .build()

    private val gson = Gson()

    /**
     * Full voice clone flow. Returns Result<voiceId> to be stored on Profile.
     * On failure, returns Result.failure with a descriptive error message.
     *
     * @param audioFile The recorded audio file (WAV, MP3, or M4A)
     * @param characterId The character ID to build the voice_id from
     * @return Result containing the voice_id on success, or an error on failure
     */
    suspend fun cloneVoice(audioFile: File, characterId: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val fileId = uploadAudioFile(audioFile)
                val safeVoiceId = buildVoiceId(characterId)
                registerVoiceClone(fileId = fileId, voiceId = safeVoiceId)
                safeVoiceId
            }
        }

    /**
     * Step 1: Upload audio file to MiniMax, return file_id
     * NOTE: file_id is Int64 in MiniMax API, use Long to avoid overflow
     */
    private fun uploadAudioFile(audioFile: File): Long {
        val mediaType = when {
            audioFile.name.endsWith(".mp3") -> "audio/mpeg"
            audioFile.name.endsWith(".m4a") -> "audio/mp4"
            audioFile.name.endsWith(".wav") -> "audio/wav"
            else -> "audio/wav"
        }

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                audioFile.name,
                audioFile.asRequestBody(mediaType.toMediaType())
            )
            .addFormDataPart("purpose", "voice_clone")
            .build()

        val request = Request.Builder()
            .url("${MiniMaxConfig.BASE_URL}/v1/files/upload")
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw IOException("Empty upload response")

        // Response: { "file": { "file_id": 123456789 }, "base_resp": { "status_code": 0 } }
        val json = gson.fromJson(body, JsonObject::class.java)
        val statusCode = json.getAsJsonObject("base_resp")?.get("status_code")?.asInt ?: -1
        if (statusCode != 0) {
            throw IOException("Upload failed (code $statusCode): $body")
        }

        return json.getAsJsonObject("file")?.get("file_id")?.asLong
            ?: throw IOException("No file_id in upload response")
    }

    /**
     * Step 2: Register the cloned voice with MiniMax
     */
    private fun registerVoiceClone(fileId: Long, voiceId: String) {
        val payload = mapOf(
            "file_id" to fileId,
            "voice_id" to voiceId,
            "model" to MiniMaxConfig.CLONE_MODEL,
            "need_noise_reduction" to false,
            "need_volume_normalization" to true
        )

        val request = Request.Builder()
            .url("${MiniMaxConfig.BASE_URL}/v1/voice_clone")
            .post(gson.toJson(payload).toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw IOException("Empty clone response")

        val json = gson.fromJson(body, JsonObject::class.java)
        val statusCode = json.getAsJsonObject("base_resp")?.get("status_code")?.asInt ?: -1
        if (statusCode != 0) {
            throw IOException("Clone failed (code $statusCode): $body")
        }
        // status_code 0 = success; voice_id is now active in the MiniMax account
    }

    /**
     * Fetch all cloned voices from MiniMax API.
     * Returns a list of voice_ids that exist on the server.
     *
     * ⚠️ A voice only appears here AFTER it has been used in at least one TTS call.
     */
    suspend fun fetchClonedVoices(): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = mapOf("voice_type" to "voice_cloning")
            val request = Request.Builder()
                .url("${MiniMaxConfig.BASE_URL}/v1/get_voice")
                .post(gson.toJson(payload).toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: throw IOException("Empty response from get_voice")

            val json = gson.fromJson(body, JsonObject::class.java)
            val statusCode = json.getAsJsonObject("base_resp")?.get("status_code")?.asInt ?: -1
            if (statusCode != 0) {
                throw IOException("get_voice failed (code $statusCode): $body")
            }

            // Extract voice_ids from voice_cloning array
            val voiceArray = json.getAsJsonArray("voice_cloning") ?: return@runCatching emptyList()
            voiceArray.mapNotNull { element ->
                element.asJsonObject?.get("voice_id")?.asString
            }
        }
    }

    /**
     * Builds a valid MiniMax voice_id from a character ID.
     *
     * Ensures uniqueness by appending a UUID suffix to avoid duplicates.
     *
     * Constraints:
     * - Length: 8–256 characters
     * - Must start with an English letter
     * - Allowed characters: letters, digits, hyphens (-), underscores (_)
     * - Cannot end with - or _
     * - Must be globally unique in your MiniMax account
     *
     * Example: characterId="Alex" → "cpAlex_a1b2c3d4"
     */
    /**
     * Delete a cloned voice from MiniMax
     *
     * @param voiceId The voice_id to delete
     * @return Result.success if deleted, Result.failure otherwise
     */
    suspend fun deleteVoice(voiceId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = mapOf(
                "voice_type" to "voice_cloning",
                "voice_id" to voiceId
            )

            val request = Request.Builder()
                .url("${MiniMaxConfig.BASE_URL}/v1/delete_voice")
                .post(gson.toJson(payload).toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: throw IOException("Empty response from delete_voice")

            val json = gson.fromJson(body, JsonObject::class.java)
            val statusCode = json.getAsJsonObject("base_resp")?.get("status_code")?.asInt ?: -1
            if (statusCode != 0) {
                val errorMsg = json.getAsJsonObject("base_resp")?.get("status_msg")?.asString ?: "Unknown error"
                throw IOException("delete_voice failed (code $statusCode): $errorMsg")
            }
        }
    }

    fun buildVoiceId(characterId: String): String {
        val sanitized = characterId
            .filter { it.isLetterOrDigit() || it == '-' || it == '_' }
            .take(15)
        val uniqueSuffix = UUID.randomUUID().toString()
            .replace("-", "")
            .take(8)
            .lowercase()
        return "cp${sanitized}_$uniqueSuffix"
    }
}

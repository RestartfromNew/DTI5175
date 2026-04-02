package com.example.chatpart.i18n

import android.content.Context
import android.util.Log
import com.example.chatpart.api.MiniMaxAudioClient
import com.example.chatpart.api.MiniMaxVoiceCloneManager
import com.example.chatpart.data.VideoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class VoiceCloneManager(
    private val context: Context
) {
    private val audioClient = MiniMaxAudioClient(context)
    private val miniMaxVoiceCloneManager = MiniMaxVoiceCloneManager(context)
    private val videoManager = VideoManager()

    suspend fun cloneVoice(
        audioFile: File?,
        characterId: String,
        uid: String,
        avatarPath: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d("VoiceDebug", "cloneVoice entered")
            Log.d("VoiceDebug", "characterId = $characterId")
            Log.d("VoiceDebug", "uid = $uid")
            Log.d("VoiceDebug", "avatarPath = $avatarPath")
            Log.d("VoiceDebug", "audioFile = ${audioFile?.absolutePath}")

            require(audioFile != null && audioFile.exists()) {
                "Audio file is missing"
            }

            if (!avatarPath.isNullOrBlank()) {
                Log.d("VoiceDebug", "before uploadAvatar")
                val avatarResp = videoManager.uploadAvatar(avatarPath, characterId)
                Log.d("VoiceDebug", "after uploadAvatar, server path = $avatarResp")
            } else {
                Log.d("VoiceDebug", "skip uploadAvatar because avatarPath is blank")
            }

            Log.d("VoiceDebug", "before MiniMax clone")
            val voiceId = miniMaxVoiceCloneManager
                .cloneVoice(audioFile, characterId, uid)
                .getOrThrow()
            Log.d("VoiceDebug", "after MiniMax clone, voiceId = $voiceId")

            Log.d("VoiceDebug", "before speechToText")
            val transcript = audioClient.speechToText(audioFile)
            Log.d("VoiceDebug", "after speechToText, transcript = $transcript")

            if (transcript.isBlank()) {
                throw IllegalStateException("Failed to generate transcript")
            }

            Log.d("VoiceDebug", "before uploadVoiceReference")
            val voiceResp = videoManager.uploadVoiceReference(
                localVoicePath = audioFile.absolutePath,
                characterId = characterId,
                transcript = transcript
            )
            Log.d(
                "VoiceDebug",
                "after uploadVoiceReference, voice = ${voiceResp.character_voice}, txt = ${voiceResp.character_txt}"
            )

            Result.success(voiceId)
        } catch (e: Exception) {
            Log.e("VoiceDebug", "cloneVoice failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun uploadReferenceAssets(
        audioFile: File?,
        characterId: String,
        avatarPath: String?,
        transcript: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d("VoiceDebug", "uploadReferenceAssets entered")
            Log.d("VoiceDebug", "characterId = $characterId")
            Log.d("VoiceDebug", "audioFile = ${audioFile?.absolutePath}")
            Log.d("VoiceDebug", "avatarPath = $avatarPath")

            require(audioFile != null && audioFile.exists()) {
                "Audio file is missing"
            }

            if (!avatarPath.isNullOrBlank()) {
                Log.d("VoiceDebug", "before uploadAvatar")
                val avatarResp = videoManager.uploadAvatar(
                    localAvatarPath = avatarPath,
                    characterId = characterId
                )
                Log.d("VoiceDebug", "after uploadAvatar, server path = $avatarResp")
            } else {
                Log.d("VoiceDebug", "skip uploadAvatar because avatarPath is blank")
            }

            Log.d("VoiceDebug", "before speechToText")

            Log.d("VoiceDebug", "after speechToText, transcript = $transcript")

            if (transcript.isBlank()) {
                throw IllegalStateException("Transcript is empty")
            }

            Log.d("VoiceDebug", "before uploadVoiceReference")
            val voiceResp = videoManager.uploadVoiceReference(
                localVoicePath = audioFile.absolutePath,
                characterId = characterId,
                transcript = transcript
            )
            Log.d(
                "VoiceDebug",
                "after uploadVoiceReference, voice = ${voiceResp.character_voice}, txt = ${voiceResp.character_txt}"
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("VoiceDebug", "uploadReferenceAssets failed: ${e.message}", e)
            Result.failure(e)
        }
    }
}
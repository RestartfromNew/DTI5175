package com.example.chatpart.data

import android.util.Log
import com.example.chatpart.api.GenerateVideoRequest
import com.example.chatpart.api.GenerateVideoResponse
import com.example.chatpart.api.VideoClient
import com.example.chatpart.domain.Profile
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class VideoManager {

    suspend fun uploadAvatar(localAvatarPath: String): String {
        val file = File(localAvatarPath)

        require(file.exists()) { "Avatar file does not exist: $localAvatarPath" }

        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val avatarPart = MultipartBody.Part.createFormData(
            name = "avatar",
            filename = file.name,
            body = requestFile
        )

        val response = VideoClient.api.uploadAvatar(avatarPart)
        Log.d("AvatarDebug", "server avatar_path = ${response.avatar_path}")
        return response.avatar_path
    }

    suspend fun generateVideo(profile: Profile, replyText: String): GenerateVideoResponse {

        android.util.Log.d("AvatarDebug", "generateVideo called")
        android.util.Log.d("AvatarDebug", "profile.id = ${profile.id}")
        android.util.Log.d("AvatarDebug", "profile.customAvatarPath = ${profile.customAvatarPath}")
        android.util.Log.d("AvatarDebug", "replyText = $replyText")


        var serverAvatarPath: String? = null

        if (!profile.customAvatarPath.isNullOrBlank()) {
            try {
                serverAvatarPath = uploadAvatar(profile.customAvatarPath)
            } catch (e: Exception) {
                Log.e("AvatarDebug", "upload avatar failed: ${e.message}", e)
            }
        }

        val request = GenerateVideoRequest(
            character_id = profile.id,
            reply_text = replyText,
            avatar_path = serverAvatarPath
        )

        Log.d("AvatarDebug", "generateVideo avatar_path = $serverAvatarPath")

        return VideoClient.api.generateVideo(request)
    }
}
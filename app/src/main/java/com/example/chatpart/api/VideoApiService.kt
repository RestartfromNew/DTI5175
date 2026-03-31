package com.example.chatpart.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface VideoApiService {

    @Multipart
    @POST("upload_avatar")
    suspend fun uploadAvatar(
        @Part avatar: MultipartBody.Part,
        @Part("character_id") characterId: RequestBody
    ): UploadAvatarResponse

    @Multipart
    @POST("upload_voice_reference")
    suspend fun uploadVoiceReference(
        @Part voice: MultipartBody.Part,
        @Part("character_id") characterId: RequestBody,
        @Part("transcript") transcript: RequestBody
    ): UploadVoiceAndTextResponse

    @POST("generate_video")
    suspend fun generateVideo(
        @retrofit2.http.Body request: GenerateVideoRequest
    ): GenerateVideoResponse
}
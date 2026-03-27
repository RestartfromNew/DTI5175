package com.example.chatpart.api


import retrofit2.http.Body
import retrofit2.http.POST
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.Part

interface VideoApiService {
    @Multipart
    @POST("upload_avatar")
    suspend fun uploadAvatar(
        @Part avatar: MultipartBody.Part
    ): UploadAvatarResponse

    @POST("generate_video")
    suspend fun generateVideo(
        @retrofit2.http.Body request: GenerateVideoRequest
    ): GenerateVideoResponse
}
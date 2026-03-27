package com.example.chatpart.api


data class GenerateVideoRequest(
    val character_id: String,
    val reply_text: String,
    val avatar_path: String? = null
)

data class GenerateVideoResponse(
    val message: String,
    val task_id: String,
    val video_url: String,
    val audio_url: String
)
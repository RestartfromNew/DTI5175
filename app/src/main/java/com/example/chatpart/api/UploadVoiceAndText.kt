package com.example.chatpart.api

data class UploadVoiceAndTextResponse(
    val message: String,
    val character_voice: String,
    val character_txt: String
)
package com.example.chatpart.domain

interface Chat {
    suspend fun sendMessage (
        profile: Profile,
        history: List<Message>,
        userText: String
    ): Result
}
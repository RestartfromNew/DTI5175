package com.example.chatpart.domain

data class Message(
    val role: Role,
    val content: String,
)

enum class Role {USER, ASSISTANT}
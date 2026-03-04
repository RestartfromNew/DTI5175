package com.example.chatpart.domain

data class Profile (
    val id: String,
    val name: String,
    val gender: String,
    val background: String,
    val relationship: String,
    val personality: String = "",
    val voiceId: String? = null,
    val facetimeUrl: String? = null,
    val speakStyle: List<String> = emptyList(),
    val doRules: List<String> = emptyList(),
    val dontRules: List<String> = emptyList(),
)
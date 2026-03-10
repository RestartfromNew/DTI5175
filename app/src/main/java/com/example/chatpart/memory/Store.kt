package com.example.chatpart.memory

data class MemoryItem (
    val id: String,
    val profileId: String,
    val text: String,
    val embedding: FloatArray? = null
)

interface MemoryStore {
    suspend fun add(profileId: String, text: String, embedding: FloatArray? = null): MemoryItem
    suspend fun search(profileId: String, queryEmbedding: FloatArray, topK: Int = 5): List<MemoryItem>
}

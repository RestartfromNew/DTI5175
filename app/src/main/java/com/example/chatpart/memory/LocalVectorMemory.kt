package com.example.chatpart.memory

import com.example.chatpart.llm.EmbeddingClient
import java.util.UUID
import kotlin.math.sqrt

class LocalVectorMemory (
    private val embedder: EmbeddingClient
): MemoryStore {
    private val items = mutableListOf<MemoryItem>()

    override suspend fun add(profileId: String, text: String, embedding: FloatArray?): MemoryItem {
        val emb = embedding ?: embedder.embed(text)
        val item = MemoryItem(UUID.randomUUID().toString(), profileId, text, emb)
        items.add(item)
        return item
    }

    override suspend fun search(profileId: String, query: FloatArray, topK: Int): List<MemoryItem> {

        return items
            .asSequence()
            .filter { it.profileId == profileId && it.embedding != null }
            .map { it to cosine(it.embedding!!, query) }
            .sortedByDescending { it.second }
            .take(topK)
            .map { it.first }
            .toList()
    }
    private fun cosine(a: FloatArray, b: FloatArray): Double {
        var dot = 0.0
        var na = 0.0
        var nb = 0.0
        val n = minOf(a.size, b.size)
        for (i in 0 until n) {
            dot += a[i] * b[i]
            na += a[i] * a[i]
            nb += b[i] * b[i]
        }
        val denom = sqrt(na) * sqrt(nb)
        return if (denom == 0.0) 0.0 else dot / denom
    }
}
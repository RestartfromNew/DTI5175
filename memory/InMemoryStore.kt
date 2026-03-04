package com.example.chatpart.memory


import java.util.UUID


class InMemoryStore: MemoryStore {
    private val items = mutableListOf<MemoryItem>()

    override suspend fun add(profileId: String, text: String, embedding: FloatArray?): MemoryItem {
        val item = MemoryItem (
            id = UUID.randomUUID().toString(),
            profileId = profileId,
            text = text,
            embedding = embedding
        )
        items.add(item)
        return item
    }

    override suspend fun search(profileId: String, queryEmbedding: FloatArray, topK: Int): List<MemoryItem> {

        return items
            .asSequence()
            .filter { it.profileId == profileId}
            .map {  it to score(it.embedding!!, queryEmbedding) }
            .sortedByDescending { it.second }
            .take(topK)
            .map { it.first }
            .toList()
    }

    private fun score(vectorA: FloatArray, vectorB: FloatArray): Float {
        if (vectorA.size != vectorB.size) return 0.0f

        var dotProduct = 0.0f
        var normA = 0.0f
        var normB = 0.0f
        for (i in vectorA.indices) {
            dotProduct += vectorA[i] * vectorB[i]
            normA += vectorA[i] * vectorA[i]
            normB += vectorB[i] * vectorB[i]
        }
        return dotProduct / (Math.sqrt(normA.toDouble()) * Math.sqrt(normB.toDouble())).toFloat()
    }
}
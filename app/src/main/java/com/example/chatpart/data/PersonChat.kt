package com.example.chatpart.data

import android.util.Log
import com.example.chatpart.domain.Chat
import com.example.chatpart.domain.Result
import com.example.chatpart.domain.Message
import com.example.chatpart.domain.Profile
import com.example.chatpart.memory.MemoryStore
import com.example.chatpart.llm.LlmClient
import com.example.chatpart.llm.EmbeddingClient

class PersonChat (
    private val llm: LlmClient,
    private val memory: MemoryStore,
    private val embeddingClient: EmbeddingClient
) : Chat {
    private val memRegex = Regex("\\[MEM:\\s*(.*?)\\]")
    override suspend fun sendMessage (p:Profile, history: List<Message>, userText: String) : Result {
        val queryVector = embeddingClient.embed(userText)
        val retrieved = memory.search(profileId = p.id, queryEmbedding = queryVector, topK = 5)
        Log.d("ChatTest", "Recalled ${retrieved.size} memories: ${retrieved.joinToString { it.text }}")

        val summary = if (retrieved.isEmpty()) null else retrieved.joinToString("\n") { "- ${it.text}" }

        val systemPrompt = Prompt.buildPrompt(p, summary)
        Log.d("ChatTest", "Final SystemPrompt: $systemPrompt")
        val dialogue = Prompt.buildDialog(history, userText)
        val dialogueText = dialogue.joinToString("\n") { (role, text) ->
            "$role: $text"
        }

        val assistant = llm.reply(systemPrompt, userText = dialogueText)
        val rawReply = assistant.replyText
        val match = memRegex.find(rawReply)
        if (match != null) {
            val extractedFact = match.groupValues[1].trim()
            Log.d("ChatTest", "get the memory: $extractedFact")

            val factVector = embeddingClient.embed(extractedFact)
            memory.add(p.id,"User's secret: $extractedFact",factVector)
        }
        val cleanReply = rawReply.replace(memRegex, "").trim()

        val memoryText = "User said: $userText"
        val userMemoryVector = embeddingClient.embed(memoryText)



        memory.add(
            profileId = p.id,
            text = memoryText,
            embedding = userMemoryVector
        )

        val aiText = "${p.name} replied: $cleanReply"
        val aiMemoryVector = embeddingClient.embed(aiText)

        memory.add(
            profileId = p.id,
            text = aiText,
            embedding = aiMemoryVector
        )

        return assistant.copy(cleanReply)
    }
}
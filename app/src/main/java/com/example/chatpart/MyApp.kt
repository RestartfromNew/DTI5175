package com.example.chatpart

import android.util.Log
import com.example.chatpart.domain.Message
import com.example.chatpart.domain.Profile
import com.example.chatpart.domain.Role
import android.app.Application
import com.example.chatpart.llm.EmbeddingLlm
import com.example.chatpart.memory.InMemoryStore
import com.example.chatpart.data.PersonChat
import com.example.chatpart.llm.AITest

class MyApp : Application() {
    lateinit var personChat: PersonChat
        private set

    override fun onCreate() {
        super.onCreate()

        val embedding = EmbeddingLlm()
        val memory = InMemoryStore()
        val llm = AITest()

        // 2. 组装成全局唯一的引擎
        personChat = PersonChat(llm, memory, embedding)
    }
}
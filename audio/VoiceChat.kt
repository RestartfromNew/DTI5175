package com.example.chatpart.audio

import com.example.chatpart.data.PersonChat
import com.example.chatpart.domain.Message
import com.example.chatpart.domain.Profile

class VoiceChat (
    private val brain: PersonChat,
    private val audioClient: AudioClient
) {
    suspend fun sendVoiceMsg(p: Profile, history: List<Message>, userAudio: ByteArray): VoiceResult {
        val userText = audioClient.voiceToText(userAudio)
        val chatReply = brain.sendMessage(p, history, userText)

        val voicePath = audioClient.textToVoice(
            text = chatReply.replyText,
            voiceId = p.voiceId ?: "default",
            emotion = chatReply.emotion
        )

        return VoiceResult(
            userText = userText,
            aiText = chatReply.replyText,
            aiVoicePath = voicePath,
            emotion = chatReply.emotion
        )

    }
}

data class VoiceResult(
    val userText: String,
    val aiText: String,
    val aiVoicePath: String,
    val emotion: String
)


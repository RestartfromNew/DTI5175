package com.example.chatpart.audio

import com.example.chatpart.data.PersonChat
import com.example.chatpart.domain.Message
import com.example.chatpart.domain.Profile
import kotlinx.coroutines.CompletableDeferred

class VoiceChat (
    private val brain: PersonChat,
    private val audioClient: AudioClient
) {
    suspend fun sendVoiceMsg(p: Profile, history: List<Message>, userAudio: ByteArray, languageBoost: String = ""): VoiceResult {
        // STT now uses live microphone via SpeechRecognizer (ignores userAudio parameter)
        // userAudio is kept for API compatibility but not used
        val userText = CompletableDeferred<String>()
        audioClient.voiceToText { text ->
            userText.complete(text)
        }
        val transcribedText = userText.await()

        val chatReply = brain.sendMessage(p, history, transcribedText)

        val voicePath = audioClient.textToVoice(
            text = chatReply.replyText,
            voiceId = p.voiceId ?: "default",
            emotion = chatReply.emotion,
            languageBoost = languageBoost
        )

        return VoiceResult(
            userText = transcribedText,
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


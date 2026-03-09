package com.example.chatpart.audio

interface AudioClient {
    /**
     * @param text change to text
     * @return path of audio file
     */
    suspend fun textToVoice(text: String, voiceId: String, emotion: String): String

    /**
     * @param audioData record voice
     * @return recognize the words
     */
    suspend fun voiceToText(audioData: ByteArray): String
}
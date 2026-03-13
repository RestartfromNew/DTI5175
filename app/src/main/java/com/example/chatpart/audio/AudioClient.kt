package com.example.chatpart.audio

interface AudioClient {
    /**
     * @param text change to text
     * @return path of audio file
     */
    suspend fun textToVoice(text: String, voiceId: String, emotion: String, languageBoost: String = ""): String

    /**
     * Converts speech to text using Android SpeechRecognizer.
     * NOTE: MiniMax has no STT endpoint, so this uses Android's built-in recognizer
     * to directly capture microphone input.
     *
     * @param onResult Callback with the transcribed text
     */
    suspend fun voiceToText(onResult: (String) -> Unit)
}
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

    /**
     * Starts continuous speech recognition with callbacks for partial results and RMS changes.
     * Must be called from main thread.
     *
     * @param onPartialResult Called when partial speech is recognized
     * @param onRmsChanged Called when audio level changes
     * @param onResult Called when speech recognition completes with final result
     */
    fun startListeningWithCallbacks(
        onPartialResult: ((String) -> Unit)? = null,
        onRmsChanged: ((Float) -> Unit)? = null,
        onResult: ((String) -> Unit)? = null
    )

    /**
     * Stops the current speech recognition session.
     */
    fun stopListening()
}
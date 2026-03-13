package com.example.chatpart.api

/**
 * MiniMax API configuration constants
 * Part 2 - API Constants
 */
object MiniMaxConfig {
    // Base URL for MiniMax API
    const val BASE_URL = "https://api.minimax.io"

    // TTS models: turbo = faster/cheaper, hd = better quality
    const val TTS_MODEL = "speech-2.6-turbo"

    // Voice clone model
    const val CLONE_MODEL = "speech-2.6-hd"

    // Audio output settings for TTS
    const val AUDIO_FORMAT = "mp3"
    const val AUDIO_SAMPLE_RATE = 32000
    const val AUDIO_BITRATE = 128000
    const val AUDIO_CHANNEL = 1

    // Output format: "hex" (hex-encoded mp3) or "url" (download URL)
    const val OUTPUT_FORMAT = "hex"

    // Voice clone requirements
    const val VOICE_CLONE_MIN_DURATION_SEC = 10
    const val VOICE_CLONE_MAX_DURATION_SEC = 300  // 5 minutes
    const val VOICE_CLONE_MAX_SIZE_MB = 20
}

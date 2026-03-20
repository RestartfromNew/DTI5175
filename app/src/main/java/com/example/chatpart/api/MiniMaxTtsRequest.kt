package com.example.chatpart.api

import com.google.gson.annotations.SerializedName

/**
 * MiniMax TTS API request/response data classes
 * Part 3.1 - TTS Request Data Classes
 */

// ==================== Request ====================

data class MiniMaxTtsRequest(
    @SerializedName("model")
    val model: String,

    @SerializedName("text")
    val text: String,

    @SerializedName("voice_setting")
    val voice_setting: VoiceSetting,

    @SerializedName("audio_setting")
    val audio_setting: AudioSetting,

    @SerializedName("output_format")
    val output_format: String = "hex"
)

data class VoiceSetting(
    @SerializedName("voice_id")
    val voice_id: String,

    @SerializedName("speed")
    val speed: Float = 1.0f,

    @SerializedName("vol")
    val vol: Float = 1.0f,

    @SerializedName("pitch")
    val pitch: Int = 0,

    @SerializedName("emotion")
    val emotion: String? = null,

    @SerializedName("language_boost")
    val language_boost: String? = null  // e.g., "Chinese" for better Chinese TTS
)

data class AudioSetting(
    @SerializedName("format")
    val format: String = "mp3",

    @SerializedName("sample_rate")
    val sample_rate: Int = 32000,

    @SerializedName("bitrate")
    val bitrate: Int = 128000,

    @SerializedName("channel")
    val channel: Int = 1
)

// ==================== Response ====================

data class MiniMaxTtsResponse(
    @SerializedName("data")
    val data: TtsData?,

    @SerializedName("base_resp")
    val base_resp: BaseResp
)

data class TtsData(
    @SerializedName("audio")
    val audio: String,  // hex-encoded mp3 bytes

    @SerializedName("status")
    val status: Int    // 2 = complete
)

data class BaseResp(
    @SerializedName("status_code")
    val status_code: Int,

    @SerializedName("status_msg")
    val status_msg: String
)

// ==================== STT ====================

data class MiniMaxSttCreateResponse(
    @SerializedName("generation_id")
    val generation_id: String?,

    @SerializedName("base_resp")
    val base_resp: BaseResp?
)

data class MiniMaxSttPollResponse(
    @SerializedName("status")
    val status: String?,       // "Processing" | "Succeeded" | "Failed"

    @SerializedName("file")
    val file: SttResultFile?,

    @SerializedName("base_resp")
    val base_resp: BaseResp?
)

data class SttResultFile(
    @SerializedName("transcription")
    val transcription: SttTranscription?
)

data class SttTranscription(
    @SerializedName("full_text")
    val full_text: String?
)

package com.example.chatpart.api

/**
 * Bot video source — controls what plays in the main video area of LiveVoiceScreen.
 *
 * Usage:
 *   None        → energy ball animation (default, no video)
 *   LocalSample → loop bot_sample.mp4 (placeholder while WebRTC is not connected)
 *   WebRTC      → live stream from GPU server via WebSocket signaling
 *
 * To switch to real WebRTC, change the video toggle in LiveVoiceScreen:
 *   BotVideoSource.WebRTC(signalingUrl = "ws://your-gpu-server/webrtc")
 */
sealed interface BotVideoSource {
    data object None : BotVideoSource
    data object AvatarImage : BotVideoSource
    data object LocalSample : BotVideoSource
    data class RemoteVideo(val url: String) : BotVideoSource
}

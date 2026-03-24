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
    /** No video — show animated energy ball */
    data object None : BotVideoSource

    /** Loop local sample.mp4 as placeholder */
    data object LocalSample : BotVideoSource

    /**
     * Live WebRTC stream from GPU server.
     *
     * Signaling protocol (JSON over WebSocket):
     *   Client → Server: {"type": "offer",  "sdp": "..."}
     *   Server → Client: {"type": "answer", "sdp": "..."}
     *   Bidirectional:   {"type": "ice", "candidate": "...", "sdpMid": "...", "sdpMLineIndex": 0}
     *
     * @param signalingUrl  WebSocket URL of your signaling server
     *                      e.g. "ws://192.168.1.100:8080/webrtc"
     */
    data class WebRTC(val signalingUrl: String) : BotVideoSource
}

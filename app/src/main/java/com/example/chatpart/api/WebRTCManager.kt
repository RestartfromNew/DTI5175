package com.example.chatpart.api

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.VideoSink
import org.webrtc.VideoTrack
import java.util.concurrent.TimeUnit

/**
 * Manages a WebRTC peer connection for receiving bot video from the GPU server.
 *
 * Flow:
 *  1. Call initialize(eglBase)
 *  2. Call connect(signalingUrl, videoSink) — videoSink is a SurfaceViewRenderer
 *  3. Signaling happens automatically (offer → answer → ICE candidates)
 *  4. onConnected fires when remote video track arrives
 *  5. Call disconnect() / release() on teardown
 *
 * The GPU server is the "answerer" — it generates video and sends it as a VideoTrack.
 * This client is the "offerer" — it receives and renders the remote video.
 *
 * Signaling message format (JSON over WebSocket):
 *   {"type": "offer",  "sdp": "..."}            ← we send
 *   {"type": "answer", "sdp": "..."}            ← server sends
 *   {"type": "ice", "candidate": "...",
 *    "sdpMid": "...", "sdpMLineIndex": 0}        ← bidirectional
 */
class WebRTCManager(private val context: Context) {

    /** Called when remote video track arrives and is being rendered */
    var onConnected: (() -> Unit)? = null

    /** Called when connection drops */
    var onDisconnected: (() -> Unit)? = null

    /** Called on signaling or connection error */
    var onError: ((String) -> Unit)? = null

    private var factory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var signalingWs: WebSocket? = null
    private var eglBase: EglBase? = null
    private var remoteSink: VideoSink? = null

    private val gson = Gson()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS) // WebSocket — no read timeout
        .build()

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Must be called once before connect().
     * Pass an EglBase created from Compose (EglBase.create()).
     */
    fun initialize(eglBase: EglBase) {
        this.eglBase = eglBase
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions
                .builder(context)
                .createInitializationOptions()
        )
        factory = PeerConnectionFactory.builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .setVideoEncoderFactory(
                DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
            )
            .createPeerConnectionFactory()
    }

    /**
     * Open signaling WebSocket and start the WebRTC handshake.
     *
     * @param signalingUrl  ws:// or wss:// URL of your GPU server's signaling endpoint
     * @param videoSink     SurfaceViewRenderer to render the remote bot video
     */
    fun connect(signalingUrl: String, videoSink: VideoSink) {
        remoteSink = videoSink
        val request = Request.Builder().url(signalingUrl).build()
        signalingWs = httpClient.newWebSocket(request, SignalingListener())
    }

    /** Stop streaming and close WebSocket. Safe to call multiple times. */
    fun disconnect() {
        signalingWs?.close(1000, "User ended call")
        signalingWs = null
        peerConnection?.close()
        peerConnection = null
        remoteSink = null
    }

    /** Full teardown — call when the screen is destroyed (DisposableEffect). */
    fun release() {
        disconnect()
        factory?.dispose()
        factory = null
        eglBase?.release()
        eglBase = null
    }

    // ── Signaling ─────────────────────────────────────────────────────────────

    private inner class SignalingListener : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
            // WebSocket connected → create PeerConnection and send SDP offer
            peerConnection = buildPeerConnection() ?: return
            peerConnection!!.createOffer(object : SdpAdapter() {
                override fun onCreateSuccess(sdp: SessionDescription) {
                    peerConnection!!.setLocalDescription(SdpAdapter(), sdp)
                    val msg = gson.toJson(mapOf("type" to "offer", "sdp" to sdp.description))
                    webSocket.send(msg)
                }
            }, MediaConstraints())
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            val json = runCatching { gson.fromJson(text, JsonObject::class.java) }.getOrNull()
                ?: return
            when (json.get("type")?.asString) {
                "answer" -> {
                    val sdp = SessionDescription(
                        SessionDescription.Type.ANSWER,
                        json.get("sdp").asString
                    )
                    peerConnection?.setRemoteDescription(SdpAdapter(), sdp)
                }
                "ice" -> {
                    val candidate = IceCandidate(
                        json.get("sdpMid").asString,
                        json.get("sdpMLineIndex").asInt,
                        json.get("candidate").asString
                    )
                    peerConnection?.addIceCandidate(candidate)
                }
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            onDisconnected?.invoke()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
            onError?.invoke(t.message ?: "Signaling error")
        }
    }

    // ── PeerConnection ────────────────────────────────────────────────────────

    private fun buildPeerConnection(): PeerConnection? {
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )
        val config = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        return factory?.createPeerConnection(config, object : PeerConnection.Observer {

            override fun onIceCandidate(candidate: IceCandidate) {
                val msg = gson.toJson(
                    mapOf(
                        "type" to "ice",
                        "candidate" to candidate.sdp,
                        "sdpMid" to candidate.sdpMid,
                        "sdpMLineIndex" to candidate.sdpMLineIndex
                    )
                )
                signalingWs?.send(msg)
            }

            override fun onTrack(transceiver: RtpTransceiver) {
                val track = transceiver.receiver?.track()
                if (track is VideoTrack) {
                    remoteSink?.let { track.addSink(it) }
                    onConnected?.invoke()
                }
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
                when (newState) {
                    PeerConnection.PeerConnectionState.FAILED,
                    PeerConnection.PeerConnectionState.DISCONNECTED -> onDisconnected?.invoke()
                    else -> {}
                }
            }

            // Required no-ops
            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {}
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
            override fun onAddStream(p0: MediaStream?) {}
            override fun onRemoveStream(p0: MediaStream?) {}
            override fun onDataChannel(p0: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {}
        })
    }
}

/** No-op SdpObserver to reduce boilerplate */
open class SdpAdapter : SdpObserver {
    override fun onCreateSuccess(sdp: SessionDescription) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(error: String?) {}
    override fun onSetFailure(error: String?) {}
}

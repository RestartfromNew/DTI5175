package com.example.chatpart.audio

import android.Manifest
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.util.Log
import androidx.annotation.RequiresPermission
import com.example.chatpart.api.DeepgramAsrClient
import com.example.chatpart.api.MiniMaxAudioClient
import com.example.chatpart.data.PersonChat
import com.example.chatpart.domain.Message
import com.example.chatpart.domain.Profile
import com.example.chatpart.i18n.Languages
import com.example.chatpart.screens.LiveVoiceState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * LiveVoice 业务控制器
 *
 * 支持两种模式：
 *   Push-to-Talk — 按住录音，松开发送（startListening / stopListening）
 *   Free Talk    — VAD 自动检测说话/静默，无需按键（startFreeMode / stopFreeMode）
 *
 * 共用管道：WAV → Deepgram ASR → Gemini AI → MiniMax TTS
 */
class LiveVoiceController(
    private val context: Context,
    private val brain: PersonChat,
    private val audioClient: MiniMaxAudioClient,
    private val scope: CoroutineScope,
    private val currentLanguage: String = "en"
) {
    private fun t(key: String) = Languages.getString(currentLanguage, key)

    companion object {
        private const val TAG = "LiveVoiceController"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT_PCM = AudioFormat.ENCODING_PCM_16BIT

        // VAD 参数
        private const val VAD_SPEECH_RMS = 800f      // RMS 超过此值 = 有人说话
        private const val VAD_SILENCE_MS = 800L      // 静默超过 800ms → 停止录音
        private const val VAD_MIN_SPEECH_MS = 400L   // 最短说话时长，避免误触
        private const val VAD_MAX_SPEECH_MS = 30_000L // 最长单次说话 30s
    }

    // UI callbacks
    var onStateChanged: ((LiveVoiceState) -> Unit)? = null
    var onUserCaptionUpdated: ((String) -> Unit)? = null
    var onAICaptionUpdated: ((String) -> Unit)? = null
    var onAmplitudesUpdated: ((List<Float>) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    private val asrClient = DeepgramAsrClient()
    private var mediaPlayer: MediaPlayer? = null
    private var currentJob: Job? = null
    private var vadJob: Job? = null
    private var isReleased = false

    @Volatile private var isRecording = false   // PTT 录音标志
    @Volatile private var isVadActive = false   // VAD 循环运行标志
    @Volatile private var isVadBlocked = false  // AI 说话/处理中，VAD 暂停捕获

    // ── Public API ────────────────────────────────────────────────────────────

    // ── Push-to-Talk ─────────────────────────────────────────────────────────

    /** 用户按下麦克风 — 开始录音 */
    fun startListening(profile: Profile, history: List<Message>) {
        if (isReleased) return
        currentJob?.cancel()
        currentJob = scope.launch {
            withContext(Dispatchers.Main) {
                onStateChanged?.invoke(LiveVoiceState.USER_SPEAKING)
                onUserCaptionUpdated?.invoke("")
                onAICaptionUpdated?.invoke("")
                onAmplitudesUpdated?.invoke(List(32) { 0.1f })
            }
            val wavFile = withContext(Dispatchers.IO) { recordAudio() }
            withContext(Dispatchers.Main) { onAmplitudesUpdated?.invoke(List(32) { 0.3f }) }
            processWavFile(wavFile, profile, history)
        }
    }

    // ── Free Talk (VAD) ──────────────────────────────────────────────────────

    /**
     * 启动自由通话模式 — VAD 循环自动检测说话/静默
     * @param historyProvider 返回最新对话历史的 lambda（每次说话都会调用拿到最新消息）
     */
    fun startFreeMode(profile: Profile, historyProvider: () -> List<Message>) {
        if (isReleased) return
        stopFreeMode()  // 确保旧的 VAD 循环已停止
        isVadActive = true
        isVadBlocked = false
        vadJob = scope.launch(Dispatchers.IO) {
            runVadLoop(profile, historyProvider)
        }
    }

    /** 停止自由通话模式 */
    fun stopFreeMode() {
        isVadActive = false
        isVadBlocked = false
        vadJob?.cancel()
        vadJob = null
        scope.launch(Dispatchers.Main) {
            onStateChanged?.invoke(LiveVoiceState.IDLE)
            onAmplitudesUpdated?.invoke(List(32) { 0.1f })
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private suspend fun runVadLoop(profile: Profile, historyProvider: () -> List<Message>) {
        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT_PCM)
            .coerceAtLeast(3200)
        val audioRecord = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT_PCM, bufferSize * 4
            )
        } catch (e: Exception) {
            Log.e(TAG, "VAD AudioRecord init failed: ${e.message}"); return
        }
        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord.release(); return
        }

        val chunk = ByteArray(bufferSize)
        var capturing = false
        var speechBuffer = ByteArrayOutputStream()
        var silenceStartMs = 0L
        var speechStartMs = 0L

        audioRecord.startRecording()
        Log.d(TAG, "VAD loop started")

        try {
            while (isVadActive) {
                val read = audioRecord.read(chunk, 0, chunk.size)
                if (read <= 0) continue

                val rms = computeRms(chunk, read)
                val now = System.currentTimeMillis()

                // AI 正在说话 / 处理中 — 排空录音，不处理
                if (isVadBlocked) {
                    if (capturing) {
                        capturing = false
                        speechBuffer.reset()
                        silenceStartMs = 0L
                    }
                    continue
                }

                if (!capturing) {
                    // 待机状态 — 微弱波形显示"正在聆听"
                    val normalized = (rms / 10000f).coerceIn(0.02f, 0.22f)
                    withContext(Dispatchers.Main) {
                        onAmplitudesUpdated?.invoke(generateAmplitudesFromRms(normalized))
                    }
                    if (rms > VAD_SPEECH_RMS) {
                        // 检测到说话 — 开始录
                        capturing = true
                        speechBuffer.reset()
                        speechBuffer.write(chunk, 0, read)
                        speechStartMs = now
                        silenceStartMs = 0L
                        withContext(Dispatchers.Main) {
                            onStateChanged?.invoke(LiveVoiceState.USER_SPEAKING)
                        }
                    }
                } else {
                    // 录音中
                    speechBuffer.write(chunk, 0, read)
                    val normalized = (rms / 10000f).coerceIn(0.05f, 1f)
                    withContext(Dispatchers.Main) {
                        onAmplitudesUpdated?.invoke(generateAmplitudesFromRms(normalized))
                    }

                    val elapsed = now - speechStartMs
                    if (rms < VAD_SPEECH_RMS) {
                        if (silenceStartMs == 0L) silenceStartMs = now
                        val silenced = now - silenceStartMs
                        if (silenced >= VAD_SILENCE_MS || elapsed >= VAD_MAX_SPEECH_MS) {
                            // 静默超时 / 达到最长时长 — 结束这段话
                            capturing = false
                            val pcmData = speechBuffer.toByteArray()
                            speechBuffer.reset()
                            silenceStartMs = 0L

                            if (elapsed >= VAD_MIN_SPEECH_MS && pcmData.size >= 3200) {
                                isVadBlocked = true
                                val wavFile = writePcmToWav(pcmData)
                                withContext(Dispatchers.Main) {
                                    onAmplitudesUpdated?.invoke(List(32) { 0.3f })
                                }
                                processWavFile(wavFile, profile, historyProvider())
                                // processWavFile 里 playAudio 结束后会回调 onStateChanged(IDLE)
                                // 但 VAD 需要在 TTS 播完后才解除 block
                                // 通过 awaitVadUnblock() 等待
                            } else {
                                withContext(Dispatchers.Main) {
                                    onStateChanged?.invoke(LiveVoiceState.IDLE)
                                }
                            }
                        }
                    } else {
                        silenceStartMs = 0L  // 重置静默计时
                    }
                }
            }
        } finally {
            audioRecord.stop()
            audioRecord.release()
            Log.d(TAG, "VAD loop stopped")
        }
    }

    // ── Shared pipeline: WAV → ASR → AI → TTS ────────────────────────────────

    /**
     * 共用处理管道，PTT 和 VAD 都调用这里。
     * 结束后自动解除 isVadBlocked（如果 VAD 模式在运行）。
     */
    private suspend fun processWavFile(
        wavFile: File?,
        profile: Profile,
        history: List<Message>
    ) {
        if (wavFile == null || wavFile.length() < 2000) {
            Log.w(TAG, "Recording too short or failed (size=${wavFile?.length()})")
            withContext(Dispatchers.Main) {
                onError?.invoke(t("LIVE_RECORD_TOO_SHORT"))
                onStateChanged?.invoke(LiveVoiceState.IDLE)
                onAmplitudesUpdated?.invoke(List(32) { 0.1f })
            }
            wavFile?.delete()
            isVadBlocked = false
            return
        }

        withContext(Dispatchers.Main) { onStateChanged?.invoke(LiveVoiceState.PROCESSING) }

        val finalText = try {
            withContext(Dispatchers.IO) { asrClient.transcribe(wavFile) }
        } catch (e: Exception) {
            Log.e(TAG, "ASR exception", e)
            wavFile.delete()
            withContext(Dispatchers.Main) {
                onError?.invoke(t("LIVE_SPEECH_NOT_RECOGNIZED"))
                onStateChanged?.invoke(LiveVoiceState.IDLE)
                onAmplitudesUpdated?.invoke(List(32) { 0.1f })
            }
            isVadBlocked = false
            return
        }
        wavFile.delete()
        Log.d(TAG, "ASR result: \"$finalText\"")

        if (finalText.isBlank()) {
            withContext(Dispatchers.Main) {
                onError?.invoke(t("LIVE_SPEECH_NOT_RECOGNIZED"))
                onStateChanged?.invoke(LiveVoiceState.IDLE)
                onAmplitudesUpdated?.invoke(List(32) { 0.1f })
            }
            isVadBlocked = false
            return
        }

        withContext(Dispatchers.Main) { onUserCaptionUpdated?.invoke(finalText) }

        try {
            val chatReply = withContext(Dispatchers.IO) {
                brain.sendMessage(profile, history, finalText)
            }
            val aiText = chatReply.replyText

            val voicePath = withContext(Dispatchers.IO) {
                audioClient.textToVoice(
                    text = aiText,
                    voiceId = profile.voiceId ?: "",
                    emotion = "",
                    languageBoost = ""
                )
            }

            withContext(Dispatchers.Main) {
                onAICaptionUpdated?.invoke(aiText)
                onStateChanged?.invoke(LiveVoiceState.AI_SPEAKING)
            }

            playAudio(voicePath) {
                if (!isReleased) {
                    scope.launch(Dispatchers.Main) {
                        isVadBlocked = false   // ← VAD 解除 block，恢复聆听
                        onStateChanged?.invoke(LiveVoiceState.IDLE)
                        onAmplitudesUpdated?.invoke(List(32) { 0.1f })
                        delay(3000)
                        if (!isReleased) {
                            onUserCaptionUpdated?.invoke("")
                            onAICaptionUpdated?.invoke("")
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Voice chat error", e)
            withContext(Dispatchers.Main) {
                onError?.invoke(t("LIVE_ERROR").replace("{message}", e.message ?: e.javaClass.simpleName))
                onStateChanged?.invoke(LiveVoiceState.IDLE)
                onAmplitudesUpdated?.invoke(List(32) { 0.1f })
            }
            isVadBlocked = false
        }
    }

    /**
     * Called when user releases mic button.
     * Signals the recording loop to stop, then processes the audio.
     */
    fun stopListening() {
        if (isReleased) return
        Log.d(TAG, "Stop recording signal sent")
        isRecording = false
    }

    /**
     * Called when user swipes mic button to the trash icon (cancel zone).
     * Stops recording WITHOUT processing the audio — discards the take entirely.
     */
    fun cancelListening() {
        if (isReleased) return
        Log.d(TAG, "Cancel recording — audio discarded")
        isRecording = false
        currentJob?.cancel()
        currentJob = null
        scope.launch(Dispatchers.Main) {
            onStateChanged?.invoke(LiveVoiceState.IDLE)
            onAmplitudesUpdated?.invoke(List(32) { 0.1f })
            onUserCaptionUpdated?.invoke("")
        }
    }

    fun release() {
        isReleased = true
        isRecording = false
        isVadActive = false
        isVadBlocked = false
        currentJob?.cancel()
        currentJob = null
        vadJob?.cancel()
        vadJob = null
        mediaPlayer?.release()
        mediaPlayer = null
        Log.d(TAG, "Controller released")
    }

    // ── Recording ─────────────────────────────────────────────────────────────

    /**
     * Blocks the calling thread until isRecording becomes false (set by stopListening()).
     * Returns a WAV File, or null if something went wrong.
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private suspend fun recordAudio(): File? {
        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT_PCM)
        if (bufferSize <= 0) {
            Log.e(TAG, "Invalid AudioRecord buffer size: $bufferSize")
            return null
        }

        val audioRecord = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,  // optimized for speech
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT_PCM,
                bufferSize * 4   // larger buffer to avoid overrun
            )
        } catch (e: Exception) {
            Log.e(TAG, "AudioRecord init error: ${e.message}")
            return null
        }

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord not initialized")
            audioRecord.release()
            return null
        }

        val pcmBuffer = ByteArrayOutputStream()
        val chunk = ByteArray(bufferSize)
        isRecording = true
        audioRecord.startRecording()
        Log.d(TAG, "Recording started (bufferSize=$bufferSize)")

        try {
            while (isRecording) {
                val read = audioRecord.read(chunk, 0, chunk.size)
                if (read > 0) {
                    pcmBuffer.write(chunk, 0, read)

                    // Compute RMS → normalize → drive waveform animation
                    val rms = computeRms(chunk, read)
                    val normalized = (rms / 10000f).coerceIn(0.05f, 1f)
                    
                    // UI update for waveform must be on Main thread
                    withContext(Dispatchers.Main) {
                        onAmplitudesUpdated?.invoke(generateAmplitudesFromRms(normalized))
                    }
                }
            }
        } finally {
            audioRecord.stop()
            audioRecord.release()
            Log.d(TAG, "Recording stopped — ${pcmBuffer.size()} PCM bytes captured")
        }

        val pcmData = pcmBuffer.toByteArray()

        // Minimum ~100ms of audio at 16kHz 16-bit = 3200 bytes
        if (pcmData.size < 3200) {
            Log.w(TAG, "Too little audio captured (${pcmData.size} bytes)")
            return null
        }

        return writePcmToWav(pcmData)
    }

    // ── WAV helpers ───────────────────────────────────────────────────────────

    private fun writePcmToWav(pcmData: ByteArray): File? {
        return try {
            val wavFile = File(context.cacheDir, "asr_${System.currentTimeMillis()}.wav")
            val byteRate = SAMPLE_RATE * 1 * 16 / 8
            val totalSize = 36 + pcmData.size

            FileOutputStream(wavFile).use { fos ->
                fos.write("RIFF".toByteArray())
                fos.write(totalSize.le4())
                fos.write("WAVE".toByteArray())
                fos.write("fmt ".toByteArray())
                fos.write(16.le4())          // PCM sub-chunk size
                fos.write(1.le2())           // audio format = PCM
                fos.write(1.le2())           // channels = 1
                fos.write(SAMPLE_RATE.le4())
                fos.write(byteRate.le4())
                fos.write(2.le2())           // block align
                fos.write(16.le2())          // bits per sample
                fos.write("data".toByteArray())
                fos.write(pcmData.size.le4())
                fos.write(pcmData)
            }

            Log.d(TAG, "WAV written: ${wavFile.length()} bytes → ${wavFile.absolutePath}")
            wavFile
        } catch (e: Exception) {
            Log.e(TAG, "WAV write error: ${e.message}")
            null
        }
    }

    // ── DSP ───────────────────────────────────────────────────────────────────

    private fun computeRms(buffer: ByteArray, length: Int): Float {
        var sum = 0.0
        val samples = length / 2
        for (i in 0 until samples) {
            val lo = buffer[i * 2].toInt() and 0xFF
            val hi = buffer[i * 2 + 1].toInt()      // signed
            val sample = (hi shl 8) or lo
            sum += sample.toLong() * sample.toLong()
        }
        return sqrt(sum / samples).toFloat()
    }

    private fun generateAmplitudesFromRms(normalizedRms: Float): List<Float> {
        return List(32) { i ->
            val center = 16f
            val dist = abs(i - center) / center
            val base = normalizedRms * (1f - dist * 0.5f)
            base.coerceIn(0.05f, 1f)
        }
    }

    // ── Little-endian byte helpers ────────────────────────────────────────────

    private fun Int.le4() = byteArrayOf(
        (this and 0xFF).toByte(),
        ((this shr 8) and 0xFF).toByte(),
        ((this shr 16) and 0xFF).toByte(),
        ((this shr 24) and 0xFF).toByte()
    )

    private fun Int.le2() = byteArrayOf(
        (this and 0xFF).toByte(),
        ((this shr 8) and 0xFF).toByte()
    )

    // ── Audio playback ────────────────────────────────────────────────────────

    private fun playAudio(filePath: String, onComplete: () -> Unit) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                setOnCompletionListener {
                    Log.d(TAG, "Playback completed")
                    mediaPlayer?.release()
                    mediaPlayer = null
                    onComplete()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                    mediaPlayer?.release()
                    mediaPlayer = null
                    onError?.invoke(t("LIVE_PLAYBACK_FAILED"))
                    true
                }
                start()
                Log.d(TAG, "Playback started: $filePath")
            }
        } catch (e: Exception) {
            Log.e(TAG, "playAudio error: ${e.message}")
            onError?.invoke(t("LIVE_PLAYBACK_FAILED"))
        }
    }
}

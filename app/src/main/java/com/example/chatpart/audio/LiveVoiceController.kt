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
 * LiveVoice 业务控制器 — Push-to-Talk（AudioRecord + AssemblyAI ASR 版）
 *
 * 按住：AudioRecord 开始录 PCM，RMS 驱动波形动画
 * 松开：停录 → 保存 WAV → 上传 AssemblyAI → 轮询结果 → AI 回复 → TTS 播放
 */
class LiveVoiceController(
    private val context: Context,
    private val brain: PersonChat,
    private val audioClient: MiniMaxAudioClient,
    private val scope: CoroutineScope,
    private val currentLanguage: String = "en"
) {
    // Helper function for localized strings
    private fun t(key: String) = Languages.getString(currentLanguage, key)
    companion object {
        private const val TAG = "LiveVoiceController"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT_PCM = AudioFormat.ENCODING_PCM_16BIT
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
    private var isReleased = false

    // Flag set by stopListening() to signal the recording loop to finish
    @Volatile private var isRecording = false

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Called when user presses mic button.
     * Starts AudioRecord, waits for stopListening(), then transcribes + replies.
     */
    fun startListening(profile: Profile, history: List<Message>) {
        if (isReleased) {
            Log.w(TAG, "Controller released, ignoring startListening")
            return
        }

        currentJob?.cancel()
        currentJob = scope.launch {
            // Initial UI update on Main thread
            withContext(Dispatchers.Main) {
                onStateChanged?.invoke(LiveVoiceState.USER_SPEAKING)
                onUserCaptionUpdated?.invoke("")
                onAICaptionUpdated?.invoke("")
                onAmplitudesUpdated?.invoke(List(32) { 0.1f })
            }

            // Record on IO thread — blocks until stopListening() is called
            val wavFile = withContext(Dispatchers.IO) {
                recordAudio()
            }

            withContext(Dispatchers.Main) {
                onAmplitudesUpdated?.invoke(List(32) { 0.3f })
            }

            if (wavFile == null || wavFile.length() < 2000) {
                Log.w(TAG, "Recording too short or failed (size=${wavFile?.length()})")
                withContext(Dispatchers.Main) {
                    onError?.invoke(t("LIVE_RECORD_TOO_SHORT"))
                    onStateChanged?.invoke(LiveVoiceState.IDLE)
                    onAmplitudesUpdated?.invoke(List(32) { 0.1f })
                }
                wavFile?.delete()
                return@launch
            }

            // ASR — show PROCESSING state while waiting
            withContext(Dispatchers.Main) {
                onStateChanged?.invoke(LiveVoiceState.PROCESSING)
            }
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
                return@launch
            }
            wavFile.delete()

            Log.d(TAG, "ASR result: \"$finalText\"")

            if (finalText.isBlank()) {
                withContext(Dispatchers.Main) {
                    onError?.invoke(t("LIVE_SPEECH_NOT_RECOGNIZED"))
                    onStateChanged?.invoke(LiveVoiceState.IDLE)
                    onAmplitudesUpdated?.invoke(List(32) { 0.1f })
                }
                return@launch
            }

            // UI Callback must be on Main thread to prevent crash
            withContext(Dispatchers.Main) {
                onUserCaptionUpdated?.invoke(finalText)
            }

            // AI reply + TTS
            try {
                val chatReply = withContext(Dispatchers.IO) {
                    brain.sendMessage(profile, history, finalText)
                }
                val aiText = chatReply.replyText
                val emotion = chatReply.emotion

                val voicePath = withContext(Dispatchers.IO) {
                    audioClient.textToVoice(
                        text = aiText,
                        voiceId = profile.voiceId ?: "",
                        emotion = "",        // emotion 只有部分中文 voice 支持，英文 voice 传任何值都报错
                        languageBoost = ""
                    )
                }

                withContext(Dispatchers.Main) {
                    onAICaptionUpdated?.invoke(aiText)
                    onStateChanged?.invoke(LiveVoiceState.AI_SPEAKING)
                }

                playAudio(voicePath) {
                    if (!isReleased) {
                        // Use scope to launch UI update correctly
                        scope.launch(Dispatchers.Main) {
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
                // 必须重新抛出 CancellationException，否则协程取消机制会失效
                if (e is CancellationException) throw e
                Log.e(TAG, "Voice chat error", e)
                withContext(Dispatchers.Main) {
                    onError?.invoke(t("LIVE_ERROR").replace("{message}", e.message ?: e.javaClass.simpleName))
                    onStateChanged?.invoke(LiveVoiceState.IDLE)
                    onAmplitudesUpdated?.invoke(List(32) { 0.1f })
                }
            }
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
        currentJob?.cancel()
        currentJob = null
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

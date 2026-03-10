package com.example.chatpart.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Manages audio recording for voice messages.
 * Records in PCM format and saves as WAV file.
 */
class AudioRecordManager(private val context: Context) {

    companion object {
        private const val TAG = "AudioRecordManager"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingFile: File? = null

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Start recording audio.
     * @return true if recording started successfully
     */
    fun startRecording(): Boolean {
        if (!hasPermission()) {
            Log.e(TAG, "❌ No RECORD_AUDIO permission")
            return false
        }

        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(TAG, "❌ Invalid buffer size: $bufferSize")
            return false
        }

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize * 2
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "❌ AudioRecord initialization failed")
                return false
            }

            // Create output file
            val outputDir = File(context.cacheDir, "voice_messages")
            outputDir.mkdirs()
            recordingFile = File(outputDir, "voice_${System.currentTimeMillis()}.wav")

            audioRecord?.startRecording()
            isRecording = true
            Log.d(TAG, "🎙️ Recording started: ${recordingFile?.name}")

            // Start writing audio data in background
            Thread {
                writeAudioData(bufferSize)
            }.start()

            return true
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Security exception", e)
            return false
        }
    }

    private fun writeAudioData(bufferSize: Int) {
        val data = ByteArray(bufferSize)
        val rawFile = File(recordingFile?.parent, "temp_raw.pcm")
        val fos = FileOutputStream(rawFile)

        try {
            while (isRecording) {
                val read = audioRecord?.read(data, 0, data.size) ?: 0
                if (read > 0) {
                    fos.write(data, 0, read)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing audio data", e)
        } finally {
            fos.close()
            // Convert raw PCM to WAV
            convertPcmToWav(rawFile, recordingFile!!)
            rawFile.delete()
        }
    }

    /**
     * Stop recording and return the audio file.
     * @return The recorded WAV file, or null if recording failed
     */
    fun stopRecording(): File? {
        isRecording = false

        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
        }

        Log.d(TAG, "🎙️ Recording stopped: ${recordingFile?.name} (${recordingFile?.length() ?: 0} bytes)")

        // Give time for the write thread to finish
        Thread.sleep(100)

        return recordingFile
    }

    private fun convertPcmToWav(pcmFile: File, wavFile: File) {
        val pcmData = pcmFile.readBytes()
        val totalDataLen = pcmData.size + 36
        val byteRate = SAMPLE_RATE * 1 * 16 / 8  // sampleRate * channels * bitsPerSample / 8

        FileOutputStream(wavFile).use { fos ->
            // WAV header
            fos.write("RIFF".toByteArray())
            fos.write(intToByteArray(totalDataLen))
            fos.write("WAVE".toByteArray())
            fos.write("fmt ".toByteArray())
            fos.write(intToByteArray(16)) // sub chunk size
            fos.write(shortToByteArray(1)) // audio format (PCM)
            fos.write(shortToByteArray(1)) // channels
            fos.write(intToByteArray(SAMPLE_RATE))
            fos.write(intToByteArray(byteRate))
            fos.write(shortToByteArray(2)) // block align
            fos.write(shortToByteArray(16)) // bits per sample
            fos.write("data".toByteArray())
            fos.write(intToByteArray(pcmData.size))
            fos.write(pcmData)
        }
    }

    private fun intToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte()
        )
    }

    private fun shortToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte()
        )
    }
}

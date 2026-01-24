package com.example.tclszero.data.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AudioStreamManager - Handles audio recording and playback for PTT
 * 
 * Features:
 * - Low-latency audio recording for voice transmission
 * - Audio playback for received voice streams
 * - Proper resource management and cleanup
 */
@Singleton
class AudioStreamManager @Inject constructor() {

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var recordingJob: Job? = null
    private var playbackJob: Job? = null
    private var outputStream: PipedOutputStream? = null

    private var isRecording = false
    private var isPlaying = false

    // Audio configuration for voice communication
    private val sampleRate = 16000
    private val channelConfigIn = AudioFormat.CHANNEL_IN_MONO
    private val channelConfigOut = AudioFormat.CHANNEL_OUT_MONO
    private val audioEncoding = AudioFormat.ENCODING_PCM_16BIT
    
    private val bufferSize: Int by lazy {
        AudioRecord.getMinBufferSize(sampleRate, channelConfigIn, audioEncoding)
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ═══════════════════════════════════════════════════════════════════════════
    // RECORDING
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Start recording voice and return as InputStream for mesh transmission
     * 
     * @return InputStream to read recorded audio, or null if failed
     */
    @SuppressLint("MissingPermission")
    fun startRecording(): PipedInputStream? {
        if (isRecording) {
            Timber.w("Recording already in progress")
            return null
        }

        return try {
            val pipedOutput = PipedOutputStream()
            val pipedInput = PipedInputStream(pipedOutput, bufferSize * 4)
            outputStream = pipedOutput

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                sampleRate,
                channelConfigIn,
                audioEncoding,
                bufferSize * 2
            )

            // Check if AudioRecord was initialized successfully
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Timber.e("AudioRecord failed to initialize")
                audioRecord?.release()
                audioRecord = null
                pipedOutput.close()
                return null
            }

            audioRecord?.startRecording()
            isRecording = true

            recordingJob = scope.launch {
                val buffer = ByteArray(bufferSize)
                try {
                    while (isActive && isRecording) {
                        val bytesRead = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                        if (bytesRead > 0) {
                            try {
                                pipedOutput.write(buffer, 0, bytesRead)
                                pipedOutput.flush()
                            } catch (e: Exception) {
                                Timber.w("Pipe closed, stopping recording")
                                break
                            }
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error in recording loop")
                } finally {
                    try {
                        pipedOutput.close()
                    } catch (e: Exception) {
                        // Ignore close errors
                    }
                }
            }

            Timber.d("Recording started")
            pipedInput
        } catch (e: Exception) {
            isRecording = false
            Timber.e(e, "Failed to start recording")
            null
        }
    }

    /**
     * Stop voice recording
     */
    fun stopRecording() {
        if (!isRecording) {
            Timber.d("Recording is not active")
            return
        }

        isRecording = false

        try {
            recordingJob?.cancel()
            recordingJob = null

            try {
                outputStream?.close()
            } catch (e: Exception) {
                // Ignore
            }
            outputStream = null

            audioRecord?.let { record ->
                if (record.state == AudioRecord.STATE_INITIALIZED) {
                    if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        record.stop()
                    }
                    record.release()
                }
            }
            audioRecord = null

            Timber.d("Recording stopped")
        } catch (e: Exception) {
            Timber.e(e, "Error stopping recording")
            audioRecord = null
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PLAYBACK
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Play incoming audio stream
     * 
     * @param inputStream The audio stream to play
     */
    fun playAudioStream(inputStream: InputStream) {
        if (isPlaying) {
            Timber.w("Playback already in progress")
            return
        }

        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfigOut)
                        .setEncoding(audioEncoding)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            if (audioTrack?.state != AudioTrack.STATE_INITIALIZED) {
                Timber.e("AudioTrack failed to initialize")
                audioTrack?.release()
                audioTrack = null
                return
            }

            audioTrack?.play()
            isPlaying = true

            playbackJob = scope.launch {
                val buffer = ByteArray(bufferSize)
                try {
                    var bytesRead = inputStream.read(buffer)
                    while (bytesRead != -1 && isActive && isPlaying) {
                        audioTrack?.write(buffer, 0, bytesRead)
                        bytesRead = inputStream.read(buffer)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error in playback loop")
                } finally {
                    stopPlayback()
                }
            }

            Timber.d("Playback started")
        } catch (e: Exception) {
            isPlaying = false
            Timber.e(e, "Failed to start playback")
        }
    }

    /**
     * Stop audio playback
     */
    fun stopPlayback() {
        if (!isPlaying) {
            return
        }

        isPlaying = false

        try {
            playbackJob?.cancel()
            playbackJob = null

            audioTrack?.let { track ->
                if (track.state == AudioTrack.STATE_INITIALIZED) {
                    if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        track.stop()
                    }
                    track.release()
                }
            }
            audioTrack = null

            Timber.d("Playback stopped")
        } catch (e: Exception) {
            Timber.e(e, "Error stopping playback")
            audioTrack = null
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CLEANUP
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Release all audio resources
     */
    fun release() {
        stopRecording()
        stopPlayback()
        scope.cancel()
        Timber.d("AudioStreamManager released")
    }
}

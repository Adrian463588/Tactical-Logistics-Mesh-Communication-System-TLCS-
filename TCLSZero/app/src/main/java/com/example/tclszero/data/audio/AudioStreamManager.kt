package com.example.tclszero.data.audio



import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.PipedInputStream
import java.io.PipedOutputStream
import javax.inject.Inject

class AudioStreamManager @Inject constructor() {

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var recordingJob: Job? = null
    private var playbackJob: Job? = null

    private var isRecording = false
    private var isPlaying = false


    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    /**
     * Start recording voice and return as InputStream for mesh transmission
     */
    fun startRecording(scope: CoroutineScope): PipedInputStream? {
        if (isRecording) {
            Timber.w("Recording already in progress")
            return null
        }

        isRecording = true
        return try {
            val output = PipedOutputStream()
            val input = PipedInputStream(output)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize * 2
            )

// Check if AudioRecord was initialized successfully
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Timber.e("AudioRecord failed to initialize")
                audioRecord?.release()
                audioRecord = null
                return null
            }

            audioRecord?.startRecording()


            recordingJob = scope.launch {
                val buffer = ByteArray(bufferSize)
                while (isActive) {
                    val bytesRead = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                    if (bytesRead > 0) {
                        output.write(buffer, 0, bytesRead)
                        output.flush()
                    }
                }
            }

            input
        } catch (e: Exception) {
            isRecording = false
            Timber.e(e, "Failed to start recording")
            null
        }
    }

    fun stopRecording() {
        if (!isRecording) {
            Timber.w("Recording is not active")
            return
        }

        try {
            recordingJob?.cancel()
            if (audioRecord != null) {
                audioRecord?.stop()
                audioRecord?.release()
            }
            audioRecord = null
        } catch (e: IllegalStateException) {
            Timber.e(e, "Error stopping recording")
            audioRecord = null
        } finally {
            isRecording = false
        }
    }



    /**
     * Play incoming audio stream with low latency
     */
    fun startPlayback(inputStream: java.io.InputStream, scope: CoroutineScope) {
        try {
            audioTrack = AudioTrack(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .build(),
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(audioFormat)
                    .build(),
                bufferSize * 2,
                AudioTrack.MODE_STREAM,
                android.media.AudioManager.AUDIO_SESSION_ID_GENERATE
            )

            audioTrack?.play()

            playbackJob = scope.launch {
                val buffer = ByteArray(bufferSize)
                var bytesRead = inputStream.read(buffer)
                while (bytesRead != -1 && isActive) {
                    audioTrack?.write(buffer, 0, bytesRead)
                    bytesRead = inputStream.read(buffer)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to start playback")
        }
    }

    fun stopPlayback() {
        try {
            playbackJob?.cancel()
            if (audioTrack != null && audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING) {
                audioTrack?.stop()
            }
            audioTrack?.release()
            audioTrack = null
        } catch (e: IllegalStateException) {
            Timber.e(e, "Error stopping playback")
            audioTrack = null
        }
    }

}

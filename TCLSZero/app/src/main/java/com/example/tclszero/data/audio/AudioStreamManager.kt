package com.example.tclszero.data.audio



import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.PipedInputStream
import java.io.PipedOutputStream

class AudioStreamManager {

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var recordingJob: Job? = null
    private var playbackJob: Job? = null

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    /**
     * Start recording voice and return as InputStream for mesh transmission
     */
    fun startRecording(scope: CoroutineScope): PipedInputStream? {
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
            Timber.e(e, "Failed to start recording")
            null
        }
    }

    fun stopRecording() {
        recordingJob?.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
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
        playbackJob?.cancel()
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }
}

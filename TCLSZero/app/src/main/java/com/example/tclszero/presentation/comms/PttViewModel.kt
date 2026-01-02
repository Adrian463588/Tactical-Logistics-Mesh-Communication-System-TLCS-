package com.example.tclszero.presentation.comms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tclszero.data.audio.AudioStreamManager
import com.example.tclszero.data.mesh.MeshNetworkManager

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PttViewModel @Inject constructor(
    private val meshNetworkManager: MeshNetworkManager,
    private val audioStreamManager: AudioStreamManager
) : ViewModel() {

    private val _pttState = MutableStateFlow<PttState>(PttState.IDLE)
    val pttState: StateFlow<PttState> = _pttState

    private val _voiceQuality = MutableStateFlow<Int>(0) // dB level
    val voiceQuality: StateFlow<Int> = _voiceQuality

    /**
     * User presses PTT button
     */
    fun startTransmit() {
        _pttState.value = PttState.TRANSMITTING
        Timber.d("PTT: Starting transmission")

        viewModelScope.launch {
            val audioStream = audioStreamManager.startRecording(viewModelScope)
            if (audioStream != null) {
                // Send to mesh as audio stream
                meshNetworkManager.sendAudioStream(audioStream)
            }
        }
    }

    /**
     * User releases PTT button
     */
    fun stopTransmit() {
        audioStreamManager.stopRecording()
        _pttState.value = PttState.IDLE
        Timber.d("PTT: Transmission ended")
    }

    /**
     * Receive audio from mesh peer
     */
    fun receiveAudioStream(stream: java.io.InputStream) {
        _pttState.value = PttState.RECEIVING
        viewModelScope.launch {
            audioStreamManager.startPlayback(stream, viewModelScope)
        }
    }

    enum class PttState {
        IDLE, TRANSMITTING, RECEIVING
    }
}

package com.example.tclszero.presentation.comms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tclszero.data.audio.AudioStreamManager
import com.example.tclszero.data.mesh.MeshNetworkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * PttViewModel - Push-to-Talk Voice Communications
 * 
 * Handles:
 * - PTT state management
 * - Audio recording and streaming
 * - Mesh network transmission
 */
@HiltViewModel
class PttViewModel @Inject constructor(
    private val meshManager: MeshNetworkManager,
    private val audioManager: AudioStreamManager
) : ViewModel() {

    // ═══════════════════════════════════════════════════════════════════════════
    // STATE
    // ═══════════════════════════════════════════════════════════════════════════

    private val _pttState = MutableStateFlow(PttState.IDLE)
    val pttState: StateFlow<PttState> = _pttState.asStateFlow()

    val meshState = meshManager.meshState
    val connectedEndpoints = meshManager.connectedEndpoints

    // ═══════════════════════════════════════════════════════════════════════════
    // PTT ACTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Start voice transmission (PTT button pressed)
     */
    fun startTransmit() {
        if (_pttState.value == PttState.TRANSMITTING) {
            Timber.w("Already transmitting")
            return
        }

        Timber.d("Starting PTT transmission")
        _pttState.value = PttState.TRANSMITTING

        viewModelScope.launch {
            try {
                // Start audio recording and get the stream
                val audioStream = audioManager.startRecording()
                
                if (audioStream != null) {
                    // Send audio stream to mesh network
                    meshManager.sendAudioStream(audioStream)
                } else {
                    Timber.e("Failed to start audio recording")
                    _pttState.value = PttState.ERROR
                }
            } catch (e: Exception) {
                Timber.e(e, "Error during transmission")
                _pttState.value = PttState.ERROR
            }
        }
    }

    /**
     * Stop voice transmission (PTT button released)
     */
    fun stopTransmit() {
        if (_pttState.value != PttState.TRANSMITTING) {
            return
        }

        Timber.d("Stopping PTT transmission")
        
        viewModelScope.launch {
            try {
                audioManager.stopRecording()
            } catch (e: Exception) {
                Timber.e(e, "Error stopping recording")
            }
        }

        _pttState.value = PttState.IDLE
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MESH NETWORK
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Start mesh network discovery and advertising
     */
    fun startMesh(localName: String = "TLCS_${System.currentTimeMillis() % 10000}") {
        if (!meshManager.hasRequiredPermissions()) {
            Timber.e("Missing mesh permissions")
            return
        }

        meshManager.startMeshNetwork(localName)
    }

    /**
     * Stop mesh network
     */
    fun stopMesh() {
        meshManager.stopMesh()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // AUDIO RECEIVING
    // ═══════════════════════════════════════════════════════════════════════════

    init {
        // Set up audio stream receiver
        meshManager.setOnAudioStreamReceivedListener { endpointId, stream ->
            if (stream != null && _pttState.value != PttState.TRANSMITTING) {
                _pttState.value = PttState.RECEIVING
                
                viewModelScope.launch {
                    try {
                        audioManager.playAudioStream(stream)
                    } catch (e: Exception) {
                        Timber.e(e, "Error playing received audio")
                    } finally {
                        if (_pttState.value == PttState.RECEIVING) {
                            _pttState.value = PttState.IDLE
                        }
                    }
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CLEANUP
    // ═══════════════════════════════════════════════════════════════════════════

    override fun onCleared() {
        super.onCleared()
        audioManager.release()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STATE ENUM
    // ═══════════════════════════════════════════════════════════════════════════

    enum class PttState {
        IDLE,           // Not transmitting or receiving
        TRANSMITTING,   // User is pressing PTT and transmitting
        RECEIVING,      // Receiving audio from another node
        ERROR           // Error state
    }
}

package com.example.tclszero.data.mesh

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import java.io.InputStream


class MeshNetworkManager(private val context: Context) {

    private val connectionsClient = Nearby.getConnectionsClient(context)

    private val _connectedEndpoints = MutableStateFlow<List<String>>(emptyList())
    val connectedEndpoints: StateFlow<List<String>> = _connectedEndpoints

    private val _meshState = MutableStateFlow<MeshState>(MeshState.IDLE)
    val meshState: StateFlow<MeshState> = _meshState

    private val connectedEndpointMap = mutableMapOf<String, String>()

    /**
     * Phase 1: Start both advertising and discovery
     * This creates the P2P_CLUSTER mesh network
     */
    fun startMeshNetwork(localName: String) {
        _meshState.value = MeshState.SEARCHING

        val strategy = Strategy.P2P_CLUSTER


        // Step 1: Start Advertising
        val advertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(strategy)
            .build()

        connectionsClient.startAdvertising(
            localName,
            SERVICE_ID,
            connectionLifecycleCallback,
            advertisingOptions
        )
            .addOnSuccessListener {
                Timber.d("Advertising started: $localName")
            }
            .addOnFailureListener {
                Timber.e(it, "Failed to start advertising")
            }

        // Step 2: Start Discovery
        val discoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(strategy)
            .build()

        connectionsClient.startDiscovery(
            SERVICE_ID,
            endpointDiscoveryCallback,
            discoveryOptions
        )
            .addOnSuccessListener {
                Timber.d("Discovery started")
            }
            .addOnFailureListener {
                Timber.e(it, "Failed to start discovery")
            }
    }

    /**
     * Handshake callback: When a discoverer finds an advertiser
     */
    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, discoveredEndpointInfo: DiscoveredEndpointInfo) {
            Timber.d("Endpoint found: $endpointId (${discoveredEndpointInfo.endpointName})")

            // Auto-connect in tactical scenario (trusted mesh)
            connectionsClient.requestConnection(
                "self_endpoint_name",
                endpointId,
                connectionLifecycleCallback
            )
                .addOnSuccessListener {
                    Timber.d("Connection request sent to $endpointId")
                }
        }

        override fun onEndpointLost(endpointId: String) {
            Timber.d("Endpoint lost: $endpointId")
            connectedEndpointMap.remove(endpointId)
            updateConnectedList()
        }
    }

    /**
     * Connection Lifecycle: Handles handshake and acceptance
     */
    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            Timber.d("Connection initiated: $endpointId from ${connectionInfo.endpointName}")

            // Auto-accept (trusted tactical environment)
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when {
                result.status.isSuccess -> {
                    Timber.d("Connected to: $endpointId")
                    connectedEndpointMap[endpointId] = endpointId
                    updateConnectedList()
                    _meshState.value = MeshState.CONNECTED
                }
                else -> {
                    Timber.e("Connection failed: $endpointId")
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Timber.d("Disconnected: $endpointId")
            connectedEndpointMap.remove(endpointId)
            updateConnectedList()
        }
    }

    /**
     * Payload handling: Receive data and audio streams
     */
    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            Timber.d("Payload received from $endpointId, type: ${payload.type}")

            when (payload.type) {
                Payload.Type.BYTES -> {
                    val data = payload.asBytes()
                    if (data != null) {
                        onDataPayloadReceived(data)
                    }

                }
                Payload.Type.STREAM -> {
                    val stream = payload.asStream()?.asInputStream()
                    onAudioStreamReceived(stream)
                }

            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            Timber.d("Payload transfer update: ${update.status}")
        }
    }

    /**
     * Send coordinate/inventory data as JSON bytes
     */
    fun sendDataPayload(data: ByteArray) {
        val payload = Payload.fromBytes(data)
        connectedEndpointMap.forEach { (endpointId, _) ->
            connectionsClient.sendPayload(endpointId, payload)
        }
    }

    /**
     * Stream audio (PTT)
     */
    fun sendAudioStream(audioStream: java.io.InputStream) {
        val payload = Payload.fromStream(audioStream)
        connectedEndpointMap.forEach { (endpointId, _) ->
            connectionsClient.sendPayload(endpointId, payload)
        }
    }

    private fun updateConnectedList() {
        _connectedEndpoints.value = connectedEndpointMap.keys.toList()
    }

    private fun onDataPayloadReceived(data: ByteArray) {
        // Parse JSON and emit to repository
        Timber.d("Data payload: ${String(data)}")
    }

    private fun onAudioStreamReceived(stream: java.io.InputStream?) {
        // Audio is fed to AudioTrack for playback
        Timber.d("Audio stream received")
    }

    fun stopMesh() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        _meshState.value = MeshState.IDLE
    }

    enum class MeshState {
        IDLE, SEARCHING, HANDSHAKE, CONNECTED
    }

    companion object {
        private const val SERVICE_ID = "com.tlcs.mesh"
    }
}

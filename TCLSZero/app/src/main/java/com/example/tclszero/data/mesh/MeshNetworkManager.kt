package com.example.tclszero.data.mesh

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.example.tclszero.util.PermissionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MeshNetworkManager - P2P Mesh Networking via Google Nearby Connections API
 * 
 * Handles:
 * - Device discovery and connection (P2P_CLUSTER strategy)
 * - Data payload transmission (JSON for logistics data)
 * - Audio stream transmission (PTT voice comms)
 * - Connection lifecycle management
 * 
 * IMPORTANT: Requires proper permissions before calling startMeshNetwork():
 * - BLUETOOTH_SCAN, BLUETOOTH_ADVERTISE, BLUETOOTH_CONNECT (API 31+)
 * - NEARBY_WIFI_DEVICES (API 33+)
 * - ACCESS_FINE_LOCATION
 */
@Singleton
class MeshNetworkManager @Inject constructor(
    private val context: Context
) {
    private val connectionsClient: ConnectionsClient by lazy {
        Nearby.getConnectionsClient(context)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STATE MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════

    private val _meshState = MutableStateFlow(MeshState.IDLE)
    val meshState: StateFlow<MeshState> = _meshState.asStateFlow()

    private val _connectedEndpoints = MutableStateFlow<List<ConnectedEndpoint>>(emptyList())
    val connectedEndpoints: StateFlow<List<ConnectedEndpoint>> = _connectedEndpoints.asStateFlow()

    private val _lastError = MutableStateFlow<MeshError?>(null)
    val lastError: StateFlow<MeshError?> = _lastError.asStateFlow()

    private val _discoveredEndpoints = MutableStateFlow<List<DiscoveredEndpoint>>(emptyList())
    val discoveredEndpoints: StateFlow<List<DiscoveredEndpoint>> = _discoveredEndpoints.asStateFlow()

    // Internal state tracking
    private val connectedEndpointMap = mutableMapOf<String, ConnectedEndpoint>()
    private val discoveredEndpointMap = mutableMapOf<String, DiscoveredEndpoint>()
    private var localEndpointName: String = "TLCS_NODE"
    private var isAdvertising = false
    private var isDiscovering = false

    // Callbacks for received data
    private var onDataReceived: ((String, ByteArray) -> Unit)? = null
    private var onAudioStreamReceived: ((String, InputStream?) -> Unit)? = null

    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Check if all required permissions are granted before starting mesh
     */
    fun hasRequiredPermissions(): Boolean {
        return PermissionManager.hasMeshPermissions(context)
    }

    /**
     * Start the mesh network (advertising + discovery)
     * 
     * @param localName The display name for this node on the mesh
     * @return true if started successfully (permissions granted), false otherwise
     */
    fun startMeshNetwork(localName: String): Boolean {
        if (!hasRequiredPermissions()) {
            Timber.e("Cannot start mesh: Missing permissions")
            _lastError.value = MeshError.PERMISSION_DENIED
            _meshState.value = MeshState.ERROR
            return false
        }

        localEndpointName = localName
        _meshState.value = MeshState.SEARCHING
        _lastError.value = null

        startAdvertising()
        startDiscovery()

        return true
    }

    /**
     * Stop all mesh networking operations
     */
    fun stopMesh() {
        Timber.d("Stopping mesh network")
        
        stopAdvertising()
        stopDiscovery()
        disconnectAllEndpoints()

        connectedEndpointMap.clear()
        discoveredEndpointMap.clear()
        updateConnectedList()
        updateDiscoveredList()

        _meshState.value = MeshState.IDLE
    }

    /**
     * Manually connect to a discovered endpoint
     */
    fun connectToEndpoint(endpointId: String) {
        if (_meshState.value == MeshState.ERROR) {
            Timber.e("Cannot connect: Mesh is in error state")
            return
        }

        Timber.d("Requesting connection to $endpointId")
        _meshState.value = MeshState.HANDSHAKE

        connectionsClient.requestConnection(
            localEndpointName,
            endpointId,
            connectionLifecycleCallback
        ).addOnSuccessListener {
            Timber.d("Connection request sent to $endpointId")
        }.addOnFailureListener { e ->
            Timber.e(e, "Failed to request connection to $endpointId")
            handleConnectionError(e)
        }
    }

    /**
     * Disconnect from a specific endpoint
     */
    fun disconnectFromEndpoint(endpointId: String) {
        connectionsClient.disconnectFromEndpoint(endpointId)
        connectedEndpointMap.remove(endpointId)
        updateConnectedList()
        
        if (connectedEndpointMap.isEmpty()) {
            _meshState.value = MeshState.SEARCHING
        }
    }

    /**
     * Send data payload to all connected endpoints
     */
    fun sendDataPayload(data: ByteArray) {
        if (connectedEndpointMap.isEmpty()) {
            Timber.w("No connected endpoints to send data to")
            return
        }

        val payload = Payload.fromBytes(data)
        connectedEndpointMap.keys.forEach { endpointId ->
            connectionsClient.sendPayload(endpointId, payload)
                .addOnSuccessListener {
                    Timber.d("Data sent to $endpointId")
                }
                .addOnFailureListener { e ->
                    Timber.e(e, "Failed to send data to $endpointId")
                }
        }
    }

    /**
     * Send data payload to a specific endpoint
     */
    fun sendDataPayloadTo(endpointId: String, data: ByteArray) {
        if (!connectedEndpointMap.containsKey(endpointId)) {
            Timber.w("Endpoint $endpointId is not connected")
            return
        }

        val payload = Payload.fromBytes(data)
        connectionsClient.sendPayload(endpointId, payload)
            .addOnSuccessListener {
                Timber.d("Data sent to $endpointId")
            }
            .addOnFailureListener { e ->
                Timber.e(e, "Failed to send data to $endpointId")
            }
    }

    /**
     * Stream audio to all connected endpoints (for PTT)
     */
    fun sendAudioStream(audioStream: InputStream) {
        if (connectedEndpointMap.isEmpty()) {
            Timber.w("No connected endpoints to stream audio to")
            return
        }

        val payload = Payload.fromStream(audioStream)
        connectedEndpointMap.keys.forEach { endpointId ->
            connectionsClient.sendPayload(endpointId, payload)
                .addOnSuccessListener {
                    Timber.d("Audio stream started to $endpointId")
                }
                .addOnFailureListener { e ->
                    Timber.e(e, "Failed to stream audio to $endpointId")
                }
        }
    }

    /**
     * Set callback for received data payloads
     */
    fun setOnDataReceivedListener(listener: (endpointId: String, data: ByteArray) -> Unit) {
        onDataReceived = listener
    }

    /**
     * Set callback for received audio streams
     */
    fun setOnAudioStreamReceivedListener(listener: (endpointId: String, stream: InputStream?) -> Unit) {
        onAudioStreamReceived = listener
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRIVATE - ADVERTISING
    // ═══════════════════════════════════════════════════════════════════════════

    private fun startAdvertising() {
        if (isAdvertising) {
            Timber.d("Already advertising")
            return
        }

        val advertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(MESH_STRATEGY)
            .build()

        connectionsClient.startAdvertising(
            localEndpointName,
            SERVICE_ID,
            connectionLifecycleCallback,
            advertisingOptions
        ).addOnSuccessListener {
            isAdvertising = true
            Timber.d("Advertising started as '$localEndpointName'")
        }.addOnFailureListener { e ->
            isAdvertising = false
            Timber.e(e, "Failed to start advertising")
            handleConnectionError(e)
        }
    }

    private fun stopAdvertising() {
        if (isAdvertising) {
            connectionsClient.stopAdvertising()
            isAdvertising = false
            Timber.d("Advertising stopped")
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRIVATE - DISCOVERY
    // ═══════════════════════════════════════════════════════════════════════════

    private fun startDiscovery() {
        if (isDiscovering) {
            Timber.d("Already discovering")
            return
        }

        val discoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(MESH_STRATEGY)
            .build()

        connectionsClient.startDiscovery(
            SERVICE_ID,
            endpointDiscoveryCallback,
            discoveryOptions
        ).addOnSuccessListener {
            isDiscovering = true
            Timber.d("Discovery started")
        }.addOnFailureListener { e ->
            isDiscovering = false
            Timber.e(e, "Failed to start discovery")
            handleConnectionError(e)
        }
    }

    private fun stopDiscovery() {
        if (isDiscovering) {
            connectionsClient.stopDiscovery()
            isDiscovering = false
            Timber.d("Discovery stopped")
        }
    }

    private fun disconnectAllEndpoints() {
        connectionsClient.stopAllEndpoints()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CALLBACKS - ENDPOINT DISCOVERY
    // ═══════════════════════════════════════════════════════════════════════════

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Timber.d("Endpoint found: $endpointId (${info.endpointName})")
            
            val discovered = DiscoveredEndpoint(
                id = endpointId,
                name = info.endpointName,
                serviceId = info.serviceId,
                discoveredAt = System.currentTimeMillis()
            )
            discoveredEndpointMap[endpointId] = discovered
            updateDiscoveredList()

            // Auto-connect for tactical mesh (trusted environment)
            // In production, you might want to add a confirmation step
            connectToEndpoint(endpointId)
        }

        override fun onEndpointLost(endpointId: String) {
            Timber.d("Endpoint lost: $endpointId")
            discoveredEndpointMap.remove(endpointId)
            updateDiscoveredList()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CALLBACKS - CONNECTION LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════════════

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            Timber.d("Connection initiated: $endpointId from ${info.endpointName}")
            _meshState.value = MeshState.HANDSHAKE

            // Auto-accept (trusted tactical environment)
            // For added security, verify info.authenticationDigits with user
            connectionsClient.acceptConnection(endpointId, payloadCallback)
                .addOnSuccessListener {
                    Timber.d("Connection accepted for $endpointId")
                }
                .addOnFailureListener { e ->
                    Timber.e(e, "Failed to accept connection for $endpointId")
                }
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Timber.d("Connected to: $endpointId")
                    
                    val endpoint = ConnectedEndpoint(
                        id = endpointId,
                        name = discoveredEndpointMap[endpointId]?.name ?: "Unknown",
                        connectedAt = System.currentTimeMillis()
                    )
                    connectedEndpointMap[endpointId] = endpoint
                    updateConnectedList()
                    
                    _meshState.value = MeshState.CONNECTED
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    Timber.w("Connection rejected by: $endpointId")
                    _lastError.value = MeshError.CONNECTION_REJECTED
                }
                ConnectionsStatusCodes.STATUS_ERROR -> {
                    Timber.e("Connection error with: $endpointId")
                    _lastError.value = MeshError.CONNECTION_FAILED
                }
                else -> {
                    Timber.e("Unknown connection result: ${result.status.statusCode}")
                    _lastError.value = MeshError.UNKNOWN
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Timber.d("Disconnected: $endpointId")
            connectedEndpointMap.remove(endpointId)
            updateConnectedList()

            if (connectedEndpointMap.isEmpty()) {
                _meshState.value = if (isDiscovering || isAdvertising) {
                    MeshState.SEARCHING
                } else {
                    MeshState.IDLE
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CALLBACKS - PAYLOAD
    // ═══════════════════════════════════════════════════════════════════════════

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            Timber.d("Payload received from $endpointId, type: ${payload.type}")

            when (payload.type) {
                Payload.Type.BYTES -> {
                    payload.asBytes()?.let { data ->
                        Timber.d("Data payload: ${String(data)}")
                        onDataReceived?.invoke(endpointId, data)
                    }
                }
                Payload.Type.STREAM -> {
                    val stream = payload.asStream()?.asInputStream()
                    Timber.d("Audio stream received from $endpointId")
                    onAudioStreamReceived?.invoke(endpointId, stream)
                }
                Payload.Type.FILE -> {
                    Timber.d("File payload received (not implemented)")
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            when (update.status) {
                PayloadTransferUpdate.Status.SUCCESS -> {
                    Timber.d("Payload transfer complete: ${update.payloadId}")
                }
                PayloadTransferUpdate.Status.FAILURE -> {
                    Timber.e("Payload transfer failed: ${update.payloadId}")
                }
                PayloadTransferUpdate.Status.IN_PROGRESS -> {
                    val progress = (update.bytesTransferred * 100 / update.totalBytes).toInt()
                    Timber.v("Payload transfer progress: $progress%")
                }
                PayloadTransferUpdate.Status.CANCELED -> {
                    Timber.w("Payload transfer canceled: ${update.payloadId}")
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    private fun updateConnectedList() {
        _connectedEndpoints.value = connectedEndpointMap.values.toList()
    }

    private fun updateDiscoveredList() {
        _discoveredEndpoints.value = discoveredEndpointMap.values.toList()
    }

    private fun handleConnectionError(exception: Exception) {
        val errorMessage = exception.message ?: "Unknown error"
        
        when {
            errorMessage.contains("MISSING_PERMISSION", ignoreCase = true) -> {
                _lastError.value = MeshError.PERMISSION_DENIED
            }
            errorMessage.contains("BLUETOOTH", ignoreCase = true) -> {
                _lastError.value = MeshError.BLUETOOTH_DISABLED
            }
            errorMessage.contains("WIFI", ignoreCase = true) -> {
                _lastError.value = MeshError.WIFI_DISABLED
            }
            errorMessage.contains("ALREADY_", ignoreCase = true) -> {
                // Already advertising/discovering - not a real error
                return
            }
            else -> {
                _lastError.value = MeshError.UNKNOWN
            }
        }

        _meshState.value = MeshState.ERROR
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DATA CLASSES & ENUMS
    // ═══════════════════════════════════════════════════════════════════════════

    enum class MeshState {
        IDLE,           // Not started
        SEARCHING,      // Advertising + Discovering
        HANDSHAKE,      // Connection in progress
        CONNECTED,      // At least one peer connected
        ERROR           // Error state - check lastError
    }

    enum class MeshError {
        PERMISSION_DENIED,
        BLUETOOTH_DISABLED,
        WIFI_DISABLED,
        CONNECTION_FAILED,
        CONNECTION_REJECTED,
        UNKNOWN
    }

    data class ConnectedEndpoint(
        val id: String,
        val name: String,
        val connectedAt: Long
    )

    data class DiscoveredEndpoint(
        val id: String,
        val name: String,
        val serviceId: String,
        val discoveredAt: Long
    )

    companion object {
        private const val SERVICE_ID = "com.tlcs.mesh"
        
        // P2P_CLUSTER allows many-to-many connections (mesh topology)
        private val MESH_STRATEGY = Strategy.P2P_CLUSTER
    }
}

package com.example.tclszero.presentation.map

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tclszero.data.local.CommandPostDao
import com.example.tclszero.data.local.SoldierNodeDao
import com.example.tclszero.data.map.OfflineTileProvider
import com.example.tclszero.domain.model.CommandPost
import com.example.tclszero.domain.model.SoldierNode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.osmdroid.config.IConfigurationProvider
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * MapViewModel - Handles map state following Clean Architecture (Presentation Layer)
 * 
 * Responsibilities:
 * - Map configuration initialization state
 * - Offline tile source management
 * - Blue Force Tracking (soldier nodes)
 * - Command post locations
 * - Map UI state
 * 
 * Dependencies injected:
 * - IConfigurationProvider: Ensures osmdroid is configured before MapView creation
 * - OfflineTileProvider: Manages offline tile loading
 * - DAOs: For tactical data
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    private val osmdroidConfig: IConfigurationProvider,  // Injected to ensure config is loaded
    private val tileProvider: OfflineTileProvider,
    private val soldierNodeDao: SoldierNodeDao,
    private val commandPostDao: CommandPostDao
) : ViewModel() {

    // ═══════════════════════════════════════════════════════════════════════════
    // CONFIGURATION STATE
    // ═══════════════════════════════════════════════════════════════════════════

    private val _isConfigReady = MutableStateFlow(false)
    val isConfigReady: StateFlow<Boolean> = _isConfigReady.asStateFlow()

    init {
        // Verify osmdroid configuration is loaded
        verifyConfiguration()
        // Load available tiles
        loadAvailableTiles()
    }

    private fun verifyConfiguration() {
        viewModelScope.launch {
            try {
                // The configuration is already loaded via Hilt injection
                // This just verifies it's working
                val userAgent = osmdroidConfig.userAgentValue
                Timber.d("OSMDroid configured with user agent: $userAgent")
                _isConfigReady.value = true
            } catch (e: Exception) {
                Timber.e(e, "OSMDroid configuration failed")
                _mapUiState.value = _mapUiState.value.copy(
                    error = "Map configuration failed: ${e.message}"
                )
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TACTICAL DATA FLOWS
    // ═══════════════════════════════════════════════════════════════════════════

    val allNodes: StateFlow<List<SoldierNode>> = soldierNodeDao.getAllNodes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allPosts: StateFlow<List<CommandPost>> = commandPostDao.getAllPosts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // ═══════════════════════════════════════════════════════════════════════════
    // MAP UI STATE
    // ═══════════════════════════════════════════════════════════════════════════

    private val _mapUiState = MutableStateFlow(MapUiState())
    val mapUiState: StateFlow<MapUiState> = _mapUiState.asStateFlow()

    val tileSourceState = tileProvider.tileSourceState
    val importProgress = tileProvider.importProgress

    // ═══════════════════════════════════════════════════════════════════════════
    // TILE MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Import tiles from a content URI (from SAF file picker)
     */
    fun importTiles(uri: Uri) {
        viewModelScope.launch {
            _mapUiState.value = _mapUiState.value.copy(isImporting = true, error = null)
            
            val result = tileProvider.importTilesFromUri(uri)
            result.fold(
                onSuccess = { importResult ->
                    Timber.d("Imported ${importResult.tileCount} tiles from ${importResult.file.name}")
                    _mapUiState.value = _mapUiState.value.copy(
                        isImporting = false,
                        currentTileSource = importResult.tileSource,
                        message = "Imported ${importResult.tileCount} tiles successfully"
                    )
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to import tiles")
                    _mapUiState.value = _mapUiState.value.copy(
                        isImporting = false,
                        error = "Import failed: ${error.message}"
                    )
                }
            )
        }
    }

    /**
     * Load available tiles from app storage on startup
     */
    private fun loadAvailableTiles() {
        viewModelScope.launch {
            try {
                val files = tileProvider.listAvailableTileFiles()
                Timber.d("Found ${files.size} tile files in storage")
                
                if (files.isNotEmpty()) {
                    // Load the first available tile file
                    val file = files.first()
                    val result = if (file.extension.equals("mbtiles", ignoreCase = true)) {
                        tileProvider.loadMbTilesFile(file)
                    } else if (file.isDirectory) {
                        tileProvider.loadTileDirectory(file)
                    } else {
                        null
                    }
                    
                    result?.onSuccess { tileSource ->
                        _mapUiState.value = _mapUiState.value.copy(
                            currentTileSource = tileSource,
                            message = "Loaded tiles: ${file.name}"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "No offline tiles available")
            }
        }
    }

    /**
     * Get the current tile source for the map
     * Falls back to default if no offline tiles available
     */
    fun getCurrentTileSource(): ITileSource {
        return _mapUiState.value.currentTileSource ?: TileSourceFactory.DEFAULT_TILE_SOURCE
    }

    /**
     * Check if offline tiles are loaded
     */
    fun hasOfflineTiles(): Boolean {
        return _mapUiState.value.currentTileSource != null
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NODE OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    fun updateNodePosition(nodeId: String, lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                soldierNodeDao.updatePosition(nodeId, lat, lon)
            } catch (e: Exception) {
                Timber.e(e, "Failed to update node position")
            }
        }
    }

    fun addOrUpdateNode(node: SoldierNode) {
        viewModelScope.launch {
            try {
                soldierNodeDao.insertOrUpdate(node)
            } catch (e: Exception) {
                Timber.e(e, "Failed to add/update node")
            }
        }
    }

    fun deleteNode(nodeId: String) {
        viewModelScope.launch {
            try {
                soldierNodeDao.deleteById(nodeId)
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete node")
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // COMMAND POST OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    fun addOrUpdatePost(post: CommandPost) {
        viewModelScope.launch {
            try {
                commandPostDao.insertOrUpdate(post)
            } catch (e: Exception) {
                Timber.e(e, "Failed to add/update post")
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            try {
                commandPostDao.deleteById(postId)
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete post")
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UI STATE MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════

    fun clearMessage() {
        _mapUiState.value = _mapUiState.value.copy(message = null)
    }

    fun clearError() {
        _mapUiState.value = _mapUiState.value.copy(error = null)
    }

    fun updateMapCenter(lat: Double, lon: Double) {
        _mapUiState.value = _mapUiState.value.copy(
            centerLat = lat,
            centerLon = lon
        )
    }

    fun updateZoom(zoom: Double) {
        _mapUiState.value = _mapUiState.value.copy(zoom = zoom)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CLEANUP
    // ═══════════════════════════════════════════════════════════════════════════

    override fun onCleared() {
        super.onCleared()
        tileProvider.closeCurrent()
    }
}

/**
 * UI State data class for the Map screen
 */
data class MapUiState(
    val isImporting: Boolean = false,
    val currentTileSource: ITileSource? = null,
    val centerLat: Double = DEFAULT_LAT,
    val centerLon: Double = DEFAULT_LON,
    val zoom: Double = DEFAULT_ZOOM,
    val message: String? = null,
    val error: String? = null
) {
    companion object {
        // Default center: Indonesia
        const val DEFAULT_LAT = -6.2088
        const val DEFAULT_LON = 106.8456
        const val DEFAULT_ZOOM = 12.0
        const val MIN_ZOOM = 3.0
        const val MAX_ZOOM = 19.0
    }
}

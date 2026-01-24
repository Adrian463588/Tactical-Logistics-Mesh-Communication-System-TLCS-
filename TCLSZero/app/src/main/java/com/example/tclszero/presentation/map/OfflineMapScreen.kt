package com.example.tclszero.presentation.map

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.tclszero.data.map.OfflineTileProvider
import com.example.tclszero.presentation.theme.LogisticsColors
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import timber.log.Timber

/**
 * OfflineMapScreen - Jetpack Compose wrapper for OSMDroid MapView
 * 
 * Features:
 * - Proper lifecycle management (resume/pause MapView)
 * - Offline tile rendering with SAF file picker import
 * - Blue Force Tracking markers
 * - Command post markers
 * - GPS location overlay
 * 
 * CRITICAL: The MapView is only created AFTER osmdroid configuration is verified.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineMapScreen(
    viewModel: MapViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Collect states
    val isConfigReady by viewModel.isConfigReady.collectAsState()
    val soldierNodes by viewModel.allNodes.collectAsState()
    val commandPosts by viewModel.allPosts.collectAsState()
    val tileState by viewModel.tileSourceState.collectAsState()
    val mapUiState by viewModel.mapUiState.collectAsState()
    val importProgress by viewModel.importProgress.collectAsState()

    // MapView reference for lifecycle management
    var mapView by remember { mutableStateOf<MapView?>(null) }

    // SAF File picker for tile import
    val tileFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { 
            Timber.d("Selected tile file: $it")
            viewModel.importTiles(it) 
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // LIFECYCLE OBSERVER - Critical for MapView
    // ═══════════════════════════════════════════════════════════════════════════
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    mapView?.onResume()
                    Timber.d("MapView resumed")
                }
                Lifecycle.Event.ON_PAUSE -> {
                    mapView?.onPause()
                    Timber.d("MapView paused")
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView?.onDetach()
            Timber.d("MapView detached")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ═══════════════════════════════════════════════════════════════════════
        // LOADING STATE - Wait for osmdroid configuration
        // ═══════════════════════════════════════════════════════════════════════
        if (!isConfigReady) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = LogisticsColors.Primary
                    )
                    Text(
                        text = "Initializing Map...",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            // ═══════════════════════════════════════════════════════════════════
            // MAP VIEW - Only created after config is ready
            // ═══════════════════════════════════════════════════════════════════
            AndroidView(
                factory = { ctx ->
                    createMapView(ctx, viewModel).also { view ->
                        mapView = view
                        Timber.d("MapView created successfully")
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    // Update tile source when it changes
                    val newTileSource = viewModel.getCurrentTileSource()
                    if (view.tileProvider.tileSource != newTileSource) {
                        view.setTileSource(newTileSource)
                        Timber.d("Tile source updated: ${newTileSource.name()}")
                    }

                    // Update markers
                    updateMapMarkers(view, soldierNodes, commandPosts)
                }
            )
        }

        // ═══════════════════════════════════════════════════════════════════════
        // TOP STATUS BAR
        // ═══════════════════════════════════════════════════════════════════════
        if (isConfigReady) {
            TileStatusCard(
                tileState = tileState,
                hasOfflineTiles = viewModel.hasOfflineTiles(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            )
        }

        // ═══════════════════════════════════════════════════════════════════════
        // IMPORT PROGRESS OVERLAY
        // ═══════════════════════════════════════════════════════════════════════
        if (mapUiState.isImporting) {
            ImportProgressDialog(progress = importProgress)
        }

        // ═══════════════════════════════════════════════════════════════════════
        // FAB CONTROLS
        // ═══════════════════════════════════════════════════════════════════════
        if (isConfigReady) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // My Location FAB
                FloatingActionButton(
                    onClick = {
                        mapView?.overlays
                            ?.filterIsInstance<MyLocationNewOverlay>()
                            ?.firstOrNull()
                            ?.myLocation
                            ?.let { location ->
                                mapView?.controller?.animateTo(location)
                            }
                    },
                    containerColor = LogisticsColors.Surface,
                    contentColor = LogisticsColors.Primary
                ) {
                    Icon(Icons.Filled.MyLocation, "My Location")
                }

                // Zoom In
                FloatingActionButton(
                    onClick = { mapView?.controller?.zoomIn() },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Filled.Add, "Zoom In")
                }

                // Zoom Out
                FloatingActionButton(
                    onClick = { mapView?.controller?.zoomOut() },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Filled.Remove, "Zoom Out")
                }

                // Import Tiles FAB
                FloatingActionButton(
                    onClick = {
                        tileFilePicker.launch(arrayOf(
                            "application/octet-stream",
                            "application/zip",
                            "application/x-sqlite3",
                            "*/*"
                        ))
                    },
                    containerColor = LogisticsColors.Primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Filled.FileOpen, "Import Tiles")
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════════════
        // SNACKBAR MESSAGES
        // ═══════════════════════════════════════════════════════════════════════
        mapUiState.message?.let { message ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearMessage() }) {
                        Text("OK")
                    }
                }
            ) {
                Text(message)
            }
        }

        mapUiState.error?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(error)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// HELPER COMPOSABLES
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun TileStatusCard(
    tileState: OfflineTileProvider.TileSourceState,
    hasOfflineTiles: Boolean,
    modifier: Modifier = Modifier
) {
    val (icon, text, containerColor) = when {
        tileState is OfflineTileProvider.TileSourceState.Ready -> Triple(
            Icons.Filled.CheckCircle,
            "Offline Tiles Loaded",
            LogisticsColors.Surface
        )
        tileState is OfflineTileProvider.TileSourceState.Importing -> Triple(
            Icons.Filled.Sync,
            "Importing tiles...",
            LogisticsColors.SurfaceVariant
        )
        tileState is OfflineTileProvider.TileSourceState.Error -> Triple(
            Icons.Filled.Error,
            (tileState as OfflineTileProvider.TileSourceState.Error).message,
            MaterialTheme.colorScheme.errorContainer
        )
        else -> Triple(
            Icons.Filled.Warning,
            "No offline tiles - Tap + to import",
            MaterialTheme.colorScheme.surfaceVariant
        )
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (hasOfflineTiles) LogisticsColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ImportProgressDialog(progress: Float) {
    AlertDialog(
        onDismissRequest = { /* Cannot dismiss while importing */ },
        confirmButton = { },
        icon = {
            CircularProgressIndicator(
                progress = { progress },
                color = LogisticsColors.Primary
            )
        },
        title = {
            Text("Importing Tiles")
        },
        text = {
            Text("${(progress * 100).toInt()}% complete")
        }
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// MAP VIEW FACTORY
// ═══════════════════════════════════════════════════════════════════════════════

private fun createMapView(context: Context, viewModel: MapViewModel): MapView {
    return MapView(context).apply {
        // Basic configuration
        setMultiTouchControls(true)
        isTilesScaledToDpi = true
        
        // Disable built-in zoom buttons (we use FABs)
        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        
        // Set tile source
        setTileSource(viewModel.getCurrentTileSource())
        
        // Initial position
        controller.setZoom(MapUiState.DEFAULT_ZOOM)
        controller.setCenter(GeoPoint(MapUiState.DEFAULT_LAT, MapUiState.DEFAULT_LON))
        
        // Zoom limits
        minZoomLevel = MapUiState.MIN_ZOOM
        maxZoomLevel = MapUiState.MAX_ZOOM

        // ═══════════════════════════════════════════════════════════════════════
        // OVERLAYS
        // ═══════════════════════════════════════════════════════════════════════
        
        // 1. Map tap events
        overlays.add(MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                Timber.d("Map tapped: ${p?.latitude}, ${p?.longitude}")
                return false
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                Timber.d("Map long pressed: ${p?.latitude}, ${p?.longitude}")
                return false
            }
        }))
        
        // 2. Compass overlay
        try {
            val compassOverlay = CompassOverlay(
                context,
                InternalCompassOrientationProvider(context),
                this
            )
            compassOverlay.enableCompass()
            overlays.add(compassOverlay)
        } catch (e: Exception) {
            Timber.w(e, "Compass overlay not available")
        }
        
        // 3. Rotation gestures
        val rotationOverlay = RotationGestureOverlay(this)
        rotationOverlay.isEnabled = true
        overlays.add(rotationOverlay)
        
        // 4. My location overlay
        try {
            val locationOverlay = MyLocationNewOverlay(
                GpsMyLocationProvider(context),
                this
            )
            locationOverlay.enableMyLocation()
            // Don't enable follow by default
            overlays.add(locationOverlay)
        } catch (e: Exception) {
            Timber.w(e, "Location overlay not available")
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// MARKER MANAGEMENT
// ═══════════════════════════════════════════════════════════════════════════════

private fun updateMapMarkers(
    mapView: MapView,
    soldierNodes: List<com.example.tclszero.domain.model.SoldierNode>,
    commandPosts: List<com.example.tclszero.domain.model.CommandPost>
) {
    // Preserve system overlays
    val systemOverlays = mapView.overlays.filter { overlay ->
        overlay is MapEventsOverlay ||
        overlay is CompassOverlay ||
        overlay is RotationGestureOverlay ||
        overlay is MyLocationNewOverlay
    }
    
    // Clear only marker overlays
    mapView.overlays.removeAll { it is Marker }

    // Add soldier node markers (Blue Force Tracking)
    soldierNodes.forEach { node ->
        val marker = Marker(mapView).apply {
            position = GeoPoint(node.latitude, node.longitude)
            title = node.displayName
            snippet = buildString {
                append("Status: ${node.status}")
                if (node.heartRate > 0) append("\nHR: ${node.heartRate} bpm")
                append("\nAmmo: ${node.ammoCount}")
                append("\nMeds: ${node.medsCount}")
            }
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        mapView.overlays.add(marker)
    }

    // Add command post markers
    commandPosts.forEach { post ->
        val marker = Marker(mapView).apply {
            position = GeoPoint(post.latitude, post.longitude)
            title = post.postName
            snippet = buildString {
                append("Ammo: ${post.ammoCapacity}")
                append("\nMeds: ${post.medsCapacity}")
                append("\nRations: ${post.rationCapacity}")
            }
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        mapView.overlays.add(marker)
    }

    mapView.invalidate()
}

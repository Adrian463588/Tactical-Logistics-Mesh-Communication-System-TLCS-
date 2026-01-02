package com.example.tclszero.presentation.map

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

import timber.log.Timber

@Composable
fun OfflineMapScreen(
    viewModel: MapViewModel = hiltViewModel()
) {
    val soldierNodes by viewModel.allNodes.collectAsState(emptyList())
    val commandPosts by viewModel.allPosts.collectAsState(emptyList())
    var mapView: MapView? = null

    Box(modifier = Modifier.fillMaxSize()) {
        // Osmdroid MapView via AndroidView
        AndroidView(
            factory = { context ->
                MapConfig.initializeOsmdroid(context)
                MapView(context).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    controller.setZoom(MapConfig.getDefaultZoom())
                    controller.setCenter(MapConfig.getDefaultCenter())
                    mapView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { view ->
            // Update markers when nodes change
            view.overlays.clear()

            // Add Blue Force Tracking markers
            soldierNodes.forEach { node ->
                val marker = Marker(view).apply {
                    position = GeoPoint(node.latitude, node.longitude)
                    title = node.displayName
                    snippet = "HR: ${node.heartRate} | Ammo: ${node.ammoCount}"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    // TODO: Set custom drawable based on status
                }
                view.overlays.add(marker)
            }

            // Add Command Posts
            commandPosts.forEach { post ->
                val marker = Marker(view).apply {
                    position = GeoPoint(post.latitude, post.longitude)
                    title = post.postName
                    snippet = "Ammo: ${post.ammoCapacity} | Meds: ${post.medsCapacity}"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                view.overlays.add(marker)
            }

            view.invalidate()
        }

        // FAB for Logistics
        FloatingActionButton(
            onClick = { Timber.d("Open logistics") },
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Settings, "Logistics")
        }
    }
}

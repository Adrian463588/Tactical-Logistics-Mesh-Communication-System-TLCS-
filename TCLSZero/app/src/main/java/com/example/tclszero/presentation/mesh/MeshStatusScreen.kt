package com.example.tclszero.presentation.mesh

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tclszero.data.mesh.MeshNetworkManager
import com.example.tclszero.presentation.comms.PttViewModel
import com.example.tclszero.presentation.theme.CommsColors
import com.example.tclszero.presentation.theme.CoreColors

/**
 * MeshStatusScreen - Shows mesh network status and connected nodes
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshStatusScreen(
    viewModel: PttViewModel
) {
    val meshState by viewModel.meshState.collectAsState()
    val connectedEndpoints by viewModel.connectedEndpoints.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when (meshState) {
                    MeshNetworkManager.MeshState.CONNECTED -> CoreColors.Success.copy(alpha = 0.1f)
                    MeshNetworkManager.MeshState.SEARCHING -> CommsColors.Connecting.copy(alpha = 0.1f)
                    MeshNetworkManager.MeshState.ERROR -> CoreColors.Error.copy(alpha = 0.1f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Icon
                Icon(
                    imageVector = when (meshState) {
                        MeshNetworkManager.MeshState.CONNECTED -> Icons.Filled.WifiTethering
                        MeshNetworkManager.MeshState.SEARCHING -> Icons.Filled.WifiFind
                        MeshNetworkManager.MeshState.HANDSHAKE -> Icons.Filled.Handshake
                        MeshNetworkManager.MeshState.ERROR -> Icons.Filled.WifiOff
                        else -> Icons.Filled.Wifi
                    },
                    contentDescription = null,
                    tint = when (meshState) {
                        MeshNetworkManager.MeshState.CONNECTED -> CoreColors.Success
                        MeshNetworkManager.MeshState.SEARCHING -> CommsColors.Connecting
                        MeshNetworkManager.MeshState.ERROR -> CoreColors.Error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Status Text
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Mesh Network",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = when (meshState) {
                            MeshNetworkManager.MeshState.CONNECTED -> "Connected (${connectedEndpoints.size} nodes)"
                            MeshNetworkManager.MeshState.SEARCHING -> "Searching for nodes..."
                            MeshNetworkManager.MeshState.HANDSHAKE -> "Connecting..."
                            MeshNetworkManager.MeshState.ERROR -> "Connection Error"
                            else -> "Idle"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Control Button
                when (meshState) {
                    MeshNetworkManager.MeshState.IDLE,
                    MeshNetworkManager.MeshState.ERROR -> {
                        FilledTonalButton(onClick = { viewModel.startMesh() }) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Start")
                        }
                    }
                    MeshNetworkManager.MeshState.SEARCHING,
                    MeshNetworkManager.MeshState.HANDSHAKE,
                    MeshNetworkManager.MeshState.CONNECTED -> {
                        FilledTonalButton(onClick = { viewModel.stopMesh() }) {
                            Icon(Icons.Filled.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stop")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Connected Nodes Section
        Text(
            text = "Connected Nodes",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (connectedEndpoints.isEmpty()) {
            // Empty State
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.GroupOff,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No nodes connected",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Nearby devices running TLCS will appear here",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Connected Nodes List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(connectedEndpoints) { endpoint ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = CoreColors.Success.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = CoreColors.Success,
                                modifier = Modifier.size(40.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = endpoint.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "ID: ${endpoint.id.take(8)}...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Connected",
                                tint = CoreColors.Success,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Mesh networking uses Bluetooth and Wi-Fi Direct to create a P2P network. No internet required.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

package com.example.tclszero.presentation.permissions

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.tclszero.presentation.theme.CoreColors
import com.example.tclszero.util.PermissionManager

/**
 * PermissionScreen - Handles runtime permission requests
 * 
 * Shows permission status and allows user to grant required permissions
 * for mesh networking, location, and audio.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionScreen(
    onAllPermissionsGranted: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var hasMeshPermissions by remember { mutableStateOf(PermissionManager.hasMeshPermissions(context)) }
    var hasLocationPermissions by remember { mutableStateOf(PermissionManager.hasLocationPermissions(context)) }
    var hasAudioPermissions by remember { mutableStateOf(PermissionManager.hasAudioPermissions(context)) }
    var hasNotificationPermissions by remember { mutableStateOf(PermissionManager.hasNotificationPermissions(context)) }

    // Permission launchers
    val meshPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasMeshPermissions = permissions.values.all { it }
        checkAllPermissions(context, onAllPermissionsGranted)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermissions = permissions.values.all { it }
        checkAllPermissions(context, onAllPermissionsGranted)
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasAudioPermissions = permissions.values.all { it }
        checkAllPermissions(context, onAllPermissionsGranted)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasNotificationPermissions = permissions.values.all { it }
        checkAllPermissions(context, onAllPermissionsGranted)
    }

    // Check if all permissions are already granted
    LaunchedEffect(Unit) {
        if (PermissionManager.hasAllRequiredPermissions(context)) {
            onAllPermissionsGranted()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Permissions Required") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "TLCS Zero requires the following permissions to function properly in offline tactical environments.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Mesh Networking Permission
            PermissionItem(
                title = "Mesh Networking",
                description = "Bluetooth & Nearby for P2P communication",
                icon = Icons.Filled.Bluetooth,
                isGranted = hasMeshPermissions,
                onRequestPermission = {
                    meshPermissionLauncher.launch(PermissionManager.getMeshPermissions())
                }
            )

            // Location Permission
            PermissionItem(
                title = "Location",
                description = "Required for offline maps and mesh discovery",
                icon = Icons.Filled.LocationOn,
                isGranted = hasLocationPermissions,
                onRequestPermission = {
                    locationPermissionLauncher.launch(PermissionManager.getLocationPermissions())
                }
            )

            // Audio Permission
            PermissionItem(
                title = "Microphone",
                description = "Required for Push-to-Talk voice communications",
                icon = Icons.Filled.Mic,
                isGranted = hasAudioPermissions,
                onRequestPermission = {
                    audioPermissionLauncher.launch(PermissionManager.getAudioPermissions())
                }
            )

            // Notification Permission (optional, but good for foreground service)
            if (PermissionManager.getNotificationPermissions().isNotEmpty()) {
                PermissionItem(
                    title = "Notifications",
                    description = "For mesh service status updates",
                    icon = Icons.Filled.Notifications,
                    isGranted = hasNotificationPermissions,
                    onRequestPermission = {
                        notificationPermissionLauncher.launch(PermissionManager.getNotificationPermissions())
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Grant All Button
            Button(
                onClick = {
                    val allPermissions = PermissionManager.getAllRequiredPermissions()
                    meshPermissionLauncher.launch(allPermissions)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Grant All Permissions")
            }
        }
    }
}

@Composable
private fun PermissionItem(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isGranted: Boolean,
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) {
                CoreColors.Success.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) CoreColors.Success else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            if (isGranted) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Granted",
                    tint = CoreColors.Success,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                TextButton(onClick = onRequestPermission) {
                    Text("Grant")
                }
            }
        }
    }
}

private fun checkAllPermissions(
    context: android.content.Context,
    onAllGranted: () -> Unit
) {
    if (PermissionManager.hasAllRequiredPermissions(context)) {
        onAllGranted()
    }
}

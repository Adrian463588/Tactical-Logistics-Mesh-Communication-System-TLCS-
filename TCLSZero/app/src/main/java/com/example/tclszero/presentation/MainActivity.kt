package com.example.tclszero.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tclszero.presentation.comms.PttViewModel
import com.example.tclszero.presentation.map.OfflineMapScreen
import com.example.tclszero.presentation.mesh.MeshStatusScreen
import com.example.tclszero.presentation.permissions.PermissionScreen
import com.example.tclszero.presentation.theme.TlcsTheme
import com.example.tclszero.presentation.theme.LogisticsColors
import com.example.tclszero.presentation.theme.CommsColors
import com.example.tclszero.util.PermissionManager
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TlcsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TlcsApp()
                }
            }
        }
    }
}

@Composable
fun TlcsApp() {
    val context = LocalContext.current
    var hasAllPermissions by remember { 
        mutableStateOf(PermissionManager.hasAllRequiredPermissions(context)) 
    }

    if (!hasAllPermissions) {
        PermissionScreen(
            onAllPermissionsGranted = {
                hasAllPermissions = true
            }
        )
    } else {
        TlcsMainContent()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TlcsMainContent() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val pttViewModel: PttViewModel = hiltViewModel()

    // Start mesh network when app launches
    LaunchedEffect(Unit) {
        pttViewModel.startMesh()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "TLCS ZERO",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            Icons.Filled.Map,
                            contentDescription = "Map",
                            tint = if (selectedTab == 0) LogisticsColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    label = {
                        Text(
                            "Map",
                            color = if (selectedTab == 0) LogisticsColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            Icons.Filled.Mic,
                            contentDescription = "Comms",
                            tint = if (selectedTab == 1) CommsColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    label = {
                        Text(
                            "PTT",
                            color = if (selectedTab == 1) CommsColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            Icons.Filled.Wifi,
                            contentDescription = "Mesh",
                            tint = if (selectedTab == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    label = {
                        Text(
                            "Mesh",
                            color = if (selectedTab == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "Settings",
                            tint = if (selectedTab == 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    label = {
                        Text(
                            "Settings",
                            color = if (selectedTab == 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> OfflineMapScreen()
                1 -> PttCommsScreen(pttViewModel)
                2 -> MeshStatusScreen(pttViewModel)
                3 -> SettingsScreen()
            }
        }
    }
}

@Composable
fun PttCommsScreen(viewModel: PttViewModel) {
    val pttState by viewModel.pttState.collectAsState()
    val meshState by viewModel.meshState.collectAsState()
    val connectedEndpoints by viewModel.connectedEndpoints.collectAsState()

    // Use Communication module colors
    val buttonColor = when (pttState) {
        PttViewModel.PttState.TRANSMITTING -> CommsColors.Transmitting
        PttViewModel.PttState.RECEIVING -> CommsColors.Receiving
        else -> CommsColors.Primary
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Connection Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = CommsColors.Surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "PUSH-TO-TALK",
                    style = MaterialTheme.typography.headlineMedium,
                    color = CommsColors.Primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Connected Nodes: ${connectedEndpoints.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // PTT Button
        Surface(
            modifier = Modifier
                .size(180.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            viewModel.startTransmit()
                            try {
                                tryAwaitRelease()
                            } finally {
                                viewModel.stopTransmit()
                            }
                        }
                    )
                },
            shape = CircleShape,
            color = buttonColor,
            shadowElevation = 12.dp,
            tonalElevation = 4.dp
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = "PTT Mic",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (pttState) {
                            PttViewModel.PttState.TRANSMITTING -> "TALKING"
                            PttViewModel.PttState.RECEIVING -> "LISTENING"
                            else -> "PUSH"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when (pttState) {
                    PttViewModel.PttState.TRANSMITTING -> CommsColors.Transmitting.copy(alpha = 0.1f)
                    PttViewModel.PttState.RECEIVING -> CommsColors.Receiving.copy(alpha = 0.1f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "PTT Status",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = pttState.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = when (pttState) {
                        PttViewModel.PttState.TRANSMITTING -> CommsColors.Transmitting
                        PttViewModel.PttState.RECEIVING -> CommsColors.Receiving
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Permissions Section
        SettingsSection(title = "Permissions") {
            SettingsItem(
                title = "Mesh Network",
                subtitle = "Bluetooth & Nearby permissions"
            )
            SettingsItem(
                title = "Location",
                subtitle = "Required for offline maps"
            )
            SettingsItem(
                title = "Microphone",
                subtitle = "Required for PTT"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Maps Section
        SettingsSection(title = "Offline Maps") {
            SettingsItem(
                title = "Import Tiles",
                subtitle = "Load .mbtiles or .zip archives"
            )
            SettingsItem(
                title = "Clear Cache",
                subtitle = "Free up storage space"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // About Section
        SettingsSection(title = "About") {
            SettingsItem(
                title = "TLCS Zero",
                subtitle = "Tactical Logistics Communication System v1.0"
            )
            SettingsItem(
                title = "100% Offline",
                subtitle = "No internet connection required"
            )
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
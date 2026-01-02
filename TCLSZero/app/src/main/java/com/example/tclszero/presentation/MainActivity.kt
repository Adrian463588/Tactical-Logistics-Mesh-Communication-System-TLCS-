package com.example.tclszero.presentation

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import android.Manifest


import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel

import com.example.tclszero.presentation.comms.PttViewModel
import com.example.tclszero.presentation.map.OfflineMapScreen
import com.example.tclszero.presentation.theme.TlcsTheme
import dagger.hilt.android.AndroidEntryPoint
import org.osmdroid.bonuspack.BuildConfig
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Timber logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Request microphone permission
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                PERMISSION_REQUEST_CODE
            )
        }

        setContent {
            TlcsTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TlcsApp()
                }
            }
        }
    }
    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }
}

@Composable
fun TlcsApp() {
    var selectedTab by remember { mutableIntStateOf(0) }
    // Ideally, hoist this ViewModel or use navigation graph,
    // but this works for a simple 2-screen setup.
    val pttViewModel: PttViewModel = hiltViewModel()

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Filled.Info, contentDescription = "Map") },
                    label = { Text("Map") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.Mic, contentDescription = "Comms") },
                    label = { Text("PTT") }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> OfflineMapScreen()
                1 -> PttCommsScreen(pttViewModel)
            }
        }
    }
}

@Composable
fun PttCommsScreen(viewModel: PttViewModel) {
    val pttState by viewModel.pttState.collectAsState()

    // Determine button color based on state
    val buttonColor = if (pttState == PttViewModel.PttState.TRANSMITTING) {
        MaterialTheme.colorScheme.error // Red when talking
    } else {
        MaterialTheme.colorScheme.primary // Blue/Primary when idle
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("PTT COMMS", style = MaterialTheme.typography.headlineLarge)

        Spacer(modifier = Modifier.height(32.dp))

        // Custom PTT Button Logic
        // We use Surface instead of Button to have fine-grained control over touch events
        Surface(
            modifier = Modifier
                .size(160.dp) // Made slightly larger for easier hitting
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            // 1. Finger touches down -> Start Transmitting
                            viewModel.startTransmit()
                            try {
                                // 2. Wait for finger release
                                tryAwaitRelease()
                            } finally {
                                // 3. Finger lifted or gesture cancelled -> Stop Transmitting
                                viewModel.stopTransmit()
                            }
                        }
                    )
                },
            shape = CircleShape,
            color = buttonColor,
            shadowElevation = 8.dp
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
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = if (pttState == PttViewModel.PttState.TRANSMITTING) "TALKING" else "PUSH",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Status: ${pttState.name}",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
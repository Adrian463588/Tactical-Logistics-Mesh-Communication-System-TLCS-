package com.example.tclszero.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.tclszero.data.mesh.MeshNetworkManager
import com.example.tclszero.R
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MeshForegroundService : Service() {

    @Inject
    lateinit var meshNetworkManager: MeshNetworkManager

    override fun onCreate() {
        super.onCreate()
        Timber.d("Mesh Foreground Service: onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, "mesh_channel")
            .setContentTitle("TLCS Mesh Active")
            .setContentText("P2P network running...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)

        // Start mesh network
        meshNetworkManager.startMeshNetwork("TacticalNode")

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        meshNetworkManager.stopMesh()
        Timber.d("Mesh Foreground Service: onDestroy")
    }
}

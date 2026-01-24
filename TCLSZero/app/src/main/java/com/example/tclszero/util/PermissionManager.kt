package com.example.tclszero.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import timber.log.Timber

/**
 * Centralized Permission Manager for TLCS Zero
 * 
 * Handles all runtime permissions for:
 * - Mesh Networking (Bluetooth, Nearby, Location)
 * - Audio (Microphone for PTT)
 * - Storage (Offline map tiles)
 * - Notifications
 */
object PermissionManager {

    // ═══════════════════════════════════════════════════════════════════════════
    // PERMISSION GROUPS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Permissions required for Mesh Networking (Nearby Connections API)
     */
    fun getMeshPermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+ (API 33+)
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.NEARBY_WIFI_DEVICES,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                // Android 12 (API 31-32)
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
            else -> {
                // Android 11 and below (API 30-)
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
        }
    }

    /**
     * Permissions required for Location (Map & geolocation)
     */
    fun getLocationPermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    /**
     * Permissions required for Audio (PTT)
     */
    fun getAudioPermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.RECORD_AUDIO
        )
    }

    /**
     * Permissions required for Storage (Tile import)
     */
    fun getStoragePermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+ uses granular media permissions
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // Android 11-12: Use SAF, no special permissions needed
                emptyArray()
            }
            else -> {
                // Android 10 and below
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
        }
    }

    /**
     * Permissions required for Notifications
     */
    fun getNotificationPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyArray()
        }
    }

    /**
     * All permissions required for full app functionality
     */
    fun getAllRequiredPermissions(): Array<String> {
        return (getMeshPermissions() +
                getLocationPermissions() +
                getAudioPermissions() +
                getStoragePermissions() +
                getNotificationPermissions()).distinct().toTypedArray()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PERMISSION CHECKING
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Check if all mesh networking permissions are granted
     */
    fun hasMeshPermissions(context: Context): Boolean {
        return hasAllPermissions(context, getMeshPermissions())
    }

    /**
     * Check if location permissions are granted
     */
    fun hasLocationPermissions(context: Context): Boolean {
        return hasAllPermissions(context, getLocationPermissions())
    }

    /**
     * Check if audio permissions are granted
     */
    fun hasAudioPermissions(context: Context): Boolean {
        return hasAllPermissions(context, getAudioPermissions())
    }

    /**
     * Check if storage permissions are granted (or not needed on API 30+)
     */
    fun hasStoragePermissions(context: Context): Boolean {
        val permissions = getStoragePermissions()
        return permissions.isEmpty() || hasAllPermissions(context, permissions)
    }

    /**
     * Check if notification permissions are granted
     */
    fun hasNotificationPermissions(context: Context): Boolean {
        val permissions = getNotificationPermissions()
        return permissions.isEmpty() || hasAllPermissions(context, permissions)
    }

    /**
     * Check if all required permissions are granted
     */
    fun hasAllRequiredPermissions(context: Context): Boolean {
        return hasAllPermissions(context, getAllRequiredPermissions())
    }

    /**
     * Generic permission check helper
     */
    fun hasAllPermissions(context: Context, permissions: Array<String>): Boolean {
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Get list of missing permissions
     */
    fun getMissingPermissions(context: Context, permissions: Array<String>): List<String> {
        return permissions.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PERMISSION REQUESTING
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Request mesh networking permissions
     */
    fun requestMeshPermissions(activity: Activity, requestCode: Int = REQUEST_MESH_PERMISSIONS) {
        val missing = getMissingPermissions(activity, getMeshPermissions())
        if (missing.isNotEmpty()) {
            Timber.d("Requesting mesh permissions: $missing")
            ActivityCompat.requestPermissions(activity, missing.toTypedArray(), requestCode)
        }
    }

    /**
     * Request location permissions
     */
    fun requestLocationPermissions(activity: Activity, requestCode: Int = REQUEST_LOCATION_PERMISSIONS) {
        val missing = getMissingPermissions(activity, getLocationPermissions())
        if (missing.isNotEmpty()) {
            Timber.d("Requesting location permissions: $missing")
            ActivityCompat.requestPermissions(activity, missing.toTypedArray(), requestCode)
        }
    }

    /**
     * Request audio permissions
     */
    fun requestAudioPermissions(activity: Activity, requestCode: Int = REQUEST_AUDIO_PERMISSIONS) {
        val missing = getMissingPermissions(activity, getAudioPermissions())
        if (missing.isNotEmpty()) {
            Timber.d("Requesting audio permissions: $missing")
            ActivityCompat.requestPermissions(activity, missing.toTypedArray(), requestCode)
        }
    }

    /**
     * Request all required permissions
     */
    fun requestAllPermissions(activity: Activity, requestCode: Int = REQUEST_ALL_PERMISSIONS) {
        val missing = getMissingPermissions(activity, getAllRequiredPermissions())
        if (missing.isNotEmpty()) {
            Timber.d("Requesting all permissions: $missing")
            ActivityCompat.requestPermissions(activity, missing.toTypedArray(), requestCode)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PERMISSION RATIONALE
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Check if we should show permission rationale
     */
    fun shouldShowRationale(activity: Activity, permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    /**
     * Check if any mesh permission needs rationale
     */
    fun shouldShowMeshRationale(activity: Activity): Boolean {
        return getMeshPermissions().any { shouldShowRationale(activity, it) }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // RESULT HANDLING
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Process permission result and return status
     */
    fun processPermissionResult(
        permissions: Array<out String>,
        grantResults: IntArray
    ): PermissionResult {
        val granted = mutableListOf<String>()
        val denied = mutableListOf<String>()

        permissions.forEachIndexed { index, permission ->
            if (grantResults.getOrNull(index) == PackageManager.PERMISSION_GRANTED) {
                granted.add(permission)
            } else {
                denied.add(permission)
            }
        }

        return PermissionResult(
            allGranted = denied.isEmpty(),
            granted = granted,
            denied = denied
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DATA CLASSES & CONSTANTS
    // ═══════════════════════════════════════════════════════════════════════════

    data class PermissionResult(
        val allGranted: Boolean,
        val granted: List<String>,
        val denied: List<String>
    )

    // Request codes
    const val REQUEST_MESH_PERMISSIONS = 1001
    const val REQUEST_LOCATION_PERMISSIONS = 1002
    const val REQUEST_AUDIO_PERMISSIONS = 1003
    const val REQUEST_STORAGE_PERMISSIONS = 1004
    const val REQUEST_NOTIFICATION_PERMISSIONS = 1005
    const val REQUEST_ALL_PERMISSIONS = 1000
}

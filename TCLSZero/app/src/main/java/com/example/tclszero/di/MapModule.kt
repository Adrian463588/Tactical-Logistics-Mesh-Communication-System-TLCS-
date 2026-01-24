package com.example.tclszero.di

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.example.tclszero.data.map.OfflineTileProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.osmdroid.config.Configuration
import org.osmdroid.config.IConfigurationProvider
import java.io.File
import javax.inject.Singleton

/**
 * MapModule - Hilt Dependency Injection for OSMDroid Map Configuration
 * 
 * Provides:
 * - IConfigurationProvider: OSMDroid's global configuration
 * - OfflineTileProvider: Manages offline tile loading
 * 
 * CRITICAL: This module initializes osmdroid configuration before any MapView is created.
 */
@Module
@InstallIn(SingletonComponent::class)
object MapModule {

    /**
     * Provides SharedPreferences for osmdroid configuration storage
     */
    @Provides
    @Singleton
    fun provideSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    /**
     * Provides and initializes the OSMDroid Configuration
     * 
     * CRITICAL: This MUST be called before any MapView is instantiated.
     * The configuration is loaded from SharedPreferences and sets up:
     * - User agent (required by tile servers)
     * - Cache paths (app-private for Scoped Storage)
     * - Tile caching settings
     * - Offline mode settings
     */
    @Provides
    @Singleton
    fun provideOsmdroidConfiguration(
        @ApplicationContext context: Context,
        sharedPreferences: SharedPreferences
    ): IConfigurationProvider {
        val config = Configuration.getInstance()
        
        // CRITICAL: Load configuration from SharedPreferences
        // This MUST happen before any MapView is created
        config.load(context, sharedPreferences)
        
        // ═══════════════════════════════════════════════════════════════════════
        // USER AGENT (Required by tile servers and for identification)
        // ═══════════════════════════════════════════════════════════════════════
        config.userAgentValue = "TLCSZero/1.0 (Android; Tactical Logistics System)"
        
        // ═══════════════════════════════════════════════════════════════════════
        // STORAGE PATHS (App-private for Scoped Storage compatibility)
        // ═══════════════════════════════════════════════════════════════════════
        val osmdroidBasePath = File(context.filesDir, "osmdroid")
        if (!osmdroidBasePath.exists()) {
            osmdroidBasePath.mkdirs()
        }
        config.osmdroidBasePath = osmdroidBasePath

        val tileCachePath = File(osmdroidBasePath, "tiles")
        if (!tileCachePath.exists()) {
            tileCachePath.mkdirs()
        }
        config.osmdroidTileCache = tileCachePath
        
        // ═══════════════════════════════════════════════════════════════════════
        // CACHE SETTINGS
        // ═══════════════════════════════════════════════════════════════════════
        
        // In-memory tile cache
        config.cacheMapTileCount = 12.toShort()
        config.cacheMapTileOvershoot = 4.toShort()
        
        // File system cache
        config.tileFileSystemCacheMaxBytes = 500L * 1024 * 1024  // 500 MB max
        config.tileFileSystemCacheTrimBytes = 400L * 1024 * 1024 // Trim to 400 MB
        
        // ═══════════════════════════════════════════════════════════════════════
        // THREAD POOL SETTINGS
        // CRITICAL: Do NOT set to 0, this causes IllegalArgumentException
        // For offline-first, set to 1 (minimum) - tiles will come from cache
        // ═══════════════════════════════════════════════════════════════════════
        config.tileDownloadThreads = 1.toShort()  // Minimum 1 to avoid crash
        config.tileFileSystemThreads = 2.toShort()
        config.tileDownloadMaxQueueSize = 4.toShort()
        
        // ═══════════════════════════════════════════════════════════════════════
        // EXPIRATION SETTINGS (For offline tiles)
        // ═══════════════════════════════════════════════════════════════════════
        config.expirationOverrideDuration = Long.MAX_VALUE  // Never expire local tiles
        config.expirationExtendedDuration = Long.MAX_VALUE
        
        // ═══════════════════════════════════════════════════════════════════════
        // PERFORMANCE SETTINGS
        // ═══════════════════════════════════════════════════════════════════════
        config.isMapViewHardwareAccelerated = true
        
        // Debug mode (disable in production)
        config.isDebugMode = false
        config.isDebugMapView = false
        config.isDebugTileProviders = false
        
        // Save configuration
        config.save(context, sharedPreferences)
        
        return config
    }

    /**
     * Provides OfflineTileProvider for managing offline map tiles
     */
    @Provides
    @Singleton
    fun provideOfflineTileProvider(
        @ApplicationContext context: Context
    ): OfflineTileProvider {
        return OfflineTileProvider(context)
    }
}

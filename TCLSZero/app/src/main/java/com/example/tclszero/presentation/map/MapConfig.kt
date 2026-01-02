package com.example.tclszero.presentation.map



import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import android.content.Context

object MapConfig {

    fun initializeOsmdroid(context: Context) {
        // Set cache path
        Configuration.getInstance().osmdroidBasePath = context.filesDir
        Configuration.getInstance().cacheMapTileCount = 500
        Configuration.getInstance().cacheMapTileOvershoot = 50

        // Disable network tile requests (100% offline)
        Configuration.getInstance().isDebugMode = false
    }

    // Default Center: Tactical Operating Area (Yemen/Iraq region)
    fun getDefaultCenter(): GeoPoint = GeoPoint(15.3694, 44.1910)

    fun getDefaultZoom(): Double = 10.0
}

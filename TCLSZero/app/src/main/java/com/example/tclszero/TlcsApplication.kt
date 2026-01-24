package com.example.tclszero

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.IConfigurationProvider
import timber.log.Timber
import javax.inject.Inject

/**
 * TlcsApplication - Application class for TLCS Zero
 * 
 * Hilt entry point. The IConfigurationProvider is injected to ensure
 * osmdroid is configured at app startup, before any MapView is created.
 */
@HiltAndroidApp
class TlcsApplication : Application() {

    // Inject the configuration provider to ensure it's initialized at app start
    @Inject
    lateinit var osmdroidConfig: IConfigurationProvider

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Timber.d("TLCS Zero Application initialized")
        Timber.d("OSMDroid user agent: ${osmdroidConfig.userAgentValue}")
    }
}
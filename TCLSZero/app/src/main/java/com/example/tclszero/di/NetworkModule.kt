package com.example.tclszero.di

import android.content.Context
import com.example.tclszero.data.mesh.MeshNetworkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * NetworkModule - Provides networking dependencies
 * 
 * Includes:
 * - MeshNetworkManager for P2P mesh communications
 * 
 * Note: OfflineTileProvider is provided by MapModule
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMeshNetworkManager(
        @ApplicationContext context: Context
    ): MeshNetworkManager = MeshNetworkManager(context)
}

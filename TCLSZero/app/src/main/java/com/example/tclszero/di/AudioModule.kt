package com.example.tclszero.di

import com.example.tclszero.data.audio.AudioStreamManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * AudioModule - Provides audio-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object AudioModule {

    @Provides
    @Singleton
    fun provideAudioStreamManager(): AudioStreamManager = AudioStreamManager()
}

package com.example.tclszero.di

import kotlin.jvm.java

import android.content.Context
import androidx.room.Room
import com.example.tclszero.data.local.AppDatabase

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "tlcs_database"
    ).build()

    @Provides
    @Singleton
    fun provideSoldierNodeDao(database: AppDatabase) = database.soldierNodeDao()

    @Provides
    @Singleton
    fun provideCommandPostDao(database: AppDatabase) = database.commandPostDao()
}

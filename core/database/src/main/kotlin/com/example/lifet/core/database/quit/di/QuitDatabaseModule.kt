package com.example.lifet.core.database.quit.di

import com.example.lifet.core.database.LifeTrackerDatabase
import com.example.lifet.core.database.quit.QuitDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object QuitDatabaseModule {
    @Provides
    fun provideQuitDao(database: LifeTrackerDatabase): QuitDao = database.quitDao()
}

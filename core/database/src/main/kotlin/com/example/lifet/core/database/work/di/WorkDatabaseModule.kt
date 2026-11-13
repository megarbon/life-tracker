package com.example.lifet.core.database.work.di

import com.example.lifet.core.database.LifeTrackerDatabase
import com.example.lifet.core.database.work.WorkDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object WorkDatabaseModule {
    @Provides
    fun provideWorkDao(database: LifeTrackerDatabase): WorkDao = database.workDao()
}

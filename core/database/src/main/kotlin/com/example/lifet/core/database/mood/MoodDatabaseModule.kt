package com.example.lifet.core.database.mood

import com.example.lifet.core.database.LifeTrackerDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object MoodDatabaseModule {
    @Provides
    fun provideMoodDao(database: LifeTrackerDatabase): MoodDao = database.moodDao()
}

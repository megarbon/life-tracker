package com.example.lifet.core.database.fitness.di

import com.example.lifet.core.database.LifeTrackerDatabase
import com.example.lifet.core.database.fitness.FitnessDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object FitnessDatabaseModule {
    @Provides
    fun provideFitnessDao(database: LifeTrackerDatabase): FitnessDao = database.fitnessDao()
}

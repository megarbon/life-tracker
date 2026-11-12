package com.example.lifet.core.data.fitness.di

import com.example.lifet.core.data.fitness.ActivitySource
import com.example.lifet.core.data.fitness.FitnessRepository
import com.example.lifet.core.data.fitness.RoomFitnessRepository
import com.example.lifet.core.data.fitness.UnavailableActivitySource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class FitnessDataModule {
    @Binds
    abstract fun bindFitnessRepository(impl: RoomFitnessRepository): FitnessRepository

    /** Mientras no exista Health Connect, la fuente declara que no está disponible. */
    @Binds
    abstract fun bindActivitySource(impl: UnavailableActivitySource): ActivitySource
}

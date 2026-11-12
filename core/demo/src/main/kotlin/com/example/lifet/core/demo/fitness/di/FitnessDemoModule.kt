package com.example.lifet.core.demo.fitness.di

import com.example.lifet.core.demo.AreaDemoDataGenerator
import com.example.lifet.core.demo.fitness.FitnessDemoDataGenerator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal abstract class FitnessDemoModule {
    @Binds
    @IntoSet
    abstract fun bindFitnessGenerator(generator: FitnessDemoDataGenerator): AreaDemoDataGenerator
}

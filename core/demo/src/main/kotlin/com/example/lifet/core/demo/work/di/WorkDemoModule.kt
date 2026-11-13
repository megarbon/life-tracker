package com.example.lifet.core.demo.work.di

import com.example.lifet.core.demo.AreaDemoDataGenerator
import com.example.lifet.core.demo.work.WorkDemoDataGenerator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal abstract class WorkDemoModule {
    @Binds
    @IntoSet
    abstract fun bindWorkGenerator(generator: WorkDemoDataGenerator): AreaDemoDataGenerator
}

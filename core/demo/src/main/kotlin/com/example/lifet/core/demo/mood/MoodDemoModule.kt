package com.example.lifet.core.demo.mood

import com.example.lifet.core.demo.AreaDemoDataGenerator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal abstract class MoodDemoModule {
    @Binds
    @IntoSet
    abstract fun bindMoodGenerator(generator: MoodDemoDataGenerator): AreaDemoDataGenerator
}

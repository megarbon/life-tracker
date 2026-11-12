package com.example.lifet.core.data.mood.di

import com.example.lifet.core.data.mood.DefaultMoodRepository
import com.example.lifet.core.data.mood.MoodRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class MoodDataModule {
    @Binds
    abstract fun bindMoodRepository(impl: DefaultMoodRepository): MoodRepository
}

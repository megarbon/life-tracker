package com.example.lifet.core.data.quit.di

import com.example.lifet.core.data.quit.QuitRepository
import com.example.lifet.core.data.quit.RoomQuitRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class QuitDataModule {
    @Binds
    abstract fun bindQuitRepository(impl: RoomQuitRepository): QuitRepository
}

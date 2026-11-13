package com.example.lifet.core.demo.quit.di

import com.example.lifet.core.demo.AreaDemoDataGenerator
import com.example.lifet.core.demo.quit.QuitDemoDataGenerator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal abstract class QuitDemoModule {
    @Binds
    @IntoSet
    abstract fun bindQuitGenerator(generator: QuitDemoDataGenerator): AreaDemoDataGenerator
}

package com.example.lifet.core.demo.di

import com.example.lifet.core.demo.AreaDemoDataGenerator
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds

/**
 * Declara el conjunto vacío para que Hilt compile aunque ningún área haya registrado su generador todavía.
 * Cada área añade el suyo con `@Binds @IntoSet` en su propio módulo.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class DemoModule {
    @Multibinds
    abstract fun generators(): Set<AreaDemoDataGenerator>
}

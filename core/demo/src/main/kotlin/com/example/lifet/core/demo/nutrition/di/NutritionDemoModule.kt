package com.example.lifet.core.demo.nutrition.di

import com.example.lifet.core.demo.AreaDemoDataGenerator
import com.example.lifet.core.demo.nutrition.NutritionDemoDataGenerator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/** Registra el generador de Alimentación en el conjunto de `DemoModule` sin tocar ningún fichero compartido. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class NutritionDemoModule {
    @Binds
    @IntoSet
    abstract fun bindGenerator(generator: NutritionDemoDataGenerator): AreaDemoDataGenerator
}

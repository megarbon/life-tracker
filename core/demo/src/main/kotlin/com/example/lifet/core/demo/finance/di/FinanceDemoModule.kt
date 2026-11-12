package com.example.lifet.core.demo.finance.di

import com.example.lifet.core.demo.AreaDemoDataGenerator
import com.example.lifet.core.demo.finance.FinanceDemoDataGenerator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal abstract class FinanceDemoModule {
    @Binds
    @IntoSet
    abstract fun bindFinanceGenerator(generator: FinanceDemoDataGenerator): AreaDemoDataGenerator
}

package com.example.lifet.core.database.finance.di

import com.example.lifet.core.database.LifeTrackerDatabase
import com.example.lifet.core.database.finance.FinanceDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object FinanceDatabaseModule {
    @Provides
    fun provideFinanceDao(database: LifeTrackerDatabase): FinanceDao = database.financeDao()
}

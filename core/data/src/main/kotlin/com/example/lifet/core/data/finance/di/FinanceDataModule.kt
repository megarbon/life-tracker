package com.example.lifet.core.data.finance.di

import com.example.lifet.core.data.finance.FinanceRepository
import com.example.lifet.core.data.finance.RoomFinanceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class FinanceDataModule {
    @Binds
    abstract fun bindFinanceRepository(impl: RoomFinanceRepository): FinanceRepository
}

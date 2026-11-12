package com.example.lifet.core.data.nutrition.di

import com.example.lifet.core.data.nutrition.FoodRepository
import com.example.lifet.core.data.nutrition.NutritionGoalsRepository
import com.example.lifet.core.data.nutrition.NutritionLogRepository
import com.example.lifet.core.data.nutrition.OfflineFoodRepository
import com.example.lifet.core.data.nutrition.OfflineNutritionGoalsRepository
import com.example.lifet.core.data.nutrition.OfflineNutritionLogRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NutritionDataModule {
    @Binds
    abstract fun bindLogRepository(impl: OfflineNutritionLogRepository): NutritionLogRepository

    @Binds
    abstract fun bindGoalsRepository(impl: OfflineNutritionGoalsRepository): NutritionGoalsRepository

    @Binds
    abstract fun bindFoodRepository(impl: OfflineFoodRepository): FoodRepository
}

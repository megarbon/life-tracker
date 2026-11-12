package com.example.lifet.core.database.nutrition.di

import com.example.lifet.core.database.LifeTrackerDatabase
import com.example.lifet.core.database.nutrition.NutritionFoodDao
import com.example.lifet.core.database.nutrition.NutritionGoalsDao
import com.example.lifet.core.database.nutrition.NutritionMealDao
import com.example.lifet.core.database.nutrition.NutritionWaterDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** DAOs de Alimentación; módulo propio para no editar `DatabaseModule`, que es compartido. */
@Module
@InstallIn(SingletonComponent::class)
internal object NutritionDatabaseModule {
    @Provides
    fun provideFoodDao(database: LifeTrackerDatabase): NutritionFoodDao = database.nutritionFoodDao()

    @Provides
    fun provideMealDao(database: LifeTrackerDatabase): NutritionMealDao = database.nutritionMealDao()

    @Provides
    fun provideWaterDao(database: LifeTrackerDatabase): NutritionWaterDao = database.nutritionWaterDao()

    @Provides
    fun provideGoalsDao(database: LifeTrackerDatabase): NutritionGoalsDao = database.nutritionGoalsDao()
}

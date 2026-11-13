package com.example.lifet.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.lifet.core.database.nutrition.NutritionFoodDao
import com.example.lifet.core.database.nutrition.NutritionFoodEntity
import com.example.lifet.core.database.nutrition.NutritionGoalsDao
import com.example.lifet.core.database.nutrition.NutritionGoalsEntity
import com.example.lifet.core.database.nutrition.NutritionMealDao
import com.example.lifet.core.database.nutrition.NutritionMealEntity
import com.example.lifet.core.database.nutrition.NutritionWaterDao
import com.example.lifet.core.database.nutrition.NutritionWaterEntity

/**
 * Base de datos única de la app (SQLite vía Room), fuente de verdad offline-first.
 *
 * Las entidades de cada área viven en su subpaquete (`finance/`, `nutrition/`…) y se registran aquí.
 * Cada cambio de esquema exige subir [VERSION] y aportar una migración con su test; los esquemas
 * exportados en `schemas/` se versionan en git.
 */
@Database(
    entities = [
        AppMetaEntity::class,
        com.example.lifet.core.database.mood.MoodEntryEntity::class,
        com.example.lifet.core.database.finance.FinanceAccountEntity::class,
        com.example.lifet.core.database.finance.FinanceCategoryEntity::class,
        com.example.lifet.core.database.finance.FinanceTransactionEntity::class,
        com.example.lifet.core.database.finance.FinanceTransferEntity::class,
        com.example.lifet.core.database.finance.FinanceBudgetEntity::class,
        com.example.lifet.core.database.finance.FinanceRecurringRuleEntity::class,
        NutritionFoodEntity::class,
        NutritionMealEntity::class,
        NutritionWaterEntity::class,
        NutritionGoalsEntity::class,
        com.example.lifet.core.database.fitness.FitnessWorkoutEntity::class,
        com.example.lifet.core.database.fitness.FitnessSetEntity::class,
        com.example.lifet.core.database.fitness.FitnessWeightEntity::class,
        com.example.lifet.core.database.fitness.FitnessStepsEntity::class,
        com.example.lifet.core.database.fitness.FitnessTemplateEntity::class,
        com.example.lifet.core.database.fitness.FitnessTemplateExerciseEntity::class,
        com.example.lifet.core.database.quit.QuitHabitEntity::class,
        com.example.lifet.core.database.quit.QuitCravingEntity::class,
        com.example.lifet.core.database.quit.QuitConsumptionEntity::class,
    ],
    version = LifeTrackerDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
internal abstract class LifeTrackerDatabase : RoomDatabase() {
    abstract fun appMetaDao(): AppMetaDao

    abstract fun moodDao(): com.example.lifet.core.database.mood.MoodDao

    abstract fun financeDao(): com.example.lifet.core.database.finance.FinanceDao

    abstract fun nutritionFoodDao(): NutritionFoodDao

    abstract fun nutritionMealDao(): NutritionMealDao

    abstract fun nutritionWaterDao(): NutritionWaterDao

    abstract fun nutritionGoalsDao(): NutritionGoalsDao

    abstract fun fitnessDao(): com.example.lifet.core.database.fitness.FitnessDao

    abstract fun quitDao(): com.example.lifet.core.database.quit.QuitDao

    companion object {
        const val VERSION = 1
        const val NAME = "life-tracker.db"
    }
}

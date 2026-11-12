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
        NutritionFoodEntity::class,
        NutritionMealEntity::class,
        NutritionWaterEntity::class,
        NutritionGoalsEntity::class,
    ],
    version = LifeTrackerDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
internal abstract class LifeTrackerDatabase : RoomDatabase() {
    abstract fun appMetaDao(): AppMetaDao

    abstract fun nutritionFoodDao(): NutritionFoodDao

    abstract fun nutritionMealDao(): NutritionMealDao

    abstract fun nutritionWaterDao(): NutritionWaterDao

    abstract fun nutritionGoalsDao(): NutritionGoalsDao

    companion object {
        const val VERSION = 1
        const val NAME = "life-tracker.db"
    }
}

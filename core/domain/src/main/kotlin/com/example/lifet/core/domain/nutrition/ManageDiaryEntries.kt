package com.example.lifet.core.domain.nutrition

import com.example.lifet.core.common.AppError
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.data.nutrition.NutritionLogRepository
import com.example.lifet.core.model.nutrition.MealEntry
import com.example.lifet.core.model.nutrition.MealType
import com.example.lifet.core.model.nutrition.WaterEntry
import javax.inject.Inject

/**
 * Edición del diario con deshacer: cambiar cantidad o comida, borrar y restaurar. Quien llama conserva las
 * entradas originales para poder devolverlas con [restoreMeals] (guardar es un upsert por id).
 */
public class ManageDiaryEntries
    @Inject
    constructor(
        private val log: NutritionLogRepository,
    ) {
        /** Cambia gramos y/o comida de [entry]; los nutrientes se reescalan. Devuelve la entrada nueva. */
        public suspend fun updateMeal(
            entry: MealEntry,
            grams: Int,
            mealType: MealType,
        ): AppResult<MealEntry> {
            val updated = entry.withGrams(grams).copy(mealType = mealType)
            return when (val result = log.saveMeals(listOf(updated))) {
                is AppResult.Success -> AppResult.Success(updated)
                is AppResult.Failure -> result
            }
        }

        public suspend fun deleteMeals(entries: List<MealEntry>): AppResult<Unit> {
            var failure: AppError? = null
            entries.forEach { entry ->
                val result = log.deleteMeal(entry.id)
                if (result is AppResult.Failure && failure == null) failure = result.error
            }
            return failure?.let { AppResult.Failure(it) } ?: AppResult.Success(Unit)
        }

        public suspend fun restoreMeals(entries: List<MealEntry>): AppResult<Unit> = log.saveMeals(entries)

        public suspend fun deleteWater(entry: WaterEntry): AppResult<Unit> = log.deleteWater(entry.id)

        public suspend fun restoreWater(entry: WaterEntry): AppResult<Unit> = log.saveWater(listOf(entry))
    }

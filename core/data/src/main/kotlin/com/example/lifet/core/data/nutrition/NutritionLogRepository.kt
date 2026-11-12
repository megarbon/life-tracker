package com.example.lifet.core.data.nutrition

import com.example.lifet.core.common.AppResult
import com.example.lifet.core.model.nutrition.MealEntry
import com.example.lifet.core.model.nutrition.WaterEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Diario de Alimentación: alimentos registrados por comida y agua. Room es la fuente de verdad. */
public interface NutritionLogRepository {
    public fun observeMeals(date: LocalDate): Flow<List<MealEntry>>

    /** Entradas de `[start, endExclusive)`, ordenadas por día y hora. */
    public fun observeMealsBetween(
        start: LocalDate,
        endExclusive: LocalDate,
    ): Flow<List<MealEntry>>

    public fun observeWater(date: LocalDate): Flow<List<WaterEntry>>

    public fun observeWaterBetween(
        start: LocalDate,
        endExclusive: LocalDate,
    ): Flow<List<WaterEntry>>

    public suspend fun getMeals(date: LocalDate): AppResult<List<MealEntry>>

    /** Inserta o sustituye (por id). Sirve para registrar, editar, deshacer un borrado y "repetir ayer". */
    public suspend fun saveMeals(entries: List<MealEntry>): AppResult<Unit>

    public suspend fun deleteMeal(id: String): AppResult<Unit>

    /** Inserta o sustituye agua por lotes (una sola transacción). */
    public suspend fun saveWater(entries: List<WaterEntry>): AppResult<Unit>

    public suspend fun deleteWater(id: String): AppResult<Unit>

    /** Borra todo el diario (comidas y agua). Lo usan los escenarios de demo y "borrar datos". */
    public suspend fun clearAll(): AppResult<Unit>
}

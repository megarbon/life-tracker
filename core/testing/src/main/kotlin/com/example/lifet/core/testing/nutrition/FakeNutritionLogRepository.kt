package com.example.lifet.core.testing.nutrition

import com.example.lifet.core.common.AppError
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.data.nutrition.NutritionLogRepository
import com.example.lifet.core.model.nutrition.MealEntry
import com.example.lifet.core.model.nutrition.WaterEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.LocalDate

/**
 * Diario en memoria. [failReads] hace que los flujos lancen (para probar el estado Error) y [failWrites] que las
 * escrituras devuelvan `AppError.Storage`.
 */
public class FakeNutritionLogRepository : NutritionLogRepository {
    public val meals: MutableStateFlow<List<MealEntry>> = MutableStateFlow(emptyList())
    public val water: MutableStateFlow<List<WaterEntry>> = MutableStateFlow(emptyList())
    public var failReads: Boolean = false
    public var failWrites: Boolean = false

    override fun observeMeals(date: LocalDate): Flow<List<MealEntry>> =
        meals
            .map { all ->
                all
                    .filter {
                        it.date == date
                    }.sortedBy { it.loggedAt }
            }.guardedBy { failReads }

    override fun observeMealsBetween(
        start: LocalDate,
        endExclusive: LocalDate,
    ): Flow<List<MealEntry>> =
        meals.map { all -> all.filter { it.date >= start && it.date < endExclusive } }.guardedBy { failReads }

    override fun observeWater(date: LocalDate): Flow<List<WaterEntry>> =
        water
            .map { all ->
                all.filter {
                    it.date == date
                }
            }.guardedBy { failReads }

    override fun observeWaterBetween(
        start: LocalDate,
        endExclusive: LocalDate,
    ): Flow<List<WaterEntry>> =
        water.map { all -> all.filter { it.date >= start && it.date < endExclusive } }.guardedBy { failReads }

    override suspend fun getMeals(date: LocalDate): AppResult<List<MealEntry>> =
        writing { meals.value.filter { it.date == date } }

    override suspend fun saveMeals(entries: List<MealEntry>): AppResult<Unit> =
        writing { meals.value = meals.value.filterNot { old -> entries.any { it.id == old.id } } + entries }

    override suspend fun deleteMeal(id: String): AppResult<Unit> =
        writing {
            meals.value =
                meals.value.filterNot { it.id == id }
        }

    override suspend fun saveWater(entries: List<WaterEntry>): AppResult<Unit> =
        writing { water.value = water.value.filterNot { old -> entries.any { it.id == old.id } } + entries }

    override suspend fun deleteWater(id: String): AppResult<Unit> =
        writing {
            water.value =
                water.value.filterNot { it.id == id }
        }

    override suspend fun clearAll(): AppResult<Unit> =
        writing {
            meals.value = emptyList()
            water.value = emptyList()
        }
}

private fun <T> Flow<T>.guardedBy(fail: () -> Boolean): Flow<T> =
    map { if (fail()) throw IOException("lectura simulada") else it }

private inline fun <T> FakeNutritionLogRepository.writing(block: () -> T): AppResult<T> =
    if (failWrites) AppResult.Failure(AppError.Storage) else AppResult.Success(block())

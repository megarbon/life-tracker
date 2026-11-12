package com.example.lifet.core.data.nutrition

import com.example.lifet.core.common.AppError
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.common.IoDispatcher
import com.example.lifet.core.common.runCatchingApp
import com.example.lifet.core.database.nutrition.NutritionMealDao
import com.example.lifet.core.database.nutrition.NutritionWaterDao
import com.example.lifet.core.model.nutrition.MealEntry
import com.example.lifet.core.model.nutrition.WaterEntry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

internal class OfflineNutritionLogRepository
    @Inject
    constructor(
        private val mealDao: NutritionMealDao,
        private val waterDao: NutritionWaterDao,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : NutritionLogRepository {
        override fun observeMeals(date: LocalDate): Flow<List<MealEntry>> =
            mealDao.observeByDate(date).map { rows -> rows.map { it.toDomain() } }.flowOn(ioDispatcher)

        override fun observeMealsBetween(
            start: LocalDate,
            endExclusive: LocalDate,
        ): Flow<List<MealEntry>> =
            mealDao
                .observeBetween(start, endExclusive)
                .map { rows -> rows.map { it.toDomain() } }
                .flowOn(ioDispatcher)

        override fun observeWater(date: LocalDate): Flow<List<WaterEntry>> =
            waterDao.observeByDate(date).map { rows -> rows.map { it.toDomain() } }.flowOn(ioDispatcher)

        override fun observeWaterBetween(
            start: LocalDate,
            endExclusive: LocalDate,
        ): Flow<List<WaterEntry>> =
            waterDao
                .observeBetween(start, endExclusive)
                .map { rows -> rows.map { it.toDomain() } }
                .flowOn(ioDispatcher)

        override suspend fun getMeals(date: LocalDate): AppResult<List<MealEntry>> =
            io { mealDao.getByDate(date).map { it.toDomain() } }

        override suspend fun saveMeals(entries: List<MealEntry>): AppResult<Unit> =
            if (entries.any { it.grams !in MealEntry.MIN_GRAMS..MealEntry.MAX_GRAMS }) {
                AppResult.Failure(AppError.Validation(INVALID_GRAMS))
            } else {
                io { mealDao.upsertAll(entries.map { it.toEntity() }) }
            }

        override suspend fun deleteMeal(id: String): AppResult<Unit> = io { mealDao.deleteById(id) }

        override suspend fun saveWater(entries: List<WaterEntry>): AppResult<Unit> =
            if (entries.any { it.ml <= 0 }) {
                AppResult.Failure(AppError.Validation(INVALID_WATER))
            } else {
                io { waterDao.upsertAll(entries.map { it.toEntity() }) }
            }

        override suspend fun deleteWater(id: String): AppResult<Unit> = io { waterDao.deleteById(id) }

        override suspend fun clearAll(): AppResult<Unit> =
            io {
                mealDao.deleteAll()
                waterDao.deleteAll()
            }

        private suspend fun <T> io(block: suspend () -> T): AppResult<T> =
            withContext(ioDispatcher) { runCatchingApp(onError = { AppError.Storage }) { block() } }

        companion object {
            const val INVALID_GRAMS = "grams_out_of_range"
            const val INVALID_WATER = "water_not_positive"
        }
    }

package com.example.lifet.core.data.nutrition

import com.example.lifet.core.common.AppError
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.common.IoDispatcher
import com.example.lifet.core.common.runCatchingApp
import com.example.lifet.core.database.nutrition.NutritionFoodDao
import com.example.lifet.core.database.nutrition.NutritionFoodSeeder
import com.example.lifet.core.database.nutrition.NutritionMealDao
import com.example.lifet.core.model.nutrition.FoodItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class OfflineFoodRepository
    @Inject
    constructor(
        private val foodDao: NutritionFoodDao,
        private val mealDao: NutritionMealDao,
        private val seeder: NutritionFoodSeeder,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : FoodRepository {
        override fun searchFoods(query: String): Flow<List<FoodItem>> =
            foodDao
                .observeSearch(query.trim())
                .seeded()
                .map { rows -> rows.map { it.toDomain() } }
                .flowOn(ioDispatcher)

        override fun observeFood(id: String): Flow<FoodItem?> =
            foodDao
                .observeById(id)
                .seeded()
                .map { it?.toDomain() }
                .flowOn(ioDispatcher)

        // Se cruza con todos los alimentos para mantener el orden de uso y descartar ids de alimentos borrados.
        override fun observeRecentFoods(limit: Int): Flow<List<FoodItem>> =
            combine(mealDao.observeRecentFoodIds(limit), foodDao.observeSearch("").seeded()) { ids, foods ->
                val byId = foods.associateBy { it.id }
                ids.mapNotNull { byId[it]?.toDomain() }
            }.flowOn(ioDispatcher)

        override fun observeFavoriteFoods(): Flow<List<FoodItem>> =
            foodDao
                .observeFavorites()
                .seeded()
                .map { rows -> rows.map { it.toDomain() } }
                .flowOn(ioDispatcher)

        override suspend fun setFavorite(
            id: String,
            favorite: Boolean,
        ): AppResult<Unit> = io { foodDao.setFavorite(id, favorite) }

        override suspend fun saveCustomFood(food: FoodItem): AppResult<Unit> =
            if (food.name.isBlank() || food.servingGrams <= 0 || food.per100g.kcal < 0.0) {
                AppResult.Failure(AppError.Validation(INVALID_FOOD))
            } else {
                io { foodDao.upsert(food.copy(name = food.name.trim(), isCustom = true).toEntity()) }
            }

        override suspend fun deleteCustomFood(id: String): AppResult<Unit> = io { foodDao.deleteCustomById(id) }

        private fun <T> Flow<T>.seeded(): Flow<T> = onStart { seeder.ensureSeeded() }

        private suspend fun <T> io(block: suspend () -> T): AppResult<T> =
            withContext(ioDispatcher) { runCatchingApp(onError = { AppError.Storage }) { block() } }

        companion object {
            const val INVALID_FOOD = "invalid_food"
        }
    }

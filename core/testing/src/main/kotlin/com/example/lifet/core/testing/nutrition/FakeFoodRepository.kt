package com.example.lifet.core.testing.nutrition

import com.example.lifet.core.common.AppError
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.data.nutrition.FoodRepository
import com.example.lifet.core.model.nutrition.FoodItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Base de alimentos en memoria. [recentIds] simula los alimentos usados recientemente, en orden. */
public class FakeFoodRepository(
    initial: List<FoodItem> = NutritionTestData.foods,
) : FoodRepository {
    public val foods: MutableStateFlow<List<FoodItem>> = MutableStateFlow(initial)
    public val recentIds: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())
    public var failWrites: Boolean = false

    override fun searchFoods(query: String): Flow<List<FoodItem>> =
        foods.map { all ->
            all
                .filter { it.name.contains(query.trim(), ignoreCase = true) }
                .sortedWith(compareByDescending<FoodItem> { it.isFavorite }.thenBy { it.name.lowercase() })
        }

    override fun observeFood(id: String): Flow<FoodItem?> = foods.map { all -> all.firstOrNull { it.id == id } }

    override fun observeRecentFoods(limit: Int): Flow<List<FoodItem>> =
        kotlinx.coroutines.flow.combine(recentIds, foods) { ids, all ->
            val byId = all.associateBy { it.id }
            ids.mapNotNull { byId[it] }.take(limit)
        }

    override fun observeFavoriteFoods(): Flow<List<FoodItem>> = foods.map { all -> all.filter { it.isFavorite } }

    override suspend fun setFavorite(
        id: String,
        favorite: Boolean,
    ): AppResult<Unit> =
        write {
            foods.value =
                foods.value.map { if (it.id == id) it.copy(isFavorite = favorite) else it }
        }

    override suspend fun saveCustomFood(food: FoodItem): AppResult<Unit> =
        if (food.name.isBlank()) {
            AppResult.Failure(AppError.Validation("invalid_food"))
        } else {
            write { foods.value = foods.value.filterNot { it.id == food.id } + food.copy(isCustom = true) }
        }

    override suspend fun deleteCustomFood(id: String): AppResult<Unit> =
        write { foods.value = foods.value.filterNot { it.id == id && it.isCustom } }

    override suspend fun clearCustomFoodsAndFavorites(): AppResult<Unit> =
        write { foods.value = foods.value.filterNot { it.isCustom }.map { it.copy(isFavorite = false) } }

    private inline fun write(block: () -> Unit): AppResult<Unit> =
        if (failWrites) AppResult.Failure(AppError.Storage) else AppResult.Success(block())
}

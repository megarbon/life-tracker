package com.example.lifet.core.demo.nutrition

import com.example.lifet.core.common.AppResult
import com.example.lifet.core.data.nutrition.FoodRepository
import com.example.lifet.core.data.nutrition.NutritionGoalsRepository
import com.example.lifet.core.data.nutrition.NutritionLogRepository
import com.example.lifet.core.model.nutrition.FoodItem
import com.example.lifet.core.model.nutrition.MealEntry
import com.example.lifet.core.model.nutrition.NutritionGoals
import com.example.lifet.core.model.nutrition.WaterEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class FakeLog : NutritionLogRepository {
    val meals = MutableStateFlow<List<MealEntry>>(emptyList())
    val water = MutableStateFlow<List<WaterEntry>>(emptyList())

    override fun observeMeals(date: LocalDate): Flow<List<MealEntry>> =
        meals.map { all -> all.filter { it.date == date } }

    override fun observeMealsBetween(
        start: LocalDate,
        endExclusive: LocalDate,
    ): Flow<List<MealEntry>> = meals

    override fun observeWater(date: LocalDate): Flow<List<WaterEntry>> =
        water.map { all -> all.filter { it.date == date } }

    override fun observeWaterBetween(
        start: LocalDate,
        endExclusive: LocalDate,
    ): Flow<List<WaterEntry>> = water

    override suspend fun getMeals(date: LocalDate): AppResult<List<MealEntry>> =
        AppResult.Success(meals.value.filter { it.date == date })

    override suspend fun saveMeals(entries: List<MealEntry>): AppResult<Unit> {
        meals.value = meals.value + entries
        return AppResult.Success(Unit)
    }

    override suspend fun deleteMeal(id: String): AppResult<Unit> = AppResult.Success(Unit)

    override suspend fun saveWater(entries: List<WaterEntry>): AppResult<Unit> {
        water.value = water.value + entries
        return AppResult.Success(Unit)
    }

    override suspend fun deleteWater(id: String): AppResult<Unit> = AppResult.Success(Unit)

    override suspend fun clearAll(): AppResult<Unit> {
        meals.value = emptyList()
        water.value = emptyList()
        return AppResult.Success(Unit)
    }
}

class FakeGoals : NutritionGoalsRepository {
    val goals = MutableStateFlow(NutritionGoals.DEFAULT)

    override fun observeGoals(): Flow<NutritionGoals> = goals

    override suspend fun setGoals(goals: NutritionGoals): AppResult<Unit> {
        this.goals.value = goals
        return AppResult.Success(Unit)
    }

    override suspend fun clearGoals(): AppResult<Unit> {
        goals.value = NutritionGoals.DEFAULT
        return AppResult.Success(Unit)
    }
}

/** Parte con la semilla real cargada: así se comprueba que "borrar" no la toca. */
class FakeFoods(
    seed: Collection<FoodItem> = SeedCatalog.foods.values,
) : FoodRepository {
    val items = MutableStateFlow(seed.toList())

    override fun searchFoods(query: String): Flow<List<FoodItem>> =
        items.map { all -> all.filter { it.name.contains(query, ignoreCase = true) } }

    override fun observeFood(id: String): Flow<FoodItem?> = items.map { all -> all.firstOrNull { it.id == id } }

    override fun observeRecentFoods(limit: Int): Flow<List<FoodItem>> = items.map { it.take(limit) }

    override fun observeFavoriteFoods(): Flow<List<FoodItem>> = items.map { all -> all.filter { it.isFavorite } }

    override suspend fun setFavorite(
        id: String,
        favorite: Boolean,
    ): AppResult<Unit> {
        items.value = items.value.map { if (it.id == id) it.copy(isFavorite = favorite) else it }
        return AppResult.Success(Unit)
    }

    override suspend fun saveCustomFood(food: FoodItem): AppResult<Unit> {
        items.value = items.value.filterNot { it.id == food.id } + food.copy(isCustom = true)
        return AppResult.Success(Unit)
    }

    override suspend fun deleteCustomFood(id: String): AppResult<Unit> = AppResult.Success(Unit)

    override suspend fun clearCustomFoodsAndFavorites(): AppResult<Unit> {
        items.value = items.value.filterNot { it.isCustom }.map { it.copy(isFavorite = false) }
        return AppResult.Success(Unit)
    }
}

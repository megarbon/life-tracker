package com.example.lifet.core.domain.nutrition

import com.example.lifet.core.common.AppResult
import com.example.lifet.core.data.nutrition.NutritionGoalsRepository
import com.example.lifet.core.data.nutrition.NutritionLogRepository
import com.example.lifet.core.model.nutrition.MealEntry
import com.example.lifet.core.model.nutrition.NutritionGoals
import com.example.lifet.core.model.nutrition.WaterEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class FakeLogRepository : NutritionLogRepository {
    val meals = MutableStateFlow<List<MealEntry>>(emptyList())
    val water = MutableStateFlow<List<WaterEntry>>(emptyList())

    override fun observeMeals(date: LocalDate): Flow<List<MealEntry>> =
        meals.map { all -> all.filter { it.date == date } }

    override fun observeMealsBetween(
        start: LocalDate,
        endExclusive: LocalDate,
    ): Flow<List<MealEntry>> = meals.map { all -> all.filter { it.date >= start && it.date < endExclusive } }

    override fun observeWater(date: LocalDate): Flow<List<WaterEntry>> =
        water.map { all -> all.filter { it.date == date } }

    override fun observeWaterBetween(
        start: LocalDate,
        endExclusive: LocalDate,
    ): Flow<List<WaterEntry>> = water.map { all -> all.filter { it.date >= start && it.date < endExclusive } }

    override suspend fun getMeals(date: LocalDate): AppResult<List<MealEntry>> =
        AppResult.Success(meals.value.filter { it.date == date })

    override suspend fun saveMeals(entries: List<MealEntry>): AppResult<Unit> {
        meals.value = meals.value.filterNot { old -> entries.any { it.id == old.id } } + entries
        return AppResult.Success(Unit)
    }

    override suspend fun deleteMeal(id: String): AppResult<Unit> {
        meals.value = meals.value.filterNot { it.id == id }
        return AppResult.Success(Unit)
    }

    override suspend fun saveWater(entries: List<WaterEntry>): AppResult<Unit> {
        water.value = water.value + entries
        return AppResult.Success(Unit)
    }

    override suspend fun deleteWater(id: String): AppResult<Unit> {
        water.value = water.value.filterNot { it.id == id }
        return AppResult.Success(Unit)
    }

    override suspend fun clearAll(): AppResult<Unit> {
        meals.value = emptyList()
        water.value = emptyList()
        return AppResult.Success(Unit)
    }
}

class FakeGoalsRepository(
    initial: NutritionGoals = NutritionGoals.DEFAULT,
) : NutritionGoalsRepository {
    val goals = MutableStateFlow(initial)

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

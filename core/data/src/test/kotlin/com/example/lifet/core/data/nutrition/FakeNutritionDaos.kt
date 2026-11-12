package com.example.lifet.core.data.nutrition

import com.example.lifet.core.database.nutrition.NutritionFoodDao
import com.example.lifet.core.database.nutrition.NutritionFoodEntity
import com.example.lifet.core.database.nutrition.NutritionGoalsDao
import com.example.lifet.core.database.nutrition.NutritionGoalsEntity
import com.example.lifet.core.database.nutrition.NutritionMealDao
import com.example.lifet.core.database.nutrition.NutritionMealEntity
import com.example.lifet.core.database.nutrition.NutritionWaterDao
import com.example.lifet.core.database.nutrition.NutritionWaterEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class FakeMealDao : NutritionMealDao {
    val rows = MutableStateFlow<List<NutritionMealEntity>>(emptyList())
    var failOnWrite = false

    override fun observeByDate(date: LocalDate): Flow<List<NutritionMealEntity>> =
        rows.map { all -> all.filter { it.date == date }.sortedBy { it.loggedAt } }

    override fun observeBetween(
        start: LocalDate,
        endExclusive: LocalDate,
    ): Flow<List<NutritionMealEntity>> = rows.map { all -> all.filter { it.date >= start && it.date < endExclusive } }

    override suspend fun getByDate(date: LocalDate): List<NutritionMealEntity> = rows.value.filter { it.date == date }

    override fun observeRecentFoodIds(limit: Int): Flow<List<String>> =
        rows.map { all ->
            all
                .groupBy { it.foodId }
                .entries
                .sortedByDescending { entry -> entry.value.maxOf { it.loggedAt } }
                .map { it.key }
                .take(limit)
        }

    override suspend fun upsert(entry: NutritionMealEntity) = upsertAll(listOf(entry))

    override suspend fun upsertAll(entries: List<NutritionMealEntity>) {
        check(!failOnWrite) { "disk full" }
        rows.value = rows.value.filterNot { old -> entries.any { it.id == old.id } } + entries
    }

    override suspend fun deleteById(id: String) {
        rows.value = rows.value.filterNot { it.id == id }
    }

    override suspend fun deleteAll() {
        rows.value = emptyList()
    }
}

class FakeWaterDao : NutritionWaterDao {
    val rows = MutableStateFlow<List<NutritionWaterEntity>>(emptyList())

    override fun observeByDate(date: LocalDate): Flow<List<NutritionWaterEntity>> =
        rows.map { all -> all.filter { it.date == date } }

    override fun observeBetween(
        start: LocalDate,
        endExclusive: LocalDate,
    ): Flow<List<NutritionWaterEntity>> = rows.map { all -> all.filter { it.date >= start && it.date < endExclusive } }

    override suspend fun getByDate(date: LocalDate): List<NutritionWaterEntity> = rows.value.filter { it.date == date }

    override suspend fun upsert(entry: NutritionWaterEntity) = upsertAll(listOf(entry))

    override suspend fun upsertAll(entries: List<NutritionWaterEntity>) {
        rows.value = rows.value.filterNot { old -> entries.any { it.id == old.id } } + entries
    }

    override suspend fun deleteById(id: String) {
        rows.value = rows.value.filterNot { it.id == id }
    }

    override suspend fun deleteAll() {
        rows.value = emptyList()
    }
}

class FakeGoalsDao : NutritionGoalsDao {
    val row = MutableStateFlow<NutritionGoalsEntity?>(null)

    override fun observe(): Flow<NutritionGoalsEntity?> = row

    override suspend fun upsert(goals: NutritionGoalsEntity) {
        row.value = goals
    }

    override suspend fun deleteAll() {
        row.value = null
    }
}

class FakeFoodDao : NutritionFoodDao {
    val rows = MutableStateFlow<List<NutritionFoodEntity>>(emptyList())

    override suspend fun getAll(): List<NutritionFoodEntity> = rows.value

    override fun observeSearch(query: String): Flow<List<NutritionFoodEntity>> =
        rows.map { all ->
            all
                .filter { it.name.contains(query, ignoreCase = true) }
                .sortedWith(compareByDescending<NutritionFoodEntity> { it.isFavorite }.thenBy { it.name.lowercase() })
        }

    override fun observeById(id: String): Flow<NutritionFoodEntity?> =
        rows.map { all -> all.firstOrNull { it.id == id } }

    override suspend fun getById(id: String): NutritionFoodEntity? = rows.value.firstOrNull { it.id == id }

    override fun observeFavorites(): Flow<List<NutritionFoodEntity>> = rows.map { all -> all.filter { it.isFavorite } }

    override suspend fun upsert(food: NutritionFoodEntity) {
        rows.value = rows.value.filterNot { it.id == food.id } + food
    }

    override suspend fun insertSeedIgnoring(foods: List<NutritionFoodEntity>) {
        val existing = rows.value.map { it.id }.toSet()
        rows.value = rows.value + foods.filter { it.id !in existing }
    }

    override suspend fun setFavorite(
        id: String,
        favorite: Boolean,
    ) {
        rows.value = rows.value.map { if (it.id == id) it.copy(isFavorite = favorite) else it }
    }

    override suspend fun deleteCustomById(id: String) {
        rows.value = rows.value.filterNot { it.id == id && it.isCustom }
    }

    override suspend fun deleteAllCustom() {
        rows.value = rows.value.filterNot { it.isCustom }
    }
}

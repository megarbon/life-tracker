package com.example.lifet.core.data.nutrition

import app.cash.turbine.test
import com.example.lifet.core.common.AppError
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.database.nutrition.NutritionFoodEntity
import com.example.lifet.core.database.nutrition.NutritionFoodSeeder
import com.example.lifet.core.model.nutrition.FoodCategory
import com.example.lifet.core.model.nutrition.FoodItem
import com.example.lifet.core.model.nutrition.MealEntry
import com.example.lifet.core.model.nutrition.MealType
import com.example.lifet.core.model.nutrition.Nutrients
import com.example.lifet.core.model.nutrition.NutritionGoals
import com.example.lifet.core.model.nutrition.WaterEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class NutritionRepositoriesTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val zone = ZoneId.of("Europe/Madrid")
    private val today = LocalDate.of(2026, 3, 10)

    private val mealDao = FakeMealDao()
    private val waterDao = FakeWaterDao()
    private val goalsDao = FakeGoalsDao()
    private val foodDao = FakeFoodDao()
    private val seeder = CountingSeeder(foodDao)

    private val log = OfflineNutritionLogRepository(mealDao, waterDao, dispatcher)
    private val goals = OfflineNutritionGoalsRepository(goalsDao, dispatcher)
    private val foods = OfflineFoodRepository(foodDao, mealDao, seeder, dispatcher)

    @Test
    fun `saved meals are observed by day and mapped back to the domain`() =
        runTest(dispatcher) {
            val entry = meal("m1", grams = 150)

            assertEquals(AppResult.Success(Unit), log.saveMeals(listOf(entry)))

            log.observeMeals(today).test {
                assertEquals(listOf(entry), awaitItem())
            }
            log.observeMeals(today.plusDays(1)).test {
                assertTrue(awaitItem().isEmpty())
            }
        }

    @Test
    fun `out of range grams are rejected before touching storage`() =
        runTest(dispatcher) {
            val result = log.saveMeals(listOf(meal("m1", grams = 0)))

            assertEquals(AppResult.Failure(AppError.Validation(OfflineNutritionLogRepository.INVALID_GRAMS)), result)
            assertTrue(mealDao.rows.value.isEmpty())
        }

    @Test
    fun `storage failures become a typed Storage error`() =
        runTest(dispatcher) {
            mealDao.failOnWrite = true

            assertEquals(AppResult.Failure(AppError.Storage), log.saveMeals(listOf(meal("m1", grams = 100))))
        }

    @Test
    fun `water must be positive and can be deleted`() =
        runTest(dispatcher) {
            val glass = WaterEntry("w1", today, WaterEntry.GLASS_ML, NOW, zone)

            assertTrue(log.saveWater(glass.copy(ml = 0)) is AppResult.Failure)
            log.saveWater(glass)
            log.observeWater(today).test {
                assertEquals(listOf(glass), awaitItem())
                log.deleteWater("w1")
                assertTrue(awaitItem().isEmpty())
            }
        }

    @Test
    fun `goals default until the user sets valid ones`() =
        runTest(dispatcher) {
            goals.observeGoals().test {
                assertEquals(NutritionGoals.DEFAULT, awaitItem())
                val custom = NutritionGoals.DEFAULT.copy(kcal = 2_400)
                goals.setGoals(custom)
                assertEquals(custom, awaitItem())
            }
            val invalid = goals.setGoals(NutritionGoals.DEFAULT.copy(kcal = 1))
            assertEquals(
                AppResult.Failure(AppError.Validation(OfflineNutritionGoalsRepository.GOALS_OUT_OF_RANGE)),
                invalid,
            )
        }

    @Test
    fun `observing foods seeds the base first`() =
        runTest(dispatcher) {
            foods.searchFoods("manz").test {
                assertEquals(listOf("seed_manzana"), awaitItem().map { it.id })
            }
            assertTrue(seeder.calls >= 1)
        }

    @Test
    fun `recent foods follow the order of use and skip deleted foods`() =
        runTest(dispatcher) {
            foodDao.upsert(seedRow("seed_pan", "Pan"))
            log.saveMeals(
                listOf(
                    meal("1", foodId = "seed_pan", hour = 8),
                    meal("2", foodId = "seed_manzana", hour = 12),
                    meal("3", foodId = "borrado", hour = 13),
                ),
            )

            foods.observeRecentFoods(limit = 5).test {
                assertEquals(listOf("seed_manzana", "seed_pan"), awaitItem().map { it.id })
            }
        }

    @Test
    fun `custom foods are forced custom, trimmed and validated`() =
        runTest(dispatcher) {
            val custom =
                FoodItem(
                    id = "c1",
                    name = "  Bizcocho de la abuela  ",
                    category = FoodCategory.SWEETS_SNACKS,
                    per100g = Nutrients(380.0, 6.0, 50.0, 17.0),
                    servingGrams = 60,
                    isCustom = false,
                )

            assertEquals(AppResult.Success(Unit), foods.saveCustomFood(custom))
            assertTrue(foods.saveCustomFood(custom.copy(name = " ")) is AppResult.Failure)

            val saved = foodDao.getById("c1")
            assertEquals("Bizcocho de la abuela", saved?.name)
            assertEquals(true, saved?.isCustom)
        }

    @Test
    fun `favorites can be toggled`() =
        runTest(dispatcher) {
            foods.observeFavoriteFoods().test {
                assertTrue(awaitItem().isEmpty())
                foods.setFavorite("seed_manzana", true)
                assertEquals(listOf("seed_manzana"), awaitItem().map { it.id })
            }
        }

    private fun meal(
        id: String,
        grams: Int = 100,
        foodId: String = "seed_manzana",
        hour: Int = 9,
    ) = MealEntry(
        id = id,
        date = today,
        mealType = MealType.BREAKFAST,
        foodId = foodId,
        foodName = foodId,
        grams = grams,
        nutrients = Nutrients(52.0, 0.3, 14.0, 0.2) * (grams / 100.0),
        loggedAt = today.atTime(hour, 0).atZone(zone).toInstant(),
        zone = zone,
    )

    /** Simula la semilla real con un único alimento y cuenta cuántas veces se invoca. */
    private class CountingSeeder(
        private val dao: FakeFoodDao,
    ) : NutritionFoodSeeder {
        var calls = 0

        override suspend fun ensureSeeded() {
            calls++
            dao.insertSeedIgnoring(listOf(seedRow("seed_manzana", "Manzana")))
        }
    }

    private companion object {
        val NOW: Instant = Instant.parse("2026-03-10T09:00:00Z")

        fun seedRow(
            id: String,
            name: String,
        ) = NutritionFoodEntity(
            id = id,
            name = name,
            category = "FRUIT",
            kcalPer100g = 52.0,
            proteinPer100g = 0.3,
            carbsPer100g = 14.0,
            fatPer100g = 0.2,
            fiberPer100g = 2.4,
            sugarPer100g = 10.4,
            servingGrams = 180,
            isCustom = false,
            isFavorite = false,
        )
    }
}

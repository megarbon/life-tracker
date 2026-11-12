package com.example.lifet.core.database.nutrition

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.example.lifet.core.database.LifeTrackerDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class NutritionDaoTest {
    private lateinit var context: Context
    private lateinit var database: LifeTrackerDatabase
    private val zone = ZoneId.of("Europe/Madrid")
    private val monday = LocalDate.of(2026, 3, 9)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database =
            Room.inMemoryDatabaseBuilder(context, LifeTrackerDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `seeder loads the bundled foods and is idempotent`() =
        runTest {
            val dao = database.nutritionFoodDao()
            val seeder = NutritionFoodSeeder(context, dao, Dispatchers.Unconfined)

            seeder.ensureSeeded()
            val first = dao.getAll().size
            // Un segundo seeder (otro proceso) vuelve a intentar: INSERT OR IGNORE no duplica.
            NutritionFoodSeeder(context, dao, Dispatchers.Unconfined).ensureSeeded()

            assertTrue("la semilla debe tener ~150 alimentos, tiene $first", first >= MIN_SEED_FOODS)
            assertEquals(first, dao.getAll().size)
        }

    @Test
    fun `reseeding keeps favorites chosen by the user`() =
        runTest {
            val dao = database.nutritionFoodDao()
            NutritionFoodSeeder(context, dao, Dispatchers.Unconfined).ensureSeeded()
            dao.setFavorite("seed_manzana", true)

            NutritionFoodSeeder(context, dao, Dispatchers.Unconfined).ensureSeeded()

            assertTrue(dao.getById("seed_manzana")?.isFavorite == true)
        }

    @Test
    fun `seed foods cannot be deleted but custom ones can`() =
        runTest {
            val dao = database.nutritionFoodDao()
            NutritionFoodSeeder(context, dao, Dispatchers.Unconfined).ensureSeeded()
            dao.upsert(customFood("custom_1"))

            dao.deleteCustomById("seed_manzana")
            dao.deleteCustomById("custom_1")

            assertNotNull(dao.getById("seed_manzana"))
            assertNull(dao.getById("custom_1"))
        }

    @Test
    fun `search is case insensitive and puts favorites first`() =
        runTest {
            val dao = database.nutritionFoodDao()
            dao.upsert(customFood("a", "Pan casero"))
            dao.upsert(customFood("b", "Pan de molde").copy(isFavorite = true))

            dao.observeSearch("PAN").test {
                assertEquals(listOf("b", "a"), awaitItem().map { it.id })
            }
        }

    @Test
    fun `meals are filtered by day and recent foods are distinct and newest first`() =
        runTest {
            val dao = database.nutritionMealDao()
            dao.upsertAll(
                listOf(
                    meal("1", monday, "pan", hour = 8),
                    meal("2", monday, "huevo", hour = 9),
                    meal("3", monday.plusDays(1), "pan", hour = 8),
                ),
            )

            dao.observeByDate(monday).test {
                assertEquals(listOf("1", "2"), awaitItem().map { it.id })
            }
            dao.observeRecentFoodIds(limit = 10).test {
                // "pan" se usó el martes, después que "huevo".
                assertEquals(listOf("pan", "huevo"), awaitItem())
            }
            dao.observeBetween(monday, monday.plusDays(1)).test {
                assertEquals(2, awaitItem().size)
            }
        }

    @Test
    fun `water entries are stored and deleted individually`() =
        runTest {
            val dao = database.nutritionWaterDao()
            dao.upsertAll(listOf(water("w1", 250), water("w2", 250)))

            dao.deleteById("w1")

            assertEquals(listOf("w2"), dao.getByDate(monday).map { it.id })
        }

    @Test
    fun `goals row is a singleton that upsert replaces`() =
        runTest {
            val dao = database.nutritionGoalsDao()
            dao.observe().test {
                assertNull(awaitItem())
                dao.upsert(NutritionGoalsEntity(kcal = 2_000, proteinG = 100, carbsG = 250, fatG = 65, waterMl = 2_000))
                assertEquals(2_000, awaitItem()?.kcal)
                dao.upsert(NutritionGoalsEntity(kcal = 2_300, proteinG = 120, carbsG = 270, fatG = 70, waterMl = 2_500))
                assertEquals(2_300, awaitItem()?.kcal)
            }
        }

    private fun customFood(
        id: String,
        name: String = "Alimento $id",
    ) = NutritionFoodEntity(
        id = id,
        name = name,
        category = "OTHER",
        kcalPer100g = 100.0,
        proteinPer100g = 5.0,
        carbsPer100g = 10.0,
        fatPer100g = 3.0,
        fiberPer100g = 0.0,
        sugarPer100g = 0.0,
        servingGrams = 100,
        isCustom = true,
        isFavorite = false,
    )

    private fun meal(
        id: String,
        date: LocalDate,
        foodId: String,
        hour: Int,
    ) = NutritionMealEntity(
        id = id,
        date = date,
        mealType = "BREAKFAST",
        foodId = foodId,
        foodName = foodId,
        grams = 100,
        kcal = 100.0,
        proteinG = 5.0,
        carbsG = 10.0,
        fatG = 3.0,
        fiberG = 0.0,
        sugarG = 0.0,
        loggedAt = date.atTime(hour, 0).atZone(zone).toInstant(),
        zone = zone,
    )

    private fun water(
        id: String,
        ml: Int,
    ) = NutritionWaterEntity(id, monday, ml, Instant.parse("2026-03-09T10:00:00Z"), zone)

    private companion object {
        const val MIN_SEED_FOODS = 150
    }
}

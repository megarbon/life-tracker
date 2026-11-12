package com.example.lifet.core.demo.nutrition

import com.example.lifet.core.demo.DemoContext
import com.example.lifet.core.demo.DemoScenario
import com.example.lifet.core.model.Area
import com.example.lifet.core.model.nutrition.NutritionGoals
import com.example.lifet.core.testing.TestClock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionDemoDataGeneratorTest {
    private val log = FakeLog()
    private val goals = FakeGoals()
    private val foods = FakeFoods()
    private val generator = NutritionDemoDataGenerator(log, goals, foods)
    private val seedSize = SeedCatalog.foods.size

    private fun context() = DemoContext(TestClock.fixed("2026-03-10T09:00:00Z"))

    @Test
    fun `it belongs to the nutrition area`() {
        assertEquals(Area.NUTRITION, generator.area)
    }

    @Test
    fun `empty scenario leaves the diary empty but keeps the whole food seed`() =
        runTest {
            generator.seed(DemoScenario.REGULAR, context())

            generator.seed(DemoScenario.EMPTY, context())

            assertTrue(log.meals.value.isEmpty())
            assertTrue(log.water.value.isEmpty())
            assertEquals(NutritionGoals.DEFAULT, goals.goals.value)
            assertEquals(seedSize, foods.items.value.size)
            assertTrue(foods.items.value.none { it.isCustom || it.isFavorite })
        }

    @Test
    fun `regular scenario persists diary water goals custom foods and favorites`() =
        runTest {
            generator.seed(DemoScenario.REGULAR, context())

            assertTrue(log.meals.value.size > 1_000)
            assertTrue(log.water.value.isNotEmpty())
            assertEquals(DEMO_REGULAR_GOALS, goals.goals.value)
            assertEquals(seedSize + DEMO_CUSTOM_FOODS.size, foods.items.value.size)
            val favorites =
                foods.items.value
                    .filter { it.isFavorite }
                    .map { it.id }
                    .toSet()
            assertEquals(NutritionDemoMenus.favoriteIds.toSet(), favorites)
        }

    @Test
    fun `seeding twice does not stack data`() =
        runTest {
            generator.seed(DemoScenario.REGULAR, context())
            val first = log.meals.value.size

            generator.seed(DemoScenario.REGULAR, context())

            assertEquals(first, log.meals.value.size)
            assertEquals(seedSize + DEMO_CUSTOM_FOODS.size, foods.items.value.size)
        }

    @Test
    fun `clear removes demo data and never the seed`() =
        runTest {
            generator.seed(DemoScenario.EDGE_CASES, context())

            generator.clear()

            assertTrue(log.meals.value.isEmpty())
            assertEquals(seedSize, foods.items.value.size)
        }

    @Test
    fun `edge cases persist their custom foods with emoji and very long names`() =
        runTest {
            generator.seed(DemoScenario.EDGE_CASES, context())

            val names =
                foods.items.value
                    .filter { it.isCustom }
                    .map { it.name }
            assertTrue(names.any { it.contains("🌮") })
            assertTrue(names.any { it.length > 200 })
        }

    @Test
    fun `new user gets three days of diary`() =
        runTest {
            generator.seed(DemoScenario.NEW_USER, context())

            assertEquals(
                3,
                log.meals.value
                    .map { it.date }
                    .toSet()
                    .size,
            )
        }
}

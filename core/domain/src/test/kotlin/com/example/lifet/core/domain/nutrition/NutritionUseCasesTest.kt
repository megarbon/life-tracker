package com.example.lifet.core.domain.nutrition

import app.cash.turbine.test
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.domain.GetTodayUseCase
import com.example.lifet.core.model.DateRange
import com.example.lifet.core.model.DateRangeUnit
import com.example.lifet.core.model.nutrition.FoodCategory
import com.example.lifet.core.model.nutrition.FoodItem
import com.example.lifet.core.model.nutrition.MealEntry
import com.example.lifet.core.model.nutrition.MealType
import com.example.lifet.core.model.nutrition.Nutrients
import com.example.lifet.core.model.nutrition.NutritionGoals
import com.example.lifet.core.testing.TestClock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class NutritionUseCasesTest {
    // 2026-03-10 09:00 UTC = 10:00 en Madrid.
    private val clock = TestClock.fixed("2026-03-10T09:00:00Z")
    private val today = LocalDate.of(2026, 3, 10)
    private val monday = LocalDate.of(2026, 3, 9)
    private val log = FakeLogRepository()
    private val goals = FakeGoalsRepository()

    private val bread =
        FoodItem(
            id = "seed_pan",
            name = "Pan",
            category = FoodCategory.GRAIN,
            per100g = Nutrients(kcal = 265.0, proteinG = 9.0, carbsG = 49.0, fatG = 3.2),
            servingGrams = 40,
        )

    @Test
    fun `logging food stores the scaled nutrients at the clock time`() =
        runTest {
            val result = LogFood(log, clock)(bread, grams = 80, mealType = MealType.BREAKFAST, date = today)

            val entry = (result as AppResult.Success).value
            assertEquals(212.0, entry.nutrients.kcal, EPS)
            assertEquals(today, entry.date)
            assertEquals("Pan", entry.foodName)
            assertEquals(10, entry.loggedAt.atZone(clock.zone).hour)
            assertEquals(listOf(entry), log.meals.value)
        }

    @Test
    fun `adding water defaults to a 250 ml glass`() =
        runTest {
            val entry = (AddWater(log, clock)(today) as AppResult.Success).value

            assertEquals(250, entry.ml)
            assertEquals(listOf(entry), log.water.value)
        }

    @Test
    fun `repeat yesterday copies meals with new ids keeping time and grams`() =
        runTest {
            val yesterday = today.minusDays(1)
            LogFood(log, clock)(bread, 60, MealType.BREAKFAST, yesterday)
            LogFood(log, clock)(bread, 100, MealType.DINNER, yesterday)

            val copies = (RepeatYesterdayMeals(log)(today) as AppResult.Success).value

            assertEquals(2, copies.size)
            assertTrue(copies.all { it.date == today })
            assertEquals(listOf(60, 100), copies.map { it.grams })
            assertEquals(listOf(MealType.BREAKFAST, MealType.DINNER), copies.map { it.mealType })
            assertEquals(4, log.meals.value.size)
            val originalIds =
                log.meals.value
                    .filter { it.date == yesterday }
                    .map { it.id }
                    .toSet()
            assertTrue(copies.none { it.id in originalIds })
        }

    @Test
    fun `repeat yesterday with nothing logged yesterday is an empty success`() =
        runTest {
            val result = RepeatYesterdayMeals(log)(today)

            assertEquals(AppResult.Success(emptyList<MealEntry>()), result)
            assertTrue(log.meals.value.isEmpty())
        }

    @Test
    fun `today summary combines meals water and goals and reacts to changes`() =
        runTest {
            val summary = ObserveNutritionTodaySummary(log, goals, GetTodayUseCase(clock))

            summary().test {
                val empty = awaitItem()
                assertEquals(0, empty.mealCount)
                assertEquals(NutritionGoals.DEFAULT, empty.goals)

                LogFood(log, clock)(bread, 100, MealType.LUNCH, today)
                AddWater(log, clock)(today)
                var latest = awaitItem()
                while (latest.waterMl == 0) latest = awaitItem()

                assertEquals(1, latest.mealCount)
                assertEquals(265.0, latest.consumed.kcal, EPS)
                assertEquals(250, latest.waterMl)
                assertEquals(1735, latest.remainingKcal)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `series has one entry per day and marks unlogged days`() =
        runTest {
            LogFood(log, clock)(bread, 100, MealType.LUNCH, monday)
            val week = DateRange.containing(DateRangeUnit.WEEK, today)

            ObserveNutritionSeries(log, goals)(week).test {
                val series = awaitItem()

                assertEquals(7, series.days.size)
                assertEquals(week.start, series.start)
                assertEquals(1, series.loggedDayCount)
                val logged = series.day(monday)
                assertTrue(logged?.hasMeals == true)
                assertFalse(series.day(today)?.hasMeals == true)
                assertEquals(265.0, series.averageKcal ?: -1.0, EPS)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `series accepts arbitrary windows such as the last 30 days`() =
        runTest {
            ObserveNutritionSeries(log, goals)(today.minusDays(29), today.plusDays(1)).test {
                assertEquals(30, awaitItem().days.size)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `day flow exposes the goals in force`() =
        runTest {
            goals.goals.value = NutritionGoals.DEFAULT.copy(kcal = 2_500)

            ObserveNutritionDay(log, goals)(today).test {
                val day = awaitItem()

                assertEquals(2_500, day.goals.kcal)
                assertNotEquals(NutritionGoals.DEFAULT, day.goals)
                cancelAndIgnoreRemainingEvents()
            }
        }

    private companion object {
        const val EPS = 1e-6
    }
}

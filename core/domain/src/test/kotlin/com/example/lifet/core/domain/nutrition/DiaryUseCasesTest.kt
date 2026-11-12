package com.example.lifet.core.domain.nutrition

import app.cash.turbine.test
import com.example.lifet.core.common.AppError
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.model.nutrition.MealType
import com.example.lifet.core.testing.nutrition.FakeNutritionGoalsRepository
import com.example.lifet.core.testing.nutrition.FakeNutritionLogRepository
import com.example.lifet.core.testing.nutrition.NutritionTestData
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DiaryUseCasesTest {
    private val today = LocalDate.of(2026, 3, 10)
    private val log = FakeNutritionLogRepository()
    private val goals = FakeNutritionGoalsRepository()
    private val manage = ManageDiaryEntries(log)

    @Test
    fun `diary combines the day with the 7 days that end on it`() =
        runTest {
            log.meals.value =
                listOf(
                    NutritionTestData.entry("a", NutritionTestData.bread, 100, today),
                    NutritionTestData.entry("b", NutritionTestData.bread, 100, today.minusDays(6)),
                    NutritionTestData.entry("c", NutritionTestData.bread, 100, today.minusDays(7)),
                )
            val diary = ObserveNutritionDiary(ObserveNutritionDay(log, goals), ObserveNutritionSeries(log, goals))

            diary(today).test {
                val value = awaitItem()
                assertEquals(1, value.day.meals.size)
                assertEquals(7, value.week.days.size)
                assertEquals(
                    today,
                    value.week.days
                        .last()
                        .date,
                )
                // "c" queda fuera de la ventana de 7 días.
                assertEquals(2, value.week.loggedDayCount)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `update rescales nutrients and can move the entry to another meal`() =
        runTest {
            val original = NutritionTestData.entry("a", NutritionTestData.bread, 100, today)
            log.meals.value = listOf(original)

            val result = manage.updateMeal(original, grams = 50, mealType = MealType.DINNER)
            val updated = (result as AppResult.Success).value

            assertEquals(132.5, updated.nutrients.kcal, 1e-6)
            assertEquals(MealType.DINNER, updated.mealType)
            assertEquals(listOf(updated), log.meals.value)
        }

    @Test
    fun `deleted entries come back intact on restore`() =
        runTest {
            val entries =
                listOf(
                    NutritionTestData.entry("a", NutritionTestData.bread, 100, today),
                    NutritionTestData.entry("b", NutritionTestData.apple, 180, today),
                )
            log.meals.value = entries

            manage.deleteMeals(entries)
            assertTrue(log.meals.value.isEmpty())
            manage.restoreMeals(entries)

            assertEquals(entries.toSet(), log.meals.value.toSet())
        }

    @Test
    fun `delete reports a storage failure`() =
        runTest {
            log.failWrites = true

            val result = manage.deleteMeals(listOf(NutritionTestData.entry("a", NutritionTestData.bread, 100, today)))

            assertEquals(AppResult.Failure(AppError.Storage), result)
        }
}

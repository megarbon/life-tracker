package com.example.lifet.core.model.nutrition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class NutritionModelTest {
    private val date = LocalDate.of(2026, 3, 10)
    private val zone = ZoneId.of("Europe/Madrid")
    private val now = Instant.parse("2026-03-10T08:00:00Z")

    private val oats = Nutrients(kcal = 372.0, proteinG = 13.5, carbsG = 59.0, fatG = 7.0)

    @Test
    fun `food nutrients scale with grams`() {
        val food =
            FoodItem(
                id = "avena",
                name = "Copos de avena",
                category = FoodCategory.GRAIN,
                per100g = oats,
                servingGrams = 40,
            )

        assertEquals(148.8, food.nutrientsFor(40).kcal, EPS)
        assertEquals(0.0, food.nutrientsFor(0).kcal, EPS)
    }

    @Test
    fun `macro split uses atwater factors`() {
        val split = Nutrients(kcal = 0.0, proteinG = 25.0, carbsG = 50.0, fatG = 10.0).macroSplit()

        // 100 kcal de proteína, 200 de hidratos y 90 de grasa: total 390.
        assertEquals(100.0 / 390.0, split.protein, EPS)
        assertEquals(200.0 / 390.0, split.carbs, EPS)
        assertEquals(90.0 / 390.0, split.fat, EPS)
    }

    @Test
    fun `macro split of an empty meal is zero`() {
        assertEquals(MacroSplit.ZERO, Nutrients.ZERO.macroSplit())
    }

    @Test
    fun `meal type follows the hour of the day`() {
        assertEquals(MealType.BREAKFAST, MealType.forTime(LocalTime.of(8, 0)))
        assertEquals(MealType.LUNCH, MealType.forTime(LocalTime.of(14, 30)))
        assertEquals(MealType.SNACK, MealType.forTime(LocalTime.of(17, 0)))
        assertEquals(MealType.DINNER, MealType.forTime(LocalTime.of(21, 15)))
        assertNull(MealType.fromName("BRUNCH"))
    }

    @Test
    fun `meal entry rescales nutrients when grams change`() {
        val entry = mealEntry(grams = 40, nutrients = oats * 0.4)

        val doubled = entry.withGrams(80)

        assertEquals(80, doubled.grams)
        assertEquals(oats.kcal * 0.8, doubled.nutrients.kcal, EPS)
    }

    @Test
    fun `day totals ignore water when counting meals and sum both`() {
        val meals =
            listOf(
                mealEntry(grams = 40, nutrients = oats * 0.4),
                mealEntry(id = "b", grams = 100, nutrients = oats),
            )
        val water = listOf(WaterEntry("w1", date, 250, now, zone), WaterEntry("w2", date, 500, now, zone))

        val totals = DayTotals.of(date, meals, water)

        assertEquals(2, totals.mealCount)
        assertEquals(750, totals.waterMl)
        assertEquals(oats.kcal * 1.4, totals.nutrients.kcal, EPS)
        assertTrue(totals.hasMeals)
        assertFalse(DayTotals.of(date, emptyList(), water).hasMeals)
    }

    @Test
    fun `goals are valid only inside their ranges`() {
        assertTrue(NutritionGoals.DEFAULT.isValid)
        assertFalse(NutritionGoals.DEFAULT.copy(kcal = 100).isValid)
        assertFalse(NutritionGoals.DEFAULT.copy(waterMl = -1).isValid)
    }

    @Test
    fun `macro kcal adds up the macro goals with atwater factors`() {
        val goals = NutritionGoals(kcal = 2000, proteinG = 100, carbsG = 250, fatG = 65, waterMl = 2000)

        assertEquals(400 + 1000 + 585, goals.macroKcal)
    }

    @Test
    fun `series averages only use days with meals`() {
        val logged = DayTotals(date, Nutrients.ZERO.copy(kcal = 2000.0), waterMl = 1000, mealCount = 3)
        val empty = DayTotals.empty(date.plusDays(1))
        val series =
            NutritionSeries(
                start = date,
                endExclusive = date.plusDays(2),
                days = listOf(logged, empty),
                goals = NutritionGoals.DEFAULT,
            )

        assertEquals(1, series.loggedDayCount)
        assertEquals(2000.0, series.averageKcal ?: -1.0, EPS)
        assertEquals(500.0, series.averageWaterMl, EPS)
    }

    @Test
    fun `series without logged days has no average`() {
        val series = NutritionSeries(date, date.plusDays(1), listOf(DayTotals.empty(date)), NutritionGoals.DEFAULT)

        assertNull(series.averageKcal)
    }

    @Test
    fun `today summary reports progress and remaining kcal`() {
        val summary =
            NutritionTodaySummary(
                date = date,
                consumed = Nutrients.ZERO.copy(kcal = 1500.0),
                goals = NutritionGoals.DEFAULT,
                waterMl = 500,
                mealCount = 2,
            )

        assertEquals(0.75, summary.kcalProgress, EPS)
        assertEquals(500, summary.remainingKcal)
        assertEquals(0.25, summary.waterProgress, EPS)
    }

    @Test
    fun `rolling average skips unlogged days and windows without data`() {
        fun day(
            offset: Long,
            kcal: Double?,
        ): DayTotals {
            val target = date.plusDays(offset)
            return if (kcal == null) {
                DayTotals.empty(target)
            } else {
                DayTotals(target, Nutrients.ZERO.copy(kcal = kcal), waterMl = 0, mealCount = 1)
            }
        }

        val series =
            NutritionSeries(
                start = date,
                endExclusive = date.plusDays(6),
                days = listOf(day(0, null), day(1, 1000.0), day(2, null), day(3, 2000.0), day(4, null), day(5, null)),
                goals = NutritionGoals.DEFAULT,
            )

        val rolling = series.rollingAverageKcal(window = 2)

        // Día 0: sin datos; 1: 1000; 2: ventana {1,2} = 1000; 3: {2,3} = 2000; 4: {3,4} = 2000; 5: {4,5} vacía.
        assertEquals(listOf(1L, 2L, 3L, 4L), rolling.map { it.first.toEpochDay() - date.toEpochDay() })
        assertEquals(listOf(1000.0, 1000.0, 2000.0, 2000.0), rolling.map { it.second })
    }

    private fun mealEntry(
        id: String = "a",
        grams: Int,
        nutrients: Nutrients,
    ) = MealEntry(
        id = id,
        date = date,
        mealType = MealType.BREAKFAST,
        foodId = "avena",
        foodName = "Copos de avena",
        grams = grams,
        nutrients = nutrients,
        loggedAt = now,
        zone = zone,
    )

    private companion object {
        const val EPS = 1e-6
    }
}

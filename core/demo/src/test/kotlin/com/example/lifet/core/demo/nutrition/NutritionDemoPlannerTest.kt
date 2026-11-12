package com.example.lifet.core.demo.nutrition

import com.example.lifet.core.demo.DemoScenario
import com.example.lifet.core.model.nutrition.DayTotals
import com.example.lifet.core.model.nutrition.MealEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.util.Random

class NutritionDemoPlannerTest {
    private val today = LocalDate.of(2026, 3, 10)
    private val zone = ZoneId.of("Europe/Madrid")

    private fun plan(
        scenario: DemoScenario,
        seed: Long = 42L,
        zone: ZoneId = this.zone,
        today: LocalDate = this.today,
    ): NutritionDemoPlan {
        val catalog = SeedCatalog.foods + (DEMO_CUSTOM_FOODS + DEMO_EDGE_FOODS).associateBy { it.id }
        val builder = NutritionDemoBuilder(zone, Random(seed), catalog)
        return NutritionDemoPlanner(builder, today, NutritionEdgeCasePlanner(builder, today)).plan(scenario)
    }

    private fun MealEntry.signature() = "$date $mealType $foodId $grams"

    @Test
    fun `every menu food exists in the bundled seed`() {
        val missing =
            NutritionDemoMenus.all
                .flatten()
                .map { it.foodId }
                .filter { it !in SeedCatalog.foods && it !in DEMO_CUSTOM_FOODS.map { food -> food.id } }
                .toSet()

        assertTrue("Alimentos de menú que no están en la semilla: $missing", missing.isEmpty())
        assertTrue(NutritionDemoMenus.favoriteIds.all { it in SeedCatalog.foods })
    }

    @Test
    fun `the seed has around 150 foods and sane values`() {
        assertTrue(SeedCatalog.foods.size >= 150)
        SeedCatalog.foods.values.forEach { food ->
            assertTrue("${food.id} kcal fuera de rango", food.per100g.kcal in 0.0..900.0)
            // La energía declarada debe ser coherente con los macros (Atwater) con un margen amplio.
            val atwater = food.per100g.proteinG * 4 + food.per100g.carbsG * 4 + food.per100g.fatG * 9
            val coherent = atwater <= food.per100g.kcal * 1.5 + 30
            assertTrue("${food.id}: ${food.per100g.kcal} kcal vs $atwater por macros", coherent)
            val macroGrams = food.per100g.proteinG + food.per100g.carbsG + food.per100g.fatG
            assertTrue("${food.id} macros > 100 g", macroGrams <= 101.0)
        }
    }

    @Test
    fun `empty scenario writes nothing`() {
        val empty = plan(DemoScenario.EMPTY)

        assertEquals(NutritionDemoPlan(), empty)
    }

    @Test
    fun `new user has about three days of data ending today`() {
        val data = plan(DemoScenario.NEW_USER)

        val days = data.meals.map { it.date }.toSet()
        assertEquals(setOf(today.minusDays(2), today.minusDays(1), today), days)
        assertTrue(data.water.isNotEmpty())
        assertNull(data.goals)
    }

    @Test
    fun `regular scenario spans about six months with some unlogged days`() {
        val data = plan(DemoScenario.REGULAR)

        val loggedDays = data.meals.map { it.date }.toSet()
        val span = today.toEpochDay() - loggedDays.min().toEpochDay()
        assertTrue("span=$span", span in 170L..180L)
        assertTrue("debe haber días sin registrar", loggedDays.size < 181)
        assertTrue("pero muy pocos", loggedDays.size > 150)
        assertEquals(today, data.meals.maxOf { it.date })
        assertNotNull(data.goals)
        assertEquals(DEMO_CUSTOM_FOODS, data.customFoods)
    }

    @Test
    fun `regular days have credible daily energy and heavier weekends`() {
        val data = plan(DemoScenario.REGULAR)
        val kcalByDay = data.meals.groupBy { it.date }.mapValues { (_, meals) -> meals.sumOf { it.nutrients.kcal } }
        val full = kcalByDay.filterKeys { it != today }

        assertTrue("kcal diarias fuera de rango creíble", full.values.all { it in 1_000.0..4_500.0 })
        val weekend = full.filterKeys { it.dayOfWeek == DayOfWeek.SATURDAY || it.dayOfWeek == DayOfWeek.SUNDAY }
        val weekday = full - weekend.keys
        assertTrue(weekend.values.average() > weekday.values.average() * 1.1)
    }

    @Test
    fun `breakfasts are repetitive during the week`() {
        val data = plan(DemoScenario.REGULAR)
        val weekdayBreakfasts =
            data.meals
                .filter { it.mealType.name == "BREAKFAST" && it.date.dayOfWeek !in WEEKEND }
                .groupBy { it.date }
                .map { (_, items) -> items.map { it.foodId }.toSet() }

        val mostCommon =
            weekdayBreakfasts
                .groupingBy { it }
                .eachCount()
                .values
                .max()
        assertTrue("el desayuno habitual debe dominar", mostCommon > weekdayBreakfasts.size / 2)
    }

    @Test
    fun `water is irregular and includes days without any`() {
        val data = plan(DemoScenario.REGULAR)

        val byDay = data.water.groupBy { it.date }.mapValues { (_, items) -> items.sumOf { it.ml } }
        assertTrue(byDay.values.toSet().size > 10)
        assertTrue(
            byDay.size <
                data.meals
                    .map { it.date }
                    .toSet()
                    .size,
        )
    }

    @Test
    fun `same seed gives identical data and a different seed does not`() {
        val first = plan(DemoScenario.REGULAR, seed = 7).meals.map { it.signature() }
        val second = plan(DemoScenario.REGULAR, seed = 7).meals.map { it.signature() }
        val other = plan(DemoScenario.REGULAR, seed = 8).meals.map { it.signature() }

        assertEquals(first, second)
        assertFalse(first == other)
        assertEquals(
            plan(DemoScenario.REGULAR, seed = 7).meals.map { it.id },
            plan(DemoScenario.REGULAR, seed = 7).meals.map { it.id },
        )
    }

    @Test
    fun `dates follow the injected clock and never the real date`() {
        val moved = plan(DemoScenario.REGULAR, today = LocalDate.of(2031, 7, 1))

        assertEquals(LocalDate.of(2031, 7, 1), moved.meals.maxOf { it.date })
        assertTrue(moved.meals.all { it.date.year in 2030..2031 })
    }

    @Test
    fun `edge cases include midnight neighbours and extreme portions`() {
        val data = plan(DemoScenario.EDGE_CASES)

        val lateNight = data.meals.first { it.date == today.minusDays(1) && it.loggedAt.atZone(zone).hour == 23 }
        val justAfter = data.meals.first { it.date == today && it.loggedAt.atZone(zone).hour == 0 }
        assertEquals(
            2L,
            java.time.Duration
                .between(lateNight.loggedAt, justAfter.loggedAt)
                .toMinutes(),
        )
        assertTrue(data.meals.any { it.grams == MealEntry.MAX_GRAMS })
        assertTrue(data.meals.any { it.grams == MealEntry.MIN_GRAMS })
        assertTrue(data.meals.any { it.foodName.length > 200 })
        assertTrue(data.meals.any { it.foodName.contains("🌮") })
        assertTrue(data.meals.any { it.nutrients.kcal == 0.0 })
        assertTrue(data.water.any { it.ml >= 9_000 })
    }

    @Test
    fun `edge cases leave a day without data between full days`() {
        val data = plan(DemoScenario.EDGE_CASES)
        val days = data.meals.map { it.date }.toSet()

        val gap = (1L..10L).map { today.minusDays(it) }.filter { it !in days }
        assertTrue("debe haber un día sin datos entre días con datos", gap.isNotEmpty())
        assertTrue(days.any { it < gap.min() })
        assertTrue(days.any { it > gap.min() })
    }

    @Test
    fun `edge cases cover the last daylight saving change of the zone`() {
        val data = plan(DemoScenario.EDGE_CASES)
        val dstDay = lastDstDay(zone, today)

        assertNotNull(dstDay)
        // Con hoy = 2026-03-10, el último cambio de hora ya ocurrido es el de otoño (25 h): domingo 26-oct-2025.
        assertEquals(LocalDate.of(2025, 10, 26), dstDay)
        val ofDay = data.meals.filter { it.date == dstDay }
        assertTrue(ofDay.isNotEmpty())
        assertTrue(ofDay.all { it.loggedAt.atZone(zone).toLocalDate() == dstDay })
    }

    @Test
    fun `a zone without daylight saving simply has no dst day`() {
        assertNull(lastDstDay(ZoneId.of("UTC"), today))
        val data = plan(DemoScenario.EDGE_CASES, zone = ZoneId.of("UTC"))

        assertTrue(data.meals.isNotEmpty())
    }

    @Test
    fun `daily totals of generated data match the sum of their entries`() {
        val data = plan(DemoScenario.REGULAR)
        val day =
            data.meals
                .groupBy { it.date }
                .entries
                .first()

        val totals = DayTotals.of(day.key, day.value, emptyList())

        assertEquals(day.value.sumOf { it.nutrients.kcal }, totals.nutrients.kcal, 1e-6)
    }

    private companion object {
        val WEEKEND = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
    }
}

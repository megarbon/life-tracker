package com.example.lifet.core.demo.nutrition

import com.example.lifet.core.demo.DemoScenario
import com.example.lifet.core.model.nutrition.FoodCategory
import com.example.lifet.core.model.nutrition.FoodItem
import com.example.lifet.core.model.nutrition.MealEntry
import com.example.lifet.core.model.nutrition.MealType
import com.example.lifet.core.model.nutrition.Nutrients
import com.example.lifet.core.model.nutrition.NutritionGoals
import com.example.lifet.core.model.nutrition.WaterEntry
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

private const val REGULAR_DAYS = 180
private const val NEW_USER_DAYS = 3
private const val SKIPPED_DAY_CHANCE = 0.06
private const val WEEKDAY_SNACK_CHANCE = 0.6
private const val WEEKEND_SNACK_CHANCE = 0.8

// Las raciones de los menús son "de libro"; en la vida real se come algo más, y el fin de semana bastante más.
private const val WEEKDAY_SCALE = 1.25
private const val WEEKEND_SCALE = 1.4
private const val WEEKEND_SPECIAL_CHANCE = 0.7
private const val NO_WATER_CHANCE = 0.15
private const val MIN_GLASSES = 2
private const val GLASS_SPREAD = 8
private const val BIG_BOTTLE_ML = 500
private const val BIG_BOTTLE_CHANCE = 0.2
private const val USUAL_BREAKFAST_CHANCE = 0.7
private const val OAT_BREAKFAST_CHANCE = 0.9
private const val CUSTOM_SNACK_CHANCE = 0.25

internal val DEMO_CUSTOM_FOODS =
    listOf(
        FoodItem(
            id = "demo_custom_bizcocho",
            name = "Bizcocho de la abuela",
            category = FoodCategory.SWEETS_SNACKS,
            per100g = Nutrients(kcal = 390.0, proteinG = 6.0, carbsG = 52.0, fatG = 17.0, fiberG = 1.2, sugarG = 28.0),
            servingGrams = 60,
            isCustom = true,
        ),
        FoodItem(
            id = "demo_custom_batido",
            name = "Batido de proteínas casero",
            category = FoodCategory.DRINKS,
            per100g = Nutrients(kcal = 95.0, proteinG = 9.0, carbsG = 10.0, fatG = 2.0, fiberG = 1.0, sugarG = 7.0),
            servingGrams = 300,
            isCustom = true,
        ),
    )

internal val DEMO_REGULAR_GOALS =
    NutritionGoals(kcal = 2_000, proteinG = 110, carbsG = 240, fatG = 65, waterMl = 2_000)

/** Decide qué datos de cada [DemoScenario] se escriben; no toca Room, así se puede probar en la JVM. */
internal class NutritionDemoPlanner(
    private val builder: NutritionDemoBuilder,
    private val today: LocalDate,
    private val edgeCases: NutritionEdgeCasePlanner,
) {
    fun plan(scenario: DemoScenario): NutritionDemoPlan =
        when (scenario) {
            DemoScenario.EMPTY -> NutritionDemoPlan()
            DemoScenario.NEW_USER -> newUser()
            DemoScenario.REGULAR -> regular()
            DemoScenario.EDGE_CASES -> edgeCases.plan()
        }

    private fun newUser(): NutritionDemoPlan {
        val days = (NEW_USER_DAYS - 1 downTo 1).map { today.minusDays(it.toLong()) }
        val earlier = days.map(::fullDay)
        val todayPart = partialToday()
        return NutritionDemoPlan(
            meals = earlier.flatMap { it.first } + todayPart.first,
            water = earlier.flatMap { it.second } + todayPart.second,
        )
    }

    private fun regular(): NutritionDemoPlan {
        val days = (REGULAR_DAYS downTo 1).map { today.minusDays(it.toLong()) }
        // Primero se decide qué días se olvida registrar para que sea independiente del resto del azar.
        val logged = days.filter { builder.random.nextDouble() >= SKIPPED_DAY_CHANCE }
        val history = logged.map(::fullDay)
        val todayPart = partialToday()
        return NutritionDemoPlan(
            meals = history.flatMap { it.first } + todayPart.first,
            water = history.flatMap { it.second } + todayPart.second,
            goals = DEMO_REGULAR_GOALS,
            customFoods = DEMO_CUSTOM_FOODS,
            favoriteIds = NutritionDemoMenus.favoriteIds,
        )
    }

    private fun fullDay(date: LocalDate): Pair<List<MealEntry>, List<WaterEntry>> {
        val weekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
        val scale = if (weekend) WEEKEND_SCALE else WEEKDAY_SCALE
        val meals =
            builder.meal(date, MealType.BREAKFAST, breakfast(weekend), BREAKFAST_HOUR, scale) +
                builder.meal(date, MealType.LUNCH, lunch(weekend), LUNCH_HOUR, scale) +
                snackMeals(date, weekend, scale) +
                builder.meal(date, MealType.DINNER, dinner(weekend), DINNER_HOUR, scale)
        return meals to waterOf(date)
    }

    private fun partialToday(): Pair<List<MealEntry>, List<WaterEntry>> {
        val breakfast = builder.meal(today, MealType.BREAKFAST, NutritionDemoMenus.usualBreakfast, BREAKFAST_HOUR)
        val water = listOf(builder.water(today, LocalTime.of(BREAKFAST_HOUR, 0), WaterEntry.GLASS_ML))
        return breakfast to water
    }

    private fun snackMeals(
        date: LocalDate,
        weekend: Boolean,
        scale: Double,
    ): List<MealEntry> {
        val chance = if (weekend) WEEKEND_SNACK_CHANCE else WEEKDAY_SNACK_CHANCE
        if (builder.random.nextDouble() >= chance) return emptyList()
        val pool = if (weekend) NutritionDemoMenus.weekendSnacks else NutritionDemoMenus.snacks
        val portions =
            if (builder.random.nextDouble() < CUSTOM_SNACK_CHANCE) {
                listOf(DemoPortion(DEMO_CUSTOM_FOODS.first().id, DEMO_CUSTOM_FOODS.first().servingGrams))
            } else {
                pool.pick()
            }
        return builder.meal(date, MealType.SNACK, portions, SNACK_HOUR, scale)
    }

    private fun waterOf(date: LocalDate): List<WaterEntry> {
        if (builder.random.nextDouble() < NO_WATER_CHANCE) return emptyList()
        val glasses = MIN_GLASSES + builder.random.nextInt(GLASS_SPREAD)
        return (0 until glasses).map { index ->
            val big = builder.random.nextDouble() < BIG_BOTTLE_CHANCE
            val hour = (WATER_FIRST_HOUR + index * 2).coerceAtMost(WATER_LAST_HOUR)
            builder.water(date, LocalTime.of(hour, builder.random.nextInt(MINUTES)), if (big) BIG_BOTTLE_ML else GLASS)
        }
    }

    private fun breakfast(weekend: Boolean): List<DemoPortion> {
        val roll = builder.random.nextDouble()
        return when {
            weekend && roll < WEEKEND_SPECIAL_CHANCE -> NutritionDemoMenus.weekendBreakfasts.pick()
            roll < USUAL_BREAKFAST_CHANCE -> NutritionDemoMenus.usualBreakfast
            roll < OAT_BREAKFAST_CHANCE -> NutritionDemoMenus.oatBreakfast
            else -> NutritionDemoMenus.yogurtBreakfast
        }
    }

    private fun lunch(weekend: Boolean): List<DemoPortion> =
        if (weekend) NutritionDemoMenus.weekendLunches.pick() else NutritionDemoMenus.lunches.pick()

    private fun dinner(weekend: Boolean): List<DemoPortion> =
        if (weekend && builder.random.nextDouble() < WEEKEND_SPECIAL_CHANCE) {
            NutritionDemoMenus.weekendDinners.pick()
        } else {
            NutritionDemoMenus.dinners.pick()
        }

    private fun <T> List<T>.pick(): T = this[builder.random.nextInt(size)]

    private companion object {
        const val BREAKFAST_HOUR = 8
        const val LUNCH_HOUR = 14
        const val SNACK_HOUR = 17
        const val DINNER_HOUR = 21
        const val WATER_FIRST_HOUR = 8
        const val WATER_LAST_HOUR = 22
        const val MINUTES = 60
        const val GLASS = WaterEntry.GLASS_ML
    }
}

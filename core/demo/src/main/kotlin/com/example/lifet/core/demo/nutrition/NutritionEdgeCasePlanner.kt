// Horas y gramos literales de un escenario de prueba: son datos de ejemplo, no lógica.
@file:Suppress("MagicNumber")

package com.example.lifet.core.demo.nutrition

import com.example.lifet.core.model.nutrition.FoodCategory
import com.example.lifet.core.model.nutrition.FoodItem
import com.example.lifet.core.model.nutrition.MealEntry
import com.example.lifet.core.model.nutrition.MealType
import com.example.lifet.core.model.nutrition.Nutrients
import com.example.lifet.core.model.nutrition.NutritionGoals
import com.example.lifet.core.model.nutrition.WaterEntry
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

private const val LONG_NAME_REPEATS = 12
private const val HUGE_GRAMS = MealEntry.MAX_GRAMS
private const val TINY_GRAMS = MealEntry.MIN_GRAMS
private const val HUGE_WATER_ML = 9_999
private const val DST_SEARCH_DAYS = 400L

internal const val EDGE_LONG_FOOD_ID = "demo_edge_long"
internal const val EDGE_EMOJI_FOOD_ID = "demo_edge_emoji"
internal const val EDGE_ZERO_FOOD_ID = "demo_edge_zero"

private val EDGE_LONG_NAME =
    "Plato combinado especial de la casa con arroz, huevo, plátano frito y salsa picante " +
        "recomendado por el chef ".repeat(LONG_NAME_REPEATS)

internal val DEMO_EDGE_FOODS =
    listOf(
        FoodItem(
            id = EDGE_LONG_FOOD_ID,
            name = EDGE_LONG_NAME.trim(),
            category = FoodCategory.DISHES,
            per100g = Nutrients(kcal = 999.0, proteinG = 80.0, carbsG = 120.0, fatG = 70.0),
            servingGrams = 400,
            isCustom = true,
        ),
        FoodItem(
            id = EDGE_EMOJI_FOOD_ID,
            name = "🌮 Tacos de la abuela 🍅🔥 (¡muy picantes!)",
            category = FoodCategory.DISHES,
            per100g = Nutrients(kcal = 215.0, proteinG = 11.0, carbsG = 24.0, fatG = 8.0),
            servingGrams = 120,
            isCustom = true,
            isFavorite = true,
        ),
        FoodItem(
            id = EDGE_ZERO_FOOD_ID,
            name = "Agua con gas",
            category = FoodCategory.DRINKS,
            per100g = Nutrients.ZERO,
            servingGrams = 250,
            isCustom = true,
        ),
    )

/**
 * Casos límite de Alimentación: cruce de medianoche, cambio de hora (DST), textos larguísimos, emojis, raciones
 * enormes y mínimas, alimentos de 0 kcal, un día sin datos entre días llenos y objetivos en el extremo del rango.
 */
internal class NutritionEdgeCasePlanner(
    private val builder: NutritionDemoBuilder,
    private val today: LocalDate,
) {
    fun plan(): NutritionDemoPlan =
        NutritionDemoPlan(
            meals =
                midnightCrossing() + dstDay() + extremeDay() + referenceDay(today.minusDays(2)) +
                    referenceDay(today.minusDays(NO_DATA_GAP_OFFSET + 1)),
            water = midnightWater() + listOf(builder.water(today, LocalTime.NOON, HUGE_WATER_ML)),
            goals = NutritionGoals(kcal = 5_000, proteinG = 300, carbsG = 600, fatG = 150, waterMl = 5_000),
            customFoods = DEMO_EDGE_FOODS,
            favoriteIds = listOf(EDGE_EMOJI_FOOD_ID),
        )

    /**
     * Una cena a las 23:59 y un snack a las 00:01 del día siguiente: cada una pertenece a su día lógico aunque
     * estén a dos minutos. `yesterday` queda con datos y `today - 1` es el día del cruce.
     */
    private fun midnightCrossing(): List<MealEntry> {
        val day = today.minusDays(1)
        return listOfNotNull(
            builder.mealAt(day, LocalTime.of(LATE_HOUR, LATE_MINUTE), MealType.DINNER, "seed_pizza", 300),
            builder.mealAt(today, LocalTime.of(0, 1), MealType.SNACK, "seed_helado", 100),
        )
    }

    private fun midnightWater(): List<WaterEntry> =
        listOf(
            builder.water(today.minusDays(1), LocalTime.of(LATE_HOUR, LATE_MINUTE), WaterEntry.GLASS_ML),
            builder.water(today, LocalTime.of(0, 1), WaterEntry.GLASS_ML),
        )

    /**
     * El día del último cambio de hora de la zona antes de hoy (hay uno cada ~6 meses): ese día dura 23 o 25 h y
     * las horas a ambos lados del salto no deben duplicarse ni perderse.
     */
    private fun dstDay(): List<MealEntry> {
        val day = lastDstDay(builder.zone, today) ?: return emptyList()
        return listOfNotNull(
            builder.mealAt(day, LocalTime.of(1, 30), MealType.SNACK, "seed_cafe_leche", 200),
            builder.mealAt(day, LocalTime.of(3, 30), MealType.BREAKFAST, "seed_tostada_tomate", 80),
            builder.mealAt(day, LocalTime.of(14, 0), MealType.LUNCH, "seed_paella", 300),
        )
    }

    /** Raciones de 5 kg, de 1 g, textos larguísimos, emojis y un alimento sin calorías en el mismo día. */
    private fun extremeDay(): List<MealEntry> {
        val day = today.minusDays(EXTREME_DAY_OFFSET)
        return listOfNotNull(
            builder.mealAt(day, LocalTime.of(9, 0), MealType.BREAKFAST, EDGE_LONG_FOOD_ID, HUGE_GRAMS),
            builder.mealAt(day, LocalTime.of(10, 0), MealType.BREAKFAST, "seed_sal_inexistente", 100),
            builder.mealAt(day, LocalTime.of(11, 0), MealType.SNACK, EDGE_EMOJI_FOOD_ID, TINY_GRAMS),
            builder.mealAt(day, LocalTime.of(13, 0), MealType.LUNCH, EDGE_EMOJI_FOOD_ID, 400),
            builder.mealAt(day, LocalTime.of(16, 0), MealType.SNACK, EDGE_ZERO_FOOD_ID, 500),
            builder.mealAt(day, LocalTime.of(20, 0), MealType.DINNER, "seed_aceite_oliva", 1_000),
        )
    }

    /** Un día normal alrededor del hueco sin datos, para que la serie tenga días llenos a ambos lados. */
    private fun referenceDay(day: LocalDate): List<MealEntry> =
        builder.meal(day, MealType.BREAKFAST, NutritionDemoMenus.usualBreakfast, REFERENCE_HOUR) +
            builder.meal(day, MealType.LUNCH, NutritionDemoMenus.lunches.first(), REFERENCE_HOUR + 6)

    private companion object {
        const val LATE_HOUR = 23
        const val LATE_MINUTE = 59
        const val EXTREME_DAY_OFFSET = 4L
        const val NO_DATA_GAP_OFFSET = 4L + 1
        const val REFERENCE_HOUR = 8
    }
}

/** Día del último cambio de hora de [zone] anterior a [today], o `null` si la zona no tiene DST (p. ej. UTC). */
internal fun lastDstDay(
    zone: ZoneId,
    today: LocalDate,
): LocalDate? {
    val start = today.atStartOfDay(zone).toInstant()
    val transition = zone.rules.previousTransition(start)
    val limit: Instant = start.minusSeconds(DST_SEARCH_DAYS * SECONDS_PER_DAY)
    return transition?.takeIf { it.instant.isAfter(limit) }?.let { it.dateTimeAfter.toLocalDate() }
}

private const val SECONDS_PER_DAY = 86_400L

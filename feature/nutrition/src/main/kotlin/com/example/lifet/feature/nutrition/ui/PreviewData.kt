// Datos de ejemplo para @Preview: los gramos y kcal son datos de muestra, no lógica.
@file:Suppress("MagicNumber")

package com.example.lifet.feature.nutrition.ui

import com.example.lifet.core.model.nutrition.DayTotals
import com.example.lifet.core.model.nutrition.FoodCategory
import com.example.lifet.core.model.nutrition.FoodItem
import com.example.lifet.core.model.nutrition.MealEntry
import com.example.lifet.core.model.nutrition.MealType
import com.example.lifet.core.model.nutrition.Nutrients
import com.example.lifet.core.model.nutrition.NutritionGoals
import com.example.lifet.core.model.nutrition.NutritionSeries
import com.example.lifet.core.model.nutrition.WaterEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Datos fijos para las previews: misma fecha siempre, nunca la real. */
internal object PreviewData {
    val today: LocalDate = LocalDate.of(2026, 3, 10)
    private val zone: ZoneId = ZoneId.of("Europe/Madrid")
    private val instant: Instant = Instant.parse("2026-03-10T09:00:00Z")

    val bread = FoodItem("p_pan", "Pan blanco", FoodCategory.GRAIN, Nutrients(265.0, 9.0, 49.0, 3.2, 2.7, 5.0), 40)
    val oil = FoodItem("p_aceite", "Aceite de oliva", FoodCategory.FATS_NUTS, Nutrients(884.0, 0.0, 0.0, 100.0), 10)
    val paella = FoodItem("p_paella", "Paella", FoodCategory.DISHES, Nutrients(148.0, 6.0, 21.0, 4.5, 1.0, 0.5), 300)
    val longName =
        FoodItem(
            id = "p_long",
            name = "🌮 Plato combinado especial de la casa con arroz, huevo, plátano frito y salsa picante del chef",
            category = FoodCategory.DISHES,
            per100g = Nutrients(215.0, 11.0, 24.0, 8.0),
            servingGrams = 400,
            isCustom = true,
            isFavorite = true,
        )

    val foods = listOf(bread, oil, paella, longName)

    fun entry(
        food: FoodItem,
        grams: Int,
        type: MealType,
    ): MealEntry =
        MealEntry(
            id = "${food.id}-$type",
            date = today,
            mealType = type,
            foodId = food.id,
            foodName = food.name,
            grams = grams,
            nutrients = food.nutrientsFor(grams),
            loggedAt = instant,
            zone = zone,
        )

    val meals: List<MealEntry> =
        listOf(
            entry(bread, 80, MealType.BREAKFAST),
            entry(oil, 10, MealType.BREAKFAST),
            entry(paella, 350, MealType.LUNCH),
        )

    val water: List<WaterEntry> = List(4) { WaterEntry("w$it", today, WaterEntry.GLASS_ML, instant, zone) }

    val goals: NutritionGoals = NutritionGoals.DEFAULT

    /** 30 días con huecos y fines de semana más altos, para la tendencia. */
    val series: NutritionSeries =
        NutritionSeries(
            start = today.minusDays(29),
            endExclusive = today.plusDays(1),
            days =
                (29 downTo 0).map { offset ->
                    val date = today.minusDays(offset.toLong())
                    if (offset % 9 == 4) {
                        DayTotals.empty(date)
                    } else {
                        val weekend = date.dayOfWeek.value >= 6
                        val kcal = (if (weekend) 2_450.0 else 1_850.0) + (offset % 5) * 60
                        DayTotals(date, Nutrients(kcal, kcal * 0.05, kcal * 0.12, kcal * 0.035), 1_500, 8)
                    }
                },
            goals = goals,
        )

    /** Una sola comida enorme (5 kg) con nombre larguísimo: caso límite para la pantalla Diario. */
    val extremeMeals: List<MealEntry> = listOf(entry(longName, 5_000, MealType.DINNER))

    /** Los últimos 7 días de [series], que es lo que lleva el Diario. */
    val week: NutritionSeries = series.copy(days = series.days.takeLast(7))

    const val PORTION_GRAMS: Int = 300
}

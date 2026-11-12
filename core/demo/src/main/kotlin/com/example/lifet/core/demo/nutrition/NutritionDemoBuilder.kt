package com.example.lifet.core.demo.nutrition

import com.example.lifet.core.model.nutrition.FoodItem
import com.example.lifet.core.model.nutrition.MealEntry
import com.example.lifet.core.model.nutrition.MealType
import com.example.lifet.core.model.nutrition.WaterEntry
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Random
import java.util.UUID
import kotlin.math.roundToInt

private const val GRAM_STEP = 5
private const val MIN_VARIATION = 0.9
private const val VARIATION_SPAN = 0.2
private const val MINUTES_IN_HOUR = 60

/** Piezas para fabricar entradas de demo: el azar sale de [random] (semilla fija) y la zona es la del reloj. */
internal class NutritionDemoBuilder(
    val zone: ZoneId,
    val random: Random,
    val foods: Map<String, FoodItem>,
) {
    /** Id determinista: con la misma semilla salen siempre los mismos UUID. */
    fun nextId(): String = UUID(random.nextLong(), random.nextLong()).toString()

    /** Una entrada exacta; `null` si el alimento no existe en la base (la demo no debe fallar por eso). */
    fun mealAt(
        date: LocalDate,
        time: LocalTime,
        type: MealType,
        foodId: String,
        grams: Int,
    ): MealEntry? =
        foods[foodId]?.let { food ->
            MealEntry(
                id = nextId(),
                date = date,
                mealType = type,
                foodId = food.id,
                foodName = food.name,
                grams = grams,
                nutrients = food.nutrientsFor(grams),
                loggedAt = date.atTime(time).atZone(zone).toInstant(),
                zone = zone,
            )
        }

    /** Un menú completo hacia las [hour]:xx, con los gramos variados ±10 % y multiplicados por [scale]. */
    fun meal(
        date: LocalDate,
        type: MealType,
        portions: List<DemoPortion>,
        hour: Int,
        scale: Double = 1.0,
    ): List<MealEntry> {
        val startMinute = random.nextInt(MINUTES_IN_HOUR - portions.size)
        return portions.mapIndexedNotNull { index, portion ->
            mealAt(date, LocalTime.of(hour, startMinute + index), type, portion.foodId, varied(portion.grams, scale))
        }
    }

    fun water(
        date: LocalDate,
        time: LocalTime,
        ml: Int,
    ): WaterEntry = WaterEntry(nextId(), date, ml, date.atTime(time).atZone(zone).toInstant(), zone)

    /** Gramos redondeados a múltiplos de 5, nunca menos de 5. */
    private fun varied(
        grams: Int,
        scale: Double,
    ): Int {
        val value = grams * scale * (MIN_VARIATION + VARIATION_SPAN * random.nextDouble())
        return maxOf(GRAM_STEP, (value / GRAM_STEP).roundToInt() * GRAM_STEP)
    }
}

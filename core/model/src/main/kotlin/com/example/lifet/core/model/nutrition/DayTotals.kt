package com.example.lifet.core.model.nutrition

import java.time.LocalDate

/** Totales de un día: lo comido ([nutrients], [mealCount] alimentos registrados) y el agua. */
public data class DayTotals(
    public val date: LocalDate,
    public val nutrients: Nutrients,
    public val waterMl: Int,
    public val mealCount: Int,
) {
    /** Un día sin ningún alimento registrado se considera "sin registrar", no "0 kcal". */
    public val hasMeals: Boolean get() = mealCount > 0

    public companion object {
        public fun empty(date: LocalDate): DayTotals = DayTotals(date, Nutrients.ZERO, waterMl = 0, mealCount = 0)

        public fun of(
            date: LocalDate,
            meals: List<MealEntry>,
            water: List<WaterEntry>,
        ): DayTotals =
            DayTotals(
                date = date,
                nutrients = meals.fold(Nutrients.ZERO) { acc, meal -> acc + meal.nutrients },
                waterMl = water.sumOf { it.ml },
                mealCount = meals.size,
            )
    }
}

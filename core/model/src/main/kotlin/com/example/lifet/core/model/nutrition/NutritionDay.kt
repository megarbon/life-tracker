package com.example.lifet.core.model.nutrition

import java.time.LocalDate

/** Todo lo que se registró en un día concreto junto con los objetivos vigentes. Base de la pantalla Diario. */
public data class NutritionDay(
    public val date: LocalDate,
    public val meals: List<MealEntry>,
    public val water: List<WaterEntry>,
    public val goals: NutritionGoals,
) {
    public val totals: DayTotals get() = DayTotals.of(date, meals, water)
}

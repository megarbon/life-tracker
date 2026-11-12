package com.example.lifet.core.model.nutrition

import java.time.LocalDate

/** Lo que la pantalla "Hoy" necesita de Alimentación: kcal y macros del día frente a sus objetivos, y agua. */
public data class NutritionTodaySummary(
    public val date: LocalDate,
    public val consumed: Nutrients,
    public val goals: NutritionGoals,
    public val waterMl: Int,
    public val mealCount: Int,
) {
    /** Progreso de energía (puede superar 1.0); 0 si el objetivo no es válido. */
    public val kcalProgress: Double
        get() = if (goals.kcal > 0) consumed.kcal / goals.kcal else 0.0

    /** kcal que faltan para el objetivo; negativo si se ha superado. */
    public val remainingKcal: Int
        get() = goals.kcal - consumed.kcal.toInt()

    public val waterProgress: Double
        get() = if (goals.waterMl > 0) waterMl.toDouble() / goals.waterMl else 0.0
}

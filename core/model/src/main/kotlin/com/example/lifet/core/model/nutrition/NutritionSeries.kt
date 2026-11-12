package com.example.lifet.core.model.nutrition

import java.time.LocalDate

/**
 * Serie diaria para gráficas e Insights: un [DayTotals] por cada día de `[start, endExclusive)`, con los días
 * sin registrar a cero ([DayTotals.hasMeals] = `false`) para que los huecos se vean como huecos.
 */
public data class NutritionSeries(
    public val start: LocalDate,
    public val endExclusive: LocalDate,
    public val days: List<DayTotals>,
    public val goals: NutritionGoals,
) {
    private val loggedDays: List<DayTotals> get() = days.filter { it.hasMeals }

    public val loggedDayCount: Int get() = loggedDays.size

    /** Media de kcal de los días con registro (los vacíos no cuentan); `null` si no hay ninguno. */
    public val averageKcal: Double?
        get() {
            val logged = loggedDays
            return if (logged.isEmpty()) null else logged.sumOf { it.nutrients.kcal } / logged.size
        }

    /** Reparto medio de macros de los días con registro. */
    public val averageMacroSplit: MacroSplit
        get() = loggedDays.fold(Nutrients.ZERO) { acc, day -> acc + day.nutrients }.macroSplit()

    /** Media de agua de todos los días del rango; el agua sí puede ser 0 de verdad. */
    public val averageWaterMl: Double
        get() = if (days.isEmpty()) 0.0 else days.sumOf { it.waterMl }.toDouble() / days.size

    public fun day(date: LocalDate): DayTotals? = days.firstOrNull { it.date == date }
}

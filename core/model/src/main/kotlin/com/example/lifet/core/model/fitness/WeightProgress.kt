package com.example.lifet.core.model.fitness

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/** Un peso con su media móvil de los últimos [FitnessRules.MOVING_AVERAGE_DAYS] días. */
public data class WeightPoint(
    public val date: LocalDate,
    public val weightGrams: Long,
    public val movingAverageGrams: Long,
)

/**
 * Evolución del peso. [points] va en orden cronológico. [changeGrams] compara la media móvil actual con la de hace
 * unos 30 días; es `null` si no hay ningún registro cerca de esa fecha (no se inventa una tendencia).
 */
public data class WeightProgress(
    public val points: List<WeightPoint>,
    public val changeGrams: Long?,
) {
    public val latest: WeightPoint? get() = points.lastOrNull()
}

/** Cálculos puros sobre el peso corporal. */
public object WeightAnalytics {
    /** Tolerancia, en días, al buscar el registro de hace 30 días con el que comparar. */
    private const val BASELINE_TOLERANCE_DAYS = 7L

    public fun progress(weights: List<BodyWeight>): WeightProgress {
        val sorted = weights.sortedBy { it.date }
        val points =
            sorted.map { weight ->
                WeightPoint(weight.date, weight.weightGrams, movingAverage(sorted, weight.date))
            }
        return WeightProgress(points, change(points))
    }

    /** Media de los registros de los [FitnessRules.MOVING_AVERAGE_DAYS] días que terminan en [date] (incluido). */
    public fun movingAverage(
        sorted: List<BodyWeight>,
        date: LocalDate,
    ): Long {
        val from = date.minusDays(FitnessRules.MOVING_AVERAGE_DAYS - 1L)
        val window = sorted.filter { it.date >= from && it.date <= date }
        return if (window.isEmpty()) 0L else window.sumOf { it.weightGrams } / window.size
    }

    private fun change(points: List<WeightPoint>): Long? {
        val latest = points.lastOrNull() ?: return null
        val target = latest.date.minusDays(FitnessRules.WEIGHT_TREND_DAYS.toLong())
        val baseline =
            points
                .filter { it !== latest && distance(it.date, target) <= BASELINE_TOLERANCE_DAYS }
                .minByOrNull { distance(it.date, target) }
        return baseline?.let { latest.movingAverageGrams - it.movingAverageGrams }
    }

    private fun distance(
        a: LocalDate,
        b: LocalDate,
    ): Long = abs(ChronoUnit.DAYS.between(a, b))
}

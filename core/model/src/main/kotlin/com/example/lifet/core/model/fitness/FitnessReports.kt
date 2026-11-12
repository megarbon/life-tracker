package com.example.lifet.core.model.fitness

import com.example.lifet.core.model.DateRange
import java.time.LocalDate

/**
 * Lo que la pantalla Hoy necesita de Deporte.
 *
 * @property weekActiveMinutes minutos activos de la semana en curso (lunes a domingo).
 * @property weeklyGoalMinutes objetivo semanal (150 por defecto, referencia de la OMS).
 * @property workedOutToday hay actividad registrada hoy; [todayActiveMinutes] son los minutos de hoy.
 * @property latestWeightGrams último peso registrado (con su fecha) o `null`.
 * @property stepsToday pasos registrados hoy o `null` si no se han anotado.
 * @property hasData `false` si el área aún no se ha usado: Hoy debe invitar a registrar el primer entreno.
 */
public data class FitnessTodaySummary(
    public val weekActiveMinutes: Int,
    public val weeklyGoalMinutes: Int,
    public val workedOutToday: Boolean,
    public val todayActiveMinutes: Int,
    public val latestWeightGrams: Long?,
    public val latestWeightDate: LocalDate?,
    public val stepsToday: Int?,
    public val hasData: Boolean,
) {
    /** Progreso hacia el objetivo semanal, entre 0 y 1. */
    public val weekProgress: Float
        get() = if (weeklyGoalMinutes <= 0) 0f else (weekActiveMinutes.toFloat() / weeklyGoalMinutes).coerceIn(0f, 1f)

    public val goalReached: Boolean get() = weeklyGoalMinutes > 0 && weekActiveMinutes >= weeklyGoalMinutes
}

/** Un día de la serie. [steps] y [weightGrams] son `null` si ese día no se registraron. */
public data class FitnessDayPoint(
    public val date: LocalDate,
    public val activeMinutes: Int,
    public val workouts: Int,
    public val steps: Int?,
    public val weightGrams: Long?,
)

/**
 * Serie de Deporte de un [range], pensada para gráficas e Insights. Sólo incluye días hasta hoy; los días sin
 * actividad aparecen con cero (no se omiten) para que las correlaciones vean los huecos.
 *
 * @property weights pesos registrados dentro del rango, con su media móvil (calculada con todo el historial).
 */
public data class FitnessSeries(
    public val range: DateRange,
    public val days: List<FitnessDayPoint>,
    public val weights: List<WeightPoint>,
) {
    public val totalMinutes: Int get() = days.sumOf { it.activeMinutes }

    public val workoutCount: Int get() = days.sumOf { it.workouts }

    public val activeDays: Int get() = days.count { it.activeMinutes > 0 }
}

/** Minutos activos de una semana que empieza en [weekStart] (lunes). */
public data class WeekTotal(
    public val weekStart: LocalDate,
    public val minutes: Int,
    public val workouts: Int,
    public val goalReached: Boolean,
)

/**
 * Todo lo que necesita la pantalla de Deporte, recalculado con cada cambio en la base de datos.
 *
 * @property weeks las últimas semanas, de la más antigua a la actual.
 * @property previousWeeksAverage media de minutos de las 4 semanas anteriores a la actual, o `null` si no hay
 *   ningún entreno en ellas (sin base para comparar).
 * @property intensity intensidad de 0 a 1 de cada día con actividad, para el heatmap.
 * @property workouts todos los entrenos, del más reciente al más antiguo.
 * @property recentWorkouts los últimos entrenos distintos (por tipo y título), para repetirlos con un toque.
 */
public data class FitnessOverview(
    public val today: LocalDate,
    public val summary: FitnessTodaySummary,
    public val week: DateRange,
    public val weekWorkouts: Int,
    public val weeks: List<WeekTotal>,
    public val previousWeeksAverage: Int?,
    public val intensity: Map<LocalDate, Float>,
    public val firstActivityDate: LocalDate?,
    public val workouts: List<Workout>,
    public val recentWorkouts: List<Workout>,
    public val history: ExerciseHistory,
    public val weight: WeightProgress,
    public val templates: List<WorkoutTemplate>,
    public val stepsGoal: Int,
    public val stepsWeekAverage: Int?,
    public val hasAnyRecord: Boolean,
)

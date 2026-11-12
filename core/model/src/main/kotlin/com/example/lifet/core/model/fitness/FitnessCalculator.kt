package com.example.lifet.core.model.fitness

import com.example.lifet.core.model.DateRange
import com.example.lifet.core.model.DateRangeUnit
import java.time.LocalDate

/**
 * Cálculos de Deporte sobre un [FitnessDataSet]. Pura y determinista: "hoy" llega de fuera.
 *
 * Un minuto activo es un minuto de cualquier entreno, de fuerza o de cardio. Los entrenos que cruzan la
 * medianoche reparten sus minutos entre los días que tocan (ver [Workout.secondsByDay]).
 */
public class FitnessCalculator(
    private val data: FitnessDataSet,
    private val today: LocalDate,
    private val weeklyGoalMinutes: Int = FitnessRules.WEEKLY_GOAL_MINUTES,
    private val stepsGoal: Int = FitnessRules.DEFAULT_STEPS_GOAL,
) {
    private val secondsByDay: Map<LocalDate, Long> by lazy {
        val totals = HashMap<LocalDate, Long>()
        data.workouts.forEach { workout ->
            workout.secondsByDay().forEach { (date, seconds) -> totals.merge(date, seconds, Long::plus) }
        }
        totals
    }

    private val workoutsByDay: Map<LocalDate, Int> by lazy {
        data.workouts.groupingBy { it.startDate }.eachCount()
    }

    private val stepsByDay: Map<LocalDate, Int> by lazy { data.steps.associate { it.date to it.steps } }

    private val weightProgress: WeightProgress by lazy { WeightAnalytics.progress(data.weights) }

    /** Minutos activos del día. */
    public fun minutesOn(date: LocalDate): Int = ((secondsByDay[date] ?: 0L) / FitnessRules.SECONDS_PER_MINUTE).toInt()

    /** Minutos activos de todos los días de [range]. */
    public fun minutesIn(range: DateRange): Int {
        val seconds = range.days.sumOf { secondsByDay[it] ?: 0L }
        return (seconds / FitnessRules.SECONDS_PER_MINUTE).toInt()
    }

    public fun todaySummary(): FitnessTodaySummary {
        val week = DateRange.containing(DateRangeUnit.WEEK, today)
        val latestWeight = weightProgress.points.lastOrNull { it.date <= today }
        return FitnessTodaySummary(
            weekActiveMinutes = minutesIn(week),
            weeklyGoalMinutes = weeklyGoalMinutes,
            workedOutToday = (secondsByDay[today] ?: 0L) > 0L,
            todayActiveMinutes = minutesOn(today),
            latestWeightGrams = latestWeight?.weightGrams,
            latestWeightDate = latestWeight?.date,
            stepsToday = stepsByDay[today],
            hasData = !data.hasNoRecords,
        )
    }

    public fun series(range: DateRange): FitnessSeries {
        val weights = weightProgress.points.associateBy { it.date }
        val days =
            range.days.filter { it <= today }.map { date ->
                FitnessDayPoint(
                    date = date,
                    activeMinutes = minutesOn(date),
                    workouts = workoutsByDay[date] ?: 0,
                    steps = stepsByDay[date],
                    weightGrams = weights[date]?.weightGrams,
                )
            }
        return FitnessSeries(range, days, weightProgress.points.filter { it.date in range })
    }

    public fun overview(): FitnessOverview {
        val currentWeek = DateRange.containing(DateRangeUnit.WEEK, today)
        val weeks = weekTotals(currentWeek)
        val sortedWorkouts = data.workouts.sortedByDescending { it.start }
        return FitnessOverview(
            today = today,
            summary = todaySummary(),
            week = currentWeek,
            weekWorkouts = currentWeek.days.sumOf { workoutsByDay[it] ?: 0 },
            weeks = weeks,
            previousWeeksAverage = previousAverage(weeks),
            intensity = intensity(),
            firstActivityDate = secondsByDay.keys.minOrNull(),
            workouts = sortedWorkouts,
            recentWorkouts = recent(sortedWorkouts),
            history = ExerciseAnalytics.history(data.workouts),
            weight = weightProgress,
            templates = data.templates.sortedBy { it.name.lowercase() },
            stepsGoal = stepsGoal,
            stepsWeekAverage = stepsWeekAverage(currentWeek),
            hasAnyRecord = !data.hasNoRecords,
        )
    }

    /** Intensidad de 0 a 1 de cada día con actividad; un día con poco movimiento sigue siendo distinguible. */
    public fun intensity(): Map<LocalDate, Float> {
        val fullSeconds = FitnessRules.HEATMAP_FULL_MINUTES * FitnessRules.SECONDS_PER_MINUTE
        return secondsByDay
            .filterValues { it > 0L }
            .mapValues { (_, seconds) ->
                (seconds.toFloat() / fullSeconds).coerceIn(FitnessRules.HEATMAP_MIN_INTENSITY, 1f)
            }
    }

    private fun weekTotals(currentWeek: DateRange): List<WeekTotal> =
        (FitnessRules.CHART_WEEKS - 1 downTo 0).map { back ->
            val week = currentWeek.shift(-back.toLong())
            val minutes = minutesIn(week)
            WeekTotal(
                weekStart = week.start,
                minutes = minutes,
                workouts = week.days.sumOf { workoutsByDay[it] ?: 0 },
                goalReached = weeklyGoalMinutes > 0 && minutes >= weeklyGoalMinutes,
            )
        }

    /** Media de las 4 semanas previas a la actual; `null` si en ellas no hubo ni un entreno. */
    private fun previousAverage(weeks: List<WeekTotal>): Int? {
        val previous = weeks.dropLast(1).takeLast(COMPARISON_WEEKS)
        return if (previous.sumOf { it.workouts } == 0) null else previous.sumOf { it.minutes } / previous.size
    }

    private fun stepsWeekAverage(week: DateRange): Int? {
        val recorded = week.days.filter { it <= today }.mapNotNull { stepsByDay[it] }
        return if (recorded.isEmpty()) null else recorded.sum() / recorded.size
    }

    private fun recent(sortedWorkouts: List<Workout>): List<Workout> =
        sortedWorkouts
            .distinctBy { it.type to it.title.trim().lowercase() }
            .take(FitnessRules.RECENT_WORKOUTS)

    private companion object {
        const val COMPARISON_WEEKS = 4
    }
}

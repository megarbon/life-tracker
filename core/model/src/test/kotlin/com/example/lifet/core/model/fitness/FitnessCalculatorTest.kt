package com.example.lifet.core.model.fitness

import com.example.lifet.core.model.DateRange
import com.example.lifet.core.model.DateRangeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class FitnessCalculatorTest {
    private val monday = LocalDate.of(2026, 3, 9)

    private fun calculator(
        vararg workouts: Workout,
        weights: List<BodyWeight> = emptyList(),
        steps: List<DailySteps> = emptyList(),
    ) = FitnessCalculator(FitnessDataSet(workouts.toList(), weights, steps), TODAY)

    @Test
    fun `week minutes add cardio and strength within monday to sunday`() {
        val summary =
            calculator(
                cardio("a", monday, 30),
                strength("b", TODAY, lift("Press", 5 to 60_000L), minutes = 50),
                cardio("last-week", monday.minusDays(1), 90),
            ).todaySummary()

        assertEquals(80, summary.weekActiveMinutes)
        assertEquals(150, summary.weeklyGoalMinutes)
        assertEquals(80f / 150f, summary.weekProgress, 0.0001f)
        assertFalse(summary.goalReached)
    }

    @Test
    fun `reaching the weekly goal is flagged and progress is capped`() {
        val summary = calculator(cardio("a", monday, 100), cardio("b", TODAY, 80)).todaySummary()

        assertTrue(summary.goalReached)
        assertEquals(1f, summary.weekProgress, 0f)
    }

    @Test
    fun `worked out today only when there is activity today`() {
        assertFalse(calculator(cardio("a", monday, 30)).todaySummary().workedOutToday)

        val summary = calculator(cardio("a", TODAY, 25)).todaySummary()
        assertTrue(summary.workedOutToday)
        assertEquals(25, summary.todayActiveMinutes)
    }

    @Test
    fun `a workout crossing midnight shares its minutes with the next day`() {
        val night = cardio("night", TODAY.minusDays(1), 120, time = LocalTime.of(22, 30))
        val calculator = calculator(night)

        assertEquals(90, calculator.minutesOn(TODAY.minusDays(1)))
        assertEquals(30, calculator.minutesOn(TODAY))
        assertTrue(calculator.todaySummary().workedOutToday)
    }

    @Test
    fun `summary reports latest weight, steps and whether the area was ever used`() {
        val empty = calculator().todaySummary()
        assertFalse(empty.hasData)
        assertNull(empty.latestWeightGrams)
        assertNull(empty.stepsToday)

        val summary =
            calculator(
                weights = listOf(BodyWeight(TODAY.minusDays(3), 81_000), BodyWeight(TODAY.minusDays(1), 80_500)),
                steps = listOf(DailySteps(TODAY, 6_200)),
            ).todaySummary()
        assertTrue(summary.hasData)
        assertEquals(80_500L, summary.latestWeightGrams)
        assertEquals(TODAY.minusDays(1), summary.latestWeightDate)
        assertEquals(6_200, summary.stepsToday)
    }

    @Test
    fun `templates alone do not count as having data`() {
        val data = FitnessDataSet(templates = listOf(WorkoutTemplate("t", "Push", WorkoutType.STRENGTH, 3_600)))

        assertFalse(FitnessCalculator(data, TODAY).todaySummary().hasData)
    }

    @Test
    fun `series fills inactive days with zeros and stops at today`() {
        val range = DateRange.containing(DateRangeUnit.WEEK, TODAY)
        val series =
            calculator(
                cardio("a", monday, 40),
                weights = listOf(BodyWeight(TODAY, 80_000)),
                steps = listOf(DailySteps(monday, 9_000)),
            ).series(range)

        assertEquals(3, series.days.size)
        assertEquals(listOf(40, 0, 0), series.days.map { it.activeMinutes })
        assertEquals(9_000, series.days.first().steps)
        assertNull(series.days[1].steps)
        assertEquals(80_000L, series.days.last().weightGrams)
        assertEquals(40, series.totalMinutes)
        assertEquals(1, series.activeDays)
        assertEquals(1, series.workoutCount)
    }

    @Test
    fun `overview has twelve weeks ending with the current one`() {
        val overview = calculator(cardio("a", monday, 60), cardio("b", monday.minusWeeks(2), 150)).overview()

        assertEquals(FitnessRules.CHART_WEEKS, overview.weeks.size)
        assertEquals(monday, overview.weeks.last().weekStart)
        assertEquals(60, overview.weeks.last().minutes)
        assertTrue(overview.weeks[overview.weeks.size - 3].goalReached)
        assertEquals(1, overview.weekWorkouts)
    }

    @Test
    fun `previous average ignores the current week and is unknown without history`() {
        assertNull(calculator(cardio("a", monday, 60)).overview().previousWeeksAverage)

        val overview = calculator(cardio("a", monday, 60), cardio("b", monday.minusWeeks(1), 120)).overview()
        assertEquals(30, overview.previousWeeksAverage)
    }

    @Test
    fun `heatmap intensity grows with minutes and never hides a short workout`() {
        val intensity =
            calculator(cardio("short", monday, 3), cardio("full", TODAY, 90)).intensity()

        assertEquals(FitnessRules.HEATMAP_MIN_INTENSITY, intensity.getValue(monday), 0f)
        assertEquals(1f, intensity.getValue(TODAY), 0f)
        assertFalse(TODAY.minusDays(1) in intensity)
    }

    @Test
    fun `recent workouts are distinct by type and title, newest first`() {
        val overview =
            calculator(
                cardio("old-run", monday.minusDays(5), 30, title = "Parque"),
                cardio("new-run", monday, 35, title = "parque"),
                cardio("walk", TODAY, 20, type = WorkoutType.WALK),
            ).overview()

        assertEquals(listOf("walk", "new-run"), overview.recentWorkouts.map { it.id })
        assertEquals(listOf("walk", "new-run", "old-run"), overview.workouts.map { it.id })
    }

    @Test
    fun `steps week average uses only recorded days up to today`() {
        val overview =
            calculator(
                steps = listOf(DailySteps(monday, 8_000), DailySteps(TODAY, 4_000)),
            ).overview()

        assertEquals(6_000, overview.stepsWeekAverage)
        assertNull(calculator().overview().stepsWeekAverage)
    }
}

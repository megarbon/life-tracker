package com.example.lifet.core.model.quit

import com.example.lifet.core.model.DateRange
import com.example.lifet.core.model.DateRangeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class QuitCalculatorTest {
    private val now = at("2026-03-10", "12:00")

    private fun calculator(
        data: QuitDataSet,
        instant: Instant = now,
    ) = QuitCalculator(data, instant, MADRID)

    @Test
    fun `clean time counts from the start when there are no slips`() {
        val habit = tobacco(at("2026-03-03", "12:00"))

        val progress = calculator(QuitDataSet(habits = listOf(habit))).progress(habit)

        assertEquals(7 * 86_400L, progress.cleanSeconds)
        assertEquals(7, progress.cleanDays)
        assertEquals(progress.cleanSeconds, progress.bestStreakSeconds)
        assertFalse(progress.hasSlipped)
    }

    @Test
    fun `a slip restarts the current streak and is counted`() {
        val habit = tobacco(at("2026-03-01", "09:00"))
        val slipAt = at("2026-03-08", "09:00")
        val data = QuitDataSet(habits = listOf(habit), consumptions = listOf(slip(habit.id, slipAt)))

        val progress = calculator(data).progress(habit)

        assertEquals(slipAt, progress.cleanSince)
        assertEquals(2 * 86_400L + 3 * 3_600L, progress.cleanSeconds)
        assertTrue(progress.hasSlipped)
        assertEquals(1, progress.slips)
        assertEquals(slipAt, progress.lastConsumptionAt)
    }

    @Test
    fun `the record survives when the current streak is shorter`() {
        val habit = tobacco(at("2026-01-01", "09:00"))
        val data = QuitDataSet(habits = listOf(habit), consumptions = listOf(slip(habit.id, at("2026-03-05", "09:00"))))

        val progress = calculator(data).progress(habit)

        // Del 1 de enero al 5 de marzo hay 31 + 28 + 4 = 63 días.
        assertEquals(63 * 86_400L, progress.bestStreakSeconds)
        assertEquals(63, progress.bestStreakDays)
        assertEquals(5 * 86_400L + 3 * 3_600L, progress.cleanSeconds)
        assertTrue(progress.cleanSeconds < progress.bestStreakSeconds)
    }

    @Test
    fun `total clean days drop only the days with a slip`() {
        val habit = tobacco(at("2026-02-01", "08:00"))
        val data =
            QuitDataSet(
                habits = listOf(habit),
                consumptions =
                    listOf(slip(habit.id, at("2026-02-10", "23:30")), slip(habit.id, at("2026-02-10", "23:50"))),
            )

        val progress = calculator(data).progress(habit)

        // Del 1 de febrero al 9 de marzo hay 37 días completos; uno de ellos tuvo consumo (dos veces).
        assertEquals(36, progress.totalCleanDays)
    }

    @Test
    fun `a habit that starts today has no clean days yet and a zero saving`() {
        val habit = tobacco(at("2026-03-10", "11:59"))

        val progress = calculator(QuitDataSet(habits = listOf(habit))).progress(habit)

        assertEquals(0, progress.totalCleanDays)
        assertEquals(60L, progress.cleanSeconds)
        assertEquals(0L, progress.saved.amountMinor)
        assertEquals(MilestoneKey.HOURS_24, progress.nextMilestone?.key)
    }

    @Test
    fun `a habit that starts in the future is not started`() {
        val habit = tobacco(at("2026-03-11", "08:00"))

        val progress = calculator(QuitDataSet(habits = listOf(habit))).progress(habit)

        assertFalse(progress.started)
        assertEquals(0L, progress.cleanSeconds)
        assertEquals(0L, progress.saved.amountMinor)
    }

    @Test
    fun `savings are the avoided units times the unit price`() {
        // 10 días sin 20 cigarrillos al día = 200 cigarrillos a 0,25 euros cada uno = 50 euros.
        val habit = tobacco(at("2026-02-28", "12:00"))

        val progress = calculator(QuitDataSet(habits = listOf(habit))).progress(habit)

        assertEquals(5_000L, progress.saved.amountMinor)
        assertEquals(200L, progress.avoidedUnits)
    }

    @Test
    fun `a slip subtracts what was consumed from the savings but never goes below zero`() {
        val habit = tobacco(at("2026-03-09", "12:00"))
        val smoked = QuitDataSet(listOf(habit), consumptions = listOf(slip(habit.id, at("2026-03-09", "20:00"), 4)))
        val binge = QuitDataSet(listOf(habit), consumptions = listOf(slip(habit.id, at("2026-03-09", "20:00"), 500)))

        // Un día completo evita 20 cigarrillos; con 4 consumidos quedan 16 a 0,25 euros = 4 euros.
        assertEquals(400L, calculator(smoked).progress(habit).saved.amountMinor)
        assertEquals(0L, calculator(binge).progress(habit).saved.amountMinor)
    }

    @Test
    fun `huge prices saturate instead of overflowing`() {
        val habit = tobacco(at("2020-01-01", "12:00"), price = Long.MAX_VALUE)

        val progress = calculator(QuitDataSet(habits = listOf(habit))).progress(habit)

        assertEquals(Long.MAX_VALUE, progress.saved.amountMinor)
    }

    @Test
    fun `years of abstinence give days, every milestone and a large saving without errors`() {
        val habit = tobacco(at("2016-03-10", "12:00"))

        val progress = calculator(QuitDataSet(habits = listOf(habit))).progress(habit)

        assertTrue(progress.cleanDays > 3_600)
        assertNull(progress.nextMilestone)
        assertTrue(progress.milestones.all { it.reachedNow })
        assertTrue(progress.saved.amountMinor > 0)
    }

    @Test
    fun `milestones keep what past streaks reached after a slip`() {
        val habit = tobacco(at("2026-01-01", "12:00"))
        val data = QuitDataSet(habits = listOf(habit), consumptions = listOf(slip(habit.id, at("2026-03-09", "18:00"))))

        val progress = calculator(data).progress(habit)

        val week = progress.milestones.first { it.key == MilestoneKey.WEEK_1 }
        val threeMonths = progress.milestones.first { it.key == MilestoneKey.MONTHS_3 }
        assertFalse(week.reachedNow)
        assertTrue(week.reachedEver) // La racha anterior de 67 días ya había llegado a la semana.
        assertFalse(threeMonths.reachedEver)
        assertEquals(MilestoneKey.HOURS_24, progress.nextMilestone?.key)
    }

    @Test
    fun `next milestone is the first one the current streak has not reached`() {
        val habit = tobacco(at("2026-03-07", "12:00"))

        val progress = calculator(QuitDataSet(habits = listOf(habit))).progress(habit)

        assertEquals(MilestoneKey.WEEK_1, progress.nextMilestone?.key)
        assertEquals(4 * 86_400L, progress.nextMilestone?.remainingSeconds)
    }

    @Test
    fun `a milestone reached less than two days ago is recent and an older one is not`() {
        val habit = tobacco(at("2026-03-08", "00:00")) // 60 horas limpias en "now".
        val older = tobacco(at("2026-03-01", "00:00"), id = "older") // 9 días y medio.

        val progress = calculator(QuitDataSet(habits = listOf(habit, older)))
        val recent = progress.progress(habit).milestones
        val long = progress.progress(older).milestones

        assertTrue(recent.first { it.key == MilestoneKey.HOURS_24 }.isRecent(now)) // Hace 36 h.
        assertTrue(recent.first { it.key == MilestoneKey.HOURS_48 }.isRecent(now)) // Hace 12 h.
        assertFalse(recent.first { it.key == MilestoneKey.HOURS_72 }.reachedNow)
        assertFalse(recent.first { it.key == MilestoneKey.HOURS_72 }.isRecent(now))
        assertFalse(long.first { it.key == MilestoneKey.HOURS_24 }.isRecent(now))
    }

    @Test
    fun `reduction mode has no milestones and reports the weekly limit`() {
        val habit = alcohol(at("2026-02-01", "12:00"), limit = 4)
        val data =
            QuitDataSet(
                habits = listOf(habit),
                consumptions =
                    listOf(slip(habit.id, at("2026-03-09", "21:00"), 3), slip(habit.id, at("2026-03-10", "10:00"), 2)),
            )

        val progress = calculator(data).progress(habit)

        assertTrue(progress.milestones.isEmpty())
        assertNull(progress.nextMilestone)
        assertEquals(5, progress.thisWeek?.units)
        assertEquals(false, progress.thisWeek?.compliant)
        assertEquals(0, progress.thisWeek?.remaining)
    }

    @Test
    fun `compliance counts the completed weeks within the limit`() {
        val habit = alcohol(at("2026-01-26", "12:00"), limit = 4)
        // Semanas completas anteriores a la actual (la del 9 de marzo): 9/2, 16/2, 23/2 y 2/3.
        val data =
            QuitDataSet(
                habits = listOf(habit),
                consumptions =
                    listOf(
                        slip(habit.id, at("2026-02-10"), 3),
                        slip(habit.id, at("2026-02-18"), 6), // Semana del 16/2: se pasa del límite.
                        slip(habit.id, at("2026-02-25"), 4),
                        slip(habit.id, at("2026-03-03"), 2),
                    ),
            )

        val progress = calculator(data).progress(habit)

        assertEquals(4, progress.compliance.size)
        assertEquals(3, progress.compliantWeeks)
    }

    @Test
    fun `compliance ignores weeks before the habit existed`() {
        val habit = alcohol(at("2026-03-02", "12:00"), limit = 4)

        val progress = calculator(QuitDataSet(habits = listOf(habit))).progress(habit)

        assertEquals(1, progress.compliance.size)
        assertEquals(1, progress.compliantWeeks)
    }

    @Test
    fun `cravings are grouped by local hour and by trigger`() {
        val habit = tobacco(at("2026-03-01", "08:00"))
        val data =
            QuitDataSet(
                habits = listOf(habit),
                cravings =
                    listOf(
                        craving(habit.id, at("2026-03-02", "09:15"), trigger = CravingTrigger.COFFEE),
                        craving(habit.id, at("2026-03-03", "09:45"), trigger = CravingTrigger.COFFEE),
                        craving(habit.id, at("2026-03-04", "23:30"), trigger = CravingTrigger.STRESS),
                        craving(habit.id, at("2026-03-10", "08:00"), trigger = CravingTrigger.COFFEE),
                    ),
            )

        val progress = calculator(data).progress(habit)

        assertEquals(24, progress.cravingsByHour.size)
        assertEquals(2, progress.cravingsByHour[9])
        assertEquals(1, progress.cravingsByHour[23])
        assertEquals(listOf(0, 0, 1, 2, 0, 0, 0, 1), progress.cravingsByBlock())
        assertEquals(TriggerCount(CravingTrigger.COFFEE, 3), progress.topTriggers.first())
        assertEquals(4, progress.cravingsTotal)
        assertEquals(1, progress.cravingsToday)
    }

    @Test
    fun `a craving around midnight belongs to the local day it happened`() {
        val habit = tobacco(at("2026-03-01", "08:00"))
        val data =
            QuitDataSet(
                habits = listOf(habit),
                cravings =
                    listOf(craving(habit.id, at("2026-03-10", "00:05")), craving(habit.id, at("2026-03-09", "23:55"))),
            )

        val progress = calculator(data).progress(habit)

        assertEquals(1, progress.cravingsToday)
        assertEquals(1, progress.cravingsByHour[0])
        assertEquals(1, progress.cravingsByHour[23])
    }

    @Test
    fun `a slip at midnight on the daylight change day uses real elapsed time`() {
        val habit = tobacco(at("2026-03-20", "12:00"))
        // El 29 de marzo de 2026 se adelanta la hora en Madrid: las 02:00 pasan a ser las 03:00.
        val afterChange = at("2026-03-29", "12:00")
        val data = QuitDataSet(habits = listOf(habit), consumptions = listOf(slip(habit.id, at("2026-03-29", "00:00"))))

        val progress = calculator(data, afterChange).progress(habit)

        assertEquals(at("2026-03-29", "00:00"), progress.cleanSince)
        assertEquals(11 * 3_600L, progress.cleanSeconds) // De las 00:00 a las 12:00 sólo pasan 11 horas reales.
        assertTrue(progress.consumedToday)
    }

    @Test
    fun `events after now are ignored`() {
        val habit = tobacco(at("2026-03-01", "08:00"))
        val data =
            QuitDataSet(
                habits = listOf(habit),
                consumptions = listOf(slip(habit.id, at("2026-03-12", "08:00"))),
                cravings = listOf(craving(habit.id, at("2026-03-12", "08:00"))),
            )

        val overview = calculator(data).overview()

        assertEquals(0, overview.habits.single().slips)
        assertEquals(0, overview.habits.single().cravingsTotal)
        assertTrue(overview.events.isEmpty())
    }

    @Test
    fun `savings curve never decreases and ends at the total`() {
        val habit = tobacco(at("2026-01-01", "12:00"))
        val data = QuitDataSet(habits = listOf(habit), consumptions = listOf(slip(habit.id, at("2026-02-01", "10:00"))))

        val progress = calculator(data).progress(habit)

        assertTrue(progress.savings.size <= QuitRules.SAVINGS_POINTS)
        assertEquals(progress.saved, progress.savings.last().saved)
        progress.savings.zipWithNext().forEach { (a, b) -> assertTrue(b.saved.amountMinor >= a.saved.amountMinor) }
    }

    @Test
    fun `savings curve of a brand new habit has one point`() {
        val habit = tobacco(at("2026-03-10", "08:00"))

        val progress = calculator(QuitDataSet(habits = listOf(habit))).progress(habit)

        assertEquals(1, progress.savings.size)
        assertEquals(date("2026-03-10"), progress.savings.single().date)
    }

    @Test
    fun `calendar marks clean days at full level and slip days dimmed`() {
        val habit = tobacco(at("2026-03-01", "08:00"))
        val data = QuitDataSet(habits = listOf(habit), consumptions = listOf(slip(habit.id, at("2026-03-05", "10:00"))))

        val intensity = calculator(data).progress(habit).intensity

        assertEquals(1f, intensity[date("2026-03-04")])
        assertEquals(QuitRules.CONSUMPTION_DAY_LEVEL, intensity[date("2026-03-05")])
        assertNull(intensity[date("2026-02-28")])
        assertNull(intensity[date("2026-03-11")])
    }

    @Test
    fun `primary habit is the oldest one that is being quit`() {
        val drink = alcohol(at("2026-01-01", "12:00"))
        val smoke = tobacco(at("2026-02-01", "12:00"))

        val summary = calculator(QuitDataSet(habits = listOf(drink, smoke))).todaySummary()

        assertEquals("Tabaco", summary.primaryHabitName)
        assertEquals(HabitMode.QUIT, summary.primaryMode)
        assertNull(summary.weekUnits)
    }

    @Test
    fun `a reduction habit alone becomes the primary one and reports its week`() {
        val drink = alcohol(at("2026-02-01", "12:00"), limit = 4)
        val data = QuitDataSet(habits = listOf(drink), consumptions = listOf(slip(drink.id, at("2026-03-09"), 2)))

        val summary = calculator(data).todaySummary()

        assertEquals(2, summary.weekUnits)
        assertEquals(4, summary.weeklyLimit)
    }

    @Test
    fun `today summary adds the savings of every habit in the same currency`() {
        val smoke = tobacco(at("2026-02-28", "12:00"))
        val drink = alcohol(at("2026-03-03", "12:00"))
        val calculator = calculator(QuitDataSet(habits = listOf(smoke, drink)))
        val expected = calculator.progress(smoke).saved.amountMinor + calculator.progress(drink).saved.amountMinor

        assertEquals(expected, calculator.todaySummary().totalSaved.amountMinor)
        assertTrue(expected > 0)
    }

    @Test
    fun `an empty data set has no data`() {
        val summary = calculator(QuitDataSet()).todaySummary()

        assertFalse(summary.hasData)
        assertNull(summary.nextMilestone)
        assertEquals(0L, summary.totalSaved.amountMinor)
        assertFalse(summary.cravingToday)
    }

    @Test
    fun `series marks clean days after the start and keeps the rest at zero`() {
        val habit = tobacco(at("2026-03-03", "08:00"))
        val data =
            QuitDataSet(
                habits = listOf(habit),
                consumptions = listOf(slip(habit.id, at("2026-03-05", "10:00"), 2)),
                cravings = listOf(craving(habit.id, at("2026-03-04", "10:00"), intensity = 4)),
            )
        val calculator = calculator(data)

        // La semana del 9 de marzo sólo llega hasta hoy (el día 10).
        assertEquals(2, calculator.series(DateRange.containing(DateRangeUnit.WEEK, date("2026-03-10"))).days.size)

        val week = calculator.series(DateRange(DateRangeUnit.WEEK, date("2026-03-02")))
        assertEquals(7, week.days.size)
        assertFalse(week.days.first { it.date == date("2026-03-02") }.tracked)
        assertTrue(week.days.first { it.date == date("2026-03-04") }.clean)
        assertFalse(week.days.first { it.date == date("2026-03-05") }.clean)
        assertEquals(2, week.days.first { it.date == date("2026-03-05") }.consumedUnits)
        assertEquals(4, week.days.first { it.date == date("2026-03-04") }.maxCravingIntensity)
        assertEquals(1, week.cravingCount)
    }

    @Test
    fun `series keeps a day clean when only the reduced habit was used`() {
        val smoke = tobacco(at("2026-03-01", "08:00"))
        val drink = alcohol(at("2026-03-01", "08:00"))
        val data =
            QuitDataSet(
                habits = listOf(smoke, drink),
                consumptions = listOf(slip(drink.id, at("2026-03-05", "21:00"), 2)),
            )

        val series = calculator(data).series(DateRange(DateRangeUnit.WEEK, date("2026-03-02")))

        val day = series.days.first { it.date == date("2026-03-05") }
        assertTrue(day.clean)
        assertEquals(2, day.consumedUnits)
    }
}

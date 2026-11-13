package com.example.lifet.core.domain.quit

import app.cash.turbine.test
import com.example.lifet.core.model.CurrencyCode
import com.example.lifet.core.model.DateRange
import com.example.lifet.core.model.DateRangeUnit
import com.example.lifet.core.model.Money
import com.example.lifet.core.model.quit.BaselinePeriod
import com.example.lifet.core.model.quit.CravingTrigger
import com.example.lifet.core.model.quit.HabitKind
import com.example.lifet.core.model.quit.HabitMode
import com.example.lifet.core.model.quit.MilestoneKey
import com.example.lifet.core.model.quit.QuitConsumption
import com.example.lifet.core.model.quit.QuitCraving
import com.example.lifet.core.model.quit.QuitDataSet
import com.example.lifet.core.model.quit.QuitHabit
import com.example.lifet.core.testing.TestClock
import com.example.lifet.core.testing.quit.InMemoryQuitRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class QuitUseCasesTest {
    private val clock = TestClock.fixed("2026-03-10T11:00:00Z")
    private val zone = clock.zone
    private val today = LocalDate.of(2026, 3, 10)

    private fun at(
        date: String,
        time: String = "12:00",
    ): Instant =
        LocalDate
            .parse(date)
            .atTime(java.time.LocalTime.parse(time))
            .atZone(zone)
            .toInstant()

    private fun tobacco(start: Instant) =
        QuitHabit(
            id = "tobacco",
            kind = HabitKind.TOBACCO,
            name = "Tabaco",
            mode = HabitMode.QUIT,
            startedAt = start,
            zone = zone,
            baselineUnits = 20,
            baselinePeriod = BaselinePeriod.DAY,
            unitsPerPack = 20,
            packPrice = Money(500, CurrencyCode.EUR),
        )

    @Test
    fun `today summary of an unused area has no data`() =
        runTest {
            ObserveQuitTodaySummary(InMemoryQuitRepository(), clock)().test {
                val summary = awaitItem()
                assertFalse(summary.hasData)
                assertNull(summary.primaryHabitName)
                assertEquals(0L, summary.totalSaved.amountMinor)
                assertFalse(summary.cravingToday)
            }
        }

    @Test
    fun `today summary reports the clean time, the saving and the next milestone`() =
        runTest {
            val habit = tobacco(at("2026-03-05", "12:00"))
            val repository = InMemoryQuitRepository(QuitDataSet(habits = listOf(habit)))

            ObserveQuitTodaySummary(repository, clock)().test {
                val summary = awaitItem()
                assertTrue(summary.hasData)
                assertEquals("Tabaco", summary.primaryHabitName)
                assertEquals(5 * 86_400L, summary.cleanSeconds)
                assertEquals(MilestoneKey.WEEK_1, summary.nextMilestone?.key)
                assertTrue(summary.totalSaved.amountMinor > 0)
                assertFalse(summary.cravingToday)
            }
        }

    @Test
    fun `today summary reacts to a craving and to a slip without losing the record`() =
        runTest {
            val habit = tobacco(at("2026-01-01", "12:00"))
            val repository = InMemoryQuitRepository(QuitDataSet(habits = listOf(habit)))

            ObserveQuitTodaySummary(repository, clock)().test {
                val before = awaitItem()
                repository.saveCraving(
                    QuitCraving("c", habit.id, at("2026-03-10", "09:00"), zone, 3, CravingTrigger.COFFEE),
                )
                assertTrue(awaitItem().cravingToday)
                repository.saveConsumption(QuitConsumption("s", habit.id, at("2026-03-10", "09:30"), zone, 1))
                val after = awaitItem()
                assertTrue(after.consumedToday)
                assertEquals(2 * 3_600L + 1_800L, after.cleanSeconds) // Desde las 09:30 hasta las 12:00.
                assertTrue(after.cleanSeconds < before.cleanSeconds)
                // El récord es la racha larga de antes de la recaída: 68 días menos las 2,5 h de hoy.
                assertEquals(68 * 86_400L - 9_000L, after.bestStreakSeconds)
            }
        }

    @Test
    fun `series gives one point per day up to today`() =
        runTest {
            val habit = tobacco(at("2026-03-01", "12:00"))
            val repository = InMemoryQuitRepository(QuitDataSet(habits = listOf(habit)))

            ObserveQuitSeries(repository, clock)(DateRange.containing(DateRangeUnit.WEEK, today)).test {
                val series = awaitItem()
                assertEquals(2, series.days.size) // Lunes 9 y martes 10.
                assertTrue(series.days.all { it.clean })
            }
        }

    @Test
    fun `overview lists the events newest first`() =
        runTest {
            val habit = tobacco(at("2026-03-01", "12:00"))
            val repository =
                InMemoryQuitRepository(
                    QuitDataSet(
                        habits = listOf(habit),
                        cravings = listOf(QuitCraving("c", habit.id, at("2026-03-04"), zone, 2, CravingTrigger.STRESS)),
                        consumptions = listOf(QuitConsumption("s", habit.id, at("2026-03-06"), zone, 1)),
                    ),
                )

            ObserveQuitOverview(repository, clock)().test {
                val overview = awaitItem()
                assertEquals(listOf("s", "c"), overview.events.map { it.id })
                assertEquals(habit.id, overview.primaryId)
            }
        }
}

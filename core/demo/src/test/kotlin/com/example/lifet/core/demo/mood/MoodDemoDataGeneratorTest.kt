package com.example.lifet.core.demo.mood

import com.example.lifet.core.demo.DemoContext
import com.example.lifet.core.demo.DemoScenario
import com.example.lifet.core.model.Area
import com.example.lifet.core.model.mood.MoodRules
import com.example.lifet.core.model.mood.MoodStats
import com.example.lifet.core.testing.TestClock
import com.example.lifet.core.testing.mood.FakeMoodRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class MoodDemoDataGeneratorTest {
    private val clock = TestClock.fixed("2026-03-10T15:00:00Z")
    private val today = LocalDate.parse("2026-03-10")

    private suspend fun load(scenario: DemoScenario): FakeMoodRepository {
        val repository = FakeMoodRepository()
        MoodDemoDataGenerator(repository).seed(scenario, DemoContext(clock))
        return repository
    }

    @Test
    fun `area is mood`() {
        assertEquals(Area.MOOD, MoodDemoDataGenerator(FakeMoodRepository()).area)
    }

    @Test
    fun `empty scenario leaves nothing`() =
        runTest {
            val repository = FakeMoodRepository()
            val generator = MoodDemoDataGenerator(repository)
            generator.seed(DemoScenario.REGULAR, DemoContext(clock))

            generator.seed(DemoScenario.EMPTY, DemoContext(clock))

            assertTrue(repository.current.isEmpty())
        }

    @Test
    fun `new user has about three days of data`() =
        runTest {
            val days = load(DemoScenario.NEW_USER).current.map { it.localDate }.toSet()

            assertEquals(setOf(today.minusDays(2), today.minusDays(1), today), days)
        }

    @Test
    fun `seeding twice gives the same data`() =
        runTest {
            val first = load(DemoScenario.REGULAR).current.map { it.copy(id = 0) }
            val second = load(DemoScenario.REGULAR).current.map { it.copy(id = 0) }

            assertEquals(first, second)
        }

    @Test
    fun `regular covers about six months, valid scales, nothing in the future`() =
        runTest {
            val entries = load(DemoScenario.REGULAR).current
            val days = entries.map { it.localDate }.toSet()

            assertTrue("hay datos de al menos 150 días", days.size >= 150)
            assertTrue(entries.all { it.localDate <= today && !it.instant.isAfter(clock.instant()) })
            assertTrue(entries.all { (it.energy ?: 3) in 1..5 && (it.stress ?: 3) in 1..5 })
            assertTrue(entries.any { it.hasDetail } && entries.any { !it.hasDetail })
            assertTrue("hay días con varios check-ins", entries.groupBy { it.localDate }.any { it.value.size > 1 })
        }

    @Test
    fun `regular has the planted weekly cycle, sport effect and low streak`() =
        runTest {
            val entries = load(DemoScenario.REGULAR).current
            val daily = MoodStats.dailyAverages(entries)

            val mondays = daily.filterKeys { it.dayOfWeek == DayOfWeek.MONDAY }.values.average()
            val others = daily.filterKeys { it.dayOfWeek != DayOfWeek.MONDAY && it.dayOfWeek != DayOfWeek.SATURDAY }
            assertTrue("los lunes son más bajos ($mondays vs ${others.values.average()})", mondays < others.values.average() - 0.3)

            val sport = MoodStats.factorImpacts(entries).first { it.factor == "sport" }
            assertTrue("el deporte mejora el ánimo (${sport.delta})", sport.delta > 0.4)

            val summary = MoodStats.todaySummary(entries, today)
            assertTrue("racha baja de ${summary.lowStreakDays} días", summary.lowStreakDays >= MoodRules.LOW_STREAK_DAYS)
            assertTrue(summary.showSupport)
        }

    @Test
    fun `edge cases include extreme content and a gap without data`() =
        runTest {
            val entries = load(DemoScenario.EDGE_CASES).current
            val days = entries.map { it.localDate }.toSet()

            assertTrue(entries.any { it.note.length >= MoodRules.TEXT_MAX_LENGTH })
            assertTrue(entries.any { entry -> entry.factors.any { "🏃" in it } })
            assertTrue(entries.any { it.zone.id == "America/New_York" })
            assertTrue((16L..24L).none { today.minusDays(it) in days })
            assertTrue(entries.groupBy { it.localDate }.any { it.value.size >= 10 })
            assertTrue(entries.none { it.instant.isAfter(clock.instant()) })
        }
}

package com.example.lifet.core.demo.quit

import com.example.lifet.core.demo.DemoContext
import com.example.lifet.core.demo.DemoScenario
import com.example.lifet.core.model.Area
import com.example.lifet.core.model.quit.HabitKind
import com.example.lifet.core.model.quit.HabitMode
import com.example.lifet.core.model.quit.QuitCalculator
import com.example.lifet.core.model.quit.QuitDataSet
import com.example.lifet.core.model.quit.QuitRules
import com.example.lifet.core.testing.TestClock
import com.example.lifet.core.testing.quit.InMemoryQuitRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class QuitDemoDataGeneratorTest {
    private val clock = TestClock.fixed("2026-03-10T15:00:00Z")
    private val today = LocalDate.parse("2026-03-10")

    private suspend fun load(
        scenario: DemoScenario,
        seed: Long = DemoContext.DEFAULT_SEED,
    ): QuitDataSet {
        val repository = InMemoryQuitRepository()
        QuitDemoDataGenerator(repository).seed(scenario, DemoContext(clock, seed))
        return repository.current
    }

    private fun calculator(data: QuitDataSet) = QuitCalculator(data, clock.instant(), clock.zone)

    @Test
    fun `area is quit`() {
        assertEquals(Area.QUIT, QuitDemoDataGenerator(InMemoryQuitRepository()).area)
    }

    @Test
    fun `empty scenario leaves nothing even after a regular load`() =
        runTest {
            val repository = InMemoryQuitRepository()
            val generator = QuitDemoDataGenerator(repository)
            generator.seed(DemoScenario.REGULAR, DemoContext(clock))

            generator.seed(DemoScenario.EMPTY, DemoContext(clock))

            assertTrue(repository.current.habits.isEmpty() && repository.current.cravings.isEmpty())
            assertTrue(repository.current.consumptions.isEmpty())
        }

    @Test
    fun `clear removes everything`() =
        runTest {
            val repository = InMemoryQuitRepository()
            val generator = QuitDemoDataGenerator(repository)
            generator.seed(DemoScenario.REGULAR, DemoContext(clock))

            generator.clear()

            assertEquals(QuitDataSet(), repository.current)
        }

    @Test
    fun `new user has a habit that is about three days old with a few cravings`() =
        runTest {
            val data = load(DemoScenario.NEW_USER)

            val progress = calculator(data).overview().habits.single()
            assertEquals(3, ChronoUnit.DAYS.between(progress.habit.startDate, today))
            assertTrue(progress.cravingsTotal in 4..10)
            assertEquals(0, progress.slips)
            assertTrue(progress.cleanDays >= 3)
        }

    @Test
    fun `regular quit tobacco 47 days ago with a single slip on day 12`() =
        runTest {
            val data = load(DemoScenario.REGULAR)

            val tobacco = data.habits.single { it.kind == HabitKind.TOBACCO }
            assertEquals(47, ChronoUnit.DAYS.between(tobacco.startDate, today))
            val slips = data.consumptions.filter { it.habitId == tobacco.id }
            assertEquals(1, slips.size)
            assertEquals(12, ChronoUnit.DAYS.between(tobacco.startDate, slips.single().date))

            val progress = calculator(data).progress(tobacco)
            // La recaída reinicia la racha pero conserva el récord y los totales.
            // La recaída fue a las 21:40 del día 12 y ahora son las 16:00: 34 días y 18 horas limpios.
            assertEquals(34, progress.cleanDays)
            assertTrue(progress.bestStreakSeconds >= progress.cleanSeconds)
            assertEquals(46, progress.totalCleanDays) // 47 días completos menos el de la recaída.
            assertTrue(progress.saved.amountMinor > 0)
            assertTrue(progress.cravingsTotal > 30)
        }

    @Test
    fun `regular alcohol is in reduction mode and meets the limit three of the last four weeks`() =
        runTest {
            val data = load(DemoScenario.REGULAR)

            val alcohol = data.habits.single { it.kind == HabitKind.ALCOHOL }
            assertEquals(HabitMode.REDUCE, alcohol.mode)
            assertNotNull(alcohol.weeklyLimit)

            val progress = calculator(data).progress(alcohol)
            assertEquals(QuitRules.COMPLIANCE_WEEKS, progress.compliance.size)
            assertEquals(3, progress.compliantWeeks)
        }

    @Test
    fun `regular cravings concentrate in the hours after the coffee and the meals`() =
        runTest {
            val data = load(DemoScenario.REGULAR)
            val tobacco = data.habits.single { it.kind == HabitKind.TOBACCO }

            val byHour = calculator(data).progress(tobacco).cravingsByHour

            assertTrue(byHour.sum() > 0)
            assertTrue(byHour[9] > byHour[3])
            assertEquals(0, byHour.slice(1..5).sum())
        }

    @Test
    fun `regular data is deterministic and depends on the seed`() =
        runTest {
            assertEquals(load(DemoScenario.REGULAR), load(DemoScenario.REGULAR))
            assertTrue(load(DemoScenario.REGULAR) != load(DemoScenario.REGULAR, seed = 7L))
        }

    @Test
    fun `nothing is generated after now or before its habit started`() =
        runTest {
            DemoScenario.entries.forEach { scenario ->
                val data = load(scenario)
                val starts = data.habits.associate { it.id to it.startedAt }

                assertTrue(data.habits.all { it.startedAt <= clock.instant() })
                assertTrue(data.cravings.all { it.at <= clock.instant() && it.at >= starts.getValue(it.habitId) })
                assertTrue(data.consumptions.all { it.at <= clock.instant() && it.at >= starts.getValue(it.habitId) })
            }
        }

    @Test
    fun `every event belongs to an existing habit and ids are unique`() =
        runTest {
            DemoScenario.entries.forEach { scenario ->
                val data = load(scenario)
                val ids = data.habits.map { it.id }.toSet()

                assertTrue(data.cravings.all { it.habitId in ids })
                assertTrue(data.consumptions.all { it.habitId in ids })
                val eventIds = data.cravings.map { it.id } + data.consumptions.map { it.id }
                assertEquals(eventIds.size, eventIds.toSet().size)
            }
        }

    @Test
    fun `values respect the limits of the form`() =
        runTest {
            DemoScenario.entries.forEach { scenario ->
                val data = load(scenario)

                assertTrue(data.cravings.all { it.intensity in QuitRules.MIN_INTENSITY..QuitRules.MAX_INTENSITY })
                assertTrue(data.cravings.all { it.note.length <= QuitRules.NOTE_MAX_LENGTH })
                assertTrue(data.consumptions.all { it.units in 1..QuitRules.MAX_CONSUMPTION_UNITS })
                assertTrue(data.consumptions.all { it.note.length <= QuitRules.NOTE_MAX_LENGTH })
                assertTrue(data.habits.all { it.name.length <= QuitRules.NAME_MAX_LENGTH })
                assertTrue(data.habits.all { it.baselineUnits in 1..QuitRules.MAX_BASELINE_UNITS })
                assertTrue(data.habits.all { it.packPrice.amountMinor in 0..QuitRules.MAX_PACK_PRICE_MINOR })
            }
        }

    @Test
    fun `edge cases cover years of abstinence, a fresh start, huge amounts and long texts`() =
        runTest {
            val data = load(DemoScenario.EDGE_CASES)
            val overview = calculator(data).overview()

            // Años de abstinencia con recaídas recientes (cambio de hora): la racha actual es corta, pero el récord,
            // los días limpios totales y el ahorro conservan los años.
            val years = overview.habits.single { it.habit.id == "edge-quit-years" }
            assertTrue(years.bestStreakDays > 365 * 5)
            assertTrue(years.totalCleanDays > 365 * 5)
            assertTrue(years.cleanSeconds < years.bestStreakSeconds)
            assertTrue(years.saved.amountMinor > 0)

            val fresh = overview.habits.single { it.habit.id == "edge-quit-fresh" }
            assertEquals(0, fresh.totalCleanDays)
            // Con 300 unidades al día, 40 minutos ya evitan algo, pero siguen siendo céntimos.
            assertTrue(fresh.saved.amountMinor in 0..100)
            assertTrue(fresh.cleanSeconds < 3_600)

            val huge = overview.habits.single { it.habit.id == "edge-quit-huge" }
            assertTrue(huge.saved.amountMinor in 0..Long.MAX_VALUE)

            val longName = data.habits.single { it.id == "edge-quit-long" }.name
            // Casi en el límite del formulario, sin pasarse ni partir un emoji por la mitad.
            assertTrue(longName.length in (QuitRules.NAME_MAX_LENGTH - 12)..QuitRules.NAME_MAX_LENGTH)
            assertTrue(data.consumptions.any { it.note.length > QuitRules.NOTE_MAX_LENGTH - 100 })
            assertTrue(
                data.habits
                    .map { it.zone }
                    .toSet()
                    .size > 1,
            )
        }

    @Test
    fun `edge cases include slips at midnight and a storm of cravings in a single day`() =
        runTest {
            val data = load(DemoScenario.EDGE_CASES)

            val midnight = data.consumptions.single { it.id == "edge-slip-midnight" }
            assertEquals(0, midnight.at.atZone(midnight.zone).hour)
            val lastSecond = data.consumptions.single { it.id == "edge-slip-last-second" }
            assertEquals(23, lastSecond.at.atZone(lastSecond.zone).hour)

            val perDay = data.cravings.groupBy { it.habitId to it.date }
            assertTrue(perDay.values.maxOf { it.size } >= 24)
        }
}

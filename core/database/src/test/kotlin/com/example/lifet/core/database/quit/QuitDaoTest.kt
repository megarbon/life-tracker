package com.example.lifet.core.database.quit

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.example.lifet.core.database.LifeTrackerDatabase
import com.example.lifet.core.model.quit.BaselinePeriod
import com.example.lifet.core.model.quit.CopingStrategy
import com.example.lifet.core.model.quit.CravingTrigger
import com.example.lifet.core.model.quit.HabitKind
import com.example.lifet.core.model.quit.HabitMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class QuitDaoTest {
    private lateinit var database: LifeTrackerDatabase
    private lateinit var dao: QuitDao
    private val zone = ZoneId.of("Europe/Madrid")

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room.inMemoryDatabaseBuilder(context, LifeTrackerDatabase::class.java).allowMainThreadQueries().build()
        dao = database.quitDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun habit(
        id: String,
        startedAt: String = "2026-01-01T08:00:00Z",
        limit: Int? = null,
    ) = QuitHabitEntity(
        id = id,
        kind = HabitKind.TOBACCO,
        name = "Tabaco $id",
        mode = if (limit == null) HabitMode.QUIT else HabitMode.REDUCE,
        startedAt = Instant.parse(startedAt),
        zoneId = zone,
        baselineUnits = 20,
        baselinePeriod = BaselinePeriod.DAY,
        unitsPerPack = 20,
        packPriceMinor = 500,
        currency = "EUR",
        weeklyLimit = limit,
    )

    private fun craving(
        id: String,
        habitId: String,
        at: String,
    ) = QuitCravingEntity(
        id = id,
        habitId = habitId,
        occurredAt = Instant.parse(at),
        zoneId = zone,
        intensity = 4,
        cravingTrigger = CravingTrigger.COFFEE,
        strategy = CopingStrategy.BREATHING,
        note = "",
    )

    private fun consumption(
        id: String,
        habitId: String,
        at: String,
    ) = QuitConsumptionEntity(
        id = id,
        habitId = habitId,
        occurredAt = Instant.parse(at),
        zoneId = zone,
        units = 2,
        cravingTrigger = null,
        note = "Sin culpa",
    )

    @Test
    fun `habits come back ordered by start and keep every field`() =
        runTest {
            dao.upsertHabit(habit("late", startedAt = "2026-02-01T08:00:00Z"))
            dao.upsertHabit(habit("early", limit = 4))

            dao.observeHabits().test {
                val habits = awaitItem()
                assertEquals(listOf("early", "late"), habits.map { it.id })
                assertEquals(4, habits.first().weeklyLimit)
                assertEquals(HabitMode.REDUCE, habits.first().mode)
                assertNull(habits.last().weeklyLimit)
                assertEquals(zone, habits.first().zoneId)
            }
        }

    @Test
    fun `upserting a habit keeps its cravings and consumptions`() =
        runTest {
            dao.upsertHabit(habit("h"))
            dao.upsertCraving(craving("c", "h", "2026-01-02T10:00:00Z"))
            dao.upsertConsumption(consumption("s", "h", "2026-01-03T10:00:00Z"))

            dao.upsertHabit(habit("h").copy(name = "Renombrado"))

            assertEquals(
                "Renombrado",
                dao
                    .observeHabits()
                    .first()
                    .single()
                    .name,
            )
            assertEquals(1, dao.observeCravings().first().size)
            assertEquals(1, dao.observeConsumptions().first().size)
        }

    @Test
    fun `events come back newest first`() =
        runTest {
            dao.upsertHabit(habit("h"))
            dao.upsertCravings(
                listOf(craving("old", "h", "2026-01-02T10:00:00Z"), craving("new", "h", "2026-01-09T10:00:00Z")),
            )

            assertEquals(listOf("new", "old"), dao.observeCravings().first().map { it.id })
        }

    @Test
    fun `deleting a habit cascades to its history and undo restores everything`() =
        runTest {
            val habit = habit("h")
            val cravings = listOf(craving("c", "h", "2026-01-02T10:00:00Z"))
            val consumptions = listOf(consumption("s", "h", "2026-01-03T10:00:00Z"))
            dao.restoreHabit(habit, cravings, consumptions)

            dao.deleteHabit("h")

            assertTrue(dao.observeHabits().first().isEmpty())
            assertTrue(dao.observeCravings().first().isEmpty())
            assertTrue(dao.observeConsumptions().first().isEmpty())

            dao.restoreHabit(habit, cravings, consumptions)

            assertEquals(cravings, dao.observeCravings().first())
            assertEquals(consumptions, dao.observeConsumptions().first())
        }

    @Test
    fun `deleting one event leaves the habit and the rest`() =
        runTest {
            dao.upsertHabit(habit("h"))
            dao.upsertCravings(
                listOf(craving("a", "h", "2026-01-02T10:00:00Z"), craving("b", "h", "2026-01-03T10:00:00Z")),
            )

            dao.deleteCraving("a")

            assertEquals(listOf("b"), dao.observeCravings().first().map { it.id })
            assertEquals(1, dao.observeHabits().first().size)
        }

    @Test
    fun `replaceAll swaps the whole content`() =
        runTest {
            dao.upsertHabit(habit("old"))
            dao.upsertCraving(craving("old-c", "old", "2026-01-02T10:00:00Z"))

            dao.replaceAll(
                habits = listOf(habit("new")),
                cravings = listOf(craving("new-c", "new", "2026-01-05T10:00:00Z")),
                consumptions = listOf(consumption("new-s", "new", "2026-01-06T10:00:00Z")),
            )

            assertEquals(listOf("new"), dao.observeHabits().first().map { it.id })
            assertEquals(listOf("new-c"), dao.observeCravings().first().map { it.id })
            assertEquals(listOf("new-s"), dao.observeConsumptions().first().map { it.id })
        }

    @Test
    fun `a cravings with a strategy and trigger round trips through enums`() =
        runTest {
            dao.upsertHabit(habit("h"))
            dao.upsertCraving(craving("c", "h", "2026-01-02T10:00:00Z"))

            val stored = dao.observeCravings().first().single()

            assertEquals(CravingTrigger.COFFEE, stored.cravingTrigger)
            assertEquals(CopingStrategy.BREATHING, stored.strategy)
            assertEquals(4, stored.intensity)
        }
}

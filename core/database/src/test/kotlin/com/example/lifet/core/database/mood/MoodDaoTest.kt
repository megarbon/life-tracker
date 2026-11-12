package com.example.lifet.core.database.mood

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.example.lifet.core.database.LifeTrackerDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class MoodDaoTest {
    private lateinit var database: LifeTrackerDatabase
    private lateinit var dao: MoodDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room.inMemoryDatabaseBuilder(context, LifeTrackerDatabase::class.java).allowMainThreadQueries().build()
        dao = database.moodDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun entity(
        instant: String,
        level: Int = 3,
        id: Long = 0,
        emotions: String = "",
    ) = MoodEntryEntity(
        id = id,
        instant = Instant.parse(instant),
        zone = ZoneId.of("Europe/Madrid"),
        level = level,
        emotions = emotions,
        energy = null,
        stress = null,
        factors = "",
        note = "",
        gratitude = "",
        updatedAt = Instant.parse(instant),
    )

    @Test
    fun `observeAll emits entries newest first and reacts to inserts`() =
        runTest {
            dao.observeAll().test {
                assertEquals(emptyList<MoodEntryEntity>(), awaitItem())
                dao.upsert(entity("2026-03-10T08:00:00Z"))
                assertEquals(1, awaitItem().size)
                dao.upsert(entity("2026-03-10T20:00:00Z", level = 5))
                assertEquals(listOf(5, 3), awaitItem().map { it.level })
            }
        }

    @Test
    fun `observeBetween is half open on the end`() =
        runTest {
            dao.upsert(entity("2026-03-10T00:00:00Z"))
            dao.upsert(entity("2026-03-11T00:00:00Z"))

            dao.observeBetween(Instant.parse("2026-03-10T00:00:00Z"), Instant.parse("2026-03-11T00:00:00Z")).test {
                assertEquals(1, awaitItem().size)
            }
        }

    @Test
    fun `upsert with an existing id replaces the row`() =
        runTest {
            val id = dao.upsert(entity("2026-03-10T08:00:00Z", level = 2))
            dao.upsert(entity("2026-03-10T08:00:00Z", level = 4, id = id))

            assertEquals(4, dao.getById(id)?.level)
        }

    @Test
    fun `delete removes the row and reports it`() =
        runTest {
            val id = dao.upsert(entity("2026-03-10T08:00:00Z"))

            assertEquals(1, dao.deleteById(id))
            assertNull(dao.getById(id))
            assertEquals(0, dao.deleteById(id))
        }

    @Test
    fun `recent tags are returned newest first`() =
        runTest {
            dao.upsert(entity("2026-03-10T08:00:00Z", emotions = "joy"))
            dao.upsert(entity("2026-03-11T08:00:00Z", emotions = "calm,tired"))

            assertEquals(listOf("calm,tired", "joy"), dao.recentEmotions(limit = 10))
        }
}

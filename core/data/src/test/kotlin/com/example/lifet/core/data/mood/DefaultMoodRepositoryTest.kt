package com.example.lifet.core.data.mood

import app.cash.turbine.test
import com.example.lifet.core.common.AppError
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.model.mood.MoodEntry
import com.example.lifet.core.model.mood.MoodLevel
import com.example.lifet.core.testing.TestClock
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class DefaultMoodRepositoryTest {
    private val clock = TestClock.fixed("2026-03-10T09:00:00Z")
    private val zone = ZoneId.of("Europe/Madrid")

    private fun TestScope.repository(dao: FakeMoodDao = FakeMoodDao()) =
        DefaultMoodRepository(dao, clock, StandardTestDispatcher(testScheduler))

    private fun entry(
        instant: String,
        emotions: List<String> = emptyList(),
    ) = MoodEntry(instant = Instant.parse(instant), zone = zone, level = MoodLevel.NEUTRAL, emotions = emotions)

    @Test
    fun `tags and optional fields round trip through the entity`() {
        val entry =
            MoodEntry(
                id = 4,
                instant = Instant.parse("2026-03-10T09:00:00Z"),
                zone = zone,
                level = MoodLevel.GOOD,
                emotions = listOf("joy", "yoga matinal"),
                factors = listOf("sport"),
                energy = 4,
                note = "nota",
            )

        assertEquals(entry, entry.toEntity(updatedAt = Instant.EPOCH).toModel())
    }

    @Test
    fun `a corrupt level is ignored instead of crashing`() {
        val corrupt = entry("2026-03-10T09:00:00Z").toEntity(Instant.EPOCH).copy(level = 9)

        assertNull(corrupt.toModel())
    }

    @Test
    fun `observeBetween keeps only entries whose own local day is in range`() =
        runTest {
            val repository = repository()
            // 23:30 UTC del 9 ya es 10 en Madrid (dentro); 23:30 UTC del 10 es el 11 (fuera).
            val inside = (repository.save(entry("2026-03-09T23:30:00Z")) as AppResult.Success).value
            repository.save(entry("2026-03-10T23:30:00Z"))

            repository
                .observeBetween(LocalDate.parse("2026-03-10"), LocalDate.parse("2026-03-11"), zone)
                .test { assertEquals(listOf(inside), awaitItem().map { it.id }) }
        }

    @Test
    fun `delete reports not found for a missing id and removes an existing one`() =
        runTest {
            val repository = repository()
            val id = (repository.save(entry("2026-03-10T08:00:00Z")) as AppResult.Success).value

            assertTrue(repository.delete(id) is AppResult.Success)
            assertEquals(AppResult.Failure(AppError.NotFound), repository.delete(id))
        }

    @Test
    fun `storage exceptions become typed errors`() =
        runTest {
            val repository = repository(FakeMoodDao(failWrites = true))

            assertEquals(AppResult.Failure(AppError.Storage), repository.save(entry("2026-03-10T08:00:00Z")))
        }

    @Test
    fun `tag history is flattened, deduplicated and newest first`() =
        runTest {
            val repository = repository()
            repository.save(entry("2026-03-09T08:00:00Z", emotions = listOf("joy", "calm")))
            repository.save(entry("2026-03-10T08:00:00Z", emotions = listOf("calm", "tired")))

            val history = (repository.tagHistory() as AppResult.Success).value

            assertEquals(listOf("calm", "tired", "joy"), history.emotions)
        }
}

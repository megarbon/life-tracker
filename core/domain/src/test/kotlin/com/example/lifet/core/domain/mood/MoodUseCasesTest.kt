package com.example.lifet.core.domain.mood

import app.cash.turbine.test
import com.example.lifet.core.common.AppError
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.model.DateRange
import com.example.lifet.core.model.DateRangeUnit
import com.example.lifet.core.model.mood.MoodEntry
import com.example.lifet.core.model.mood.MoodLevel
import com.example.lifet.core.testing.TestClock
import com.example.lifet.core.testing.mood.FakeMoodRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class MoodUseCasesTest {
    private val clock = TestClock.fixed("2026-03-10T09:00:00Z")
    private val zone = clock.zone
    private val today = LocalDate.parse("2026-03-10")

    private fun entry(
        daysAgo: Long,
        level: MoodLevel,
        id: Long = 0,
    ) = MoodEntry(
        id = id,
        instant = today.minusDays(daysAgo).atTime(12, 0).atZone(zone).toInstant(),
        zone = zone,
        level = level,
    )

    @Test
    fun `today summary flags a missing check-in and averages today`() =
        runTest {
            val repository = FakeMoodRepository(listOf(entry(1, MoodLevel.GOOD, id = 1)))
            val observe = ObserveMoodTodaySummary(repository, clock)

            observe().test {
                assertTrue(awaitItem().missingCheckIn)
                repository.save(entry(0, MoodLevel.VERY_GOOD))
                val summary = awaitItem()
                assertFalse(summary.missingCheckIn)
                assertEquals(1, summary.checkInsToday)
                assertEquals(5.0, summary.averageToday ?: 0.0, 0.0001)
                assertEquals(4.5, summary.weekAverage ?: 0.0, 0.0001)
            }
        }

    @Test
    fun `today summary shows support after five low days`() =
        runTest {
            val repository = FakeMoodRepository((1L..5L).map { entry(it, MoodLevel.LOW, id = it) })

            ObserveMoodTodaySummary(repository, clock)().test {
                assertTrue(awaitItem().showSupport)
            }
        }

    @Test
    fun `series spans the range and reports the average`() =
        runTest {
            val range = DateRange.containing(DateRangeUnit.WEEK, today)
            val repository = FakeMoodRepository(listOf(entry(0, MoodLevel.VERY_GOOD, id = 1), entry(1, MoodLevel.GOOD, id = 2)))

            ObserveMoodSeries(repository, clock)(range).test {
                val series = awaitItem()
                assertEquals(7, series.days.size)
                assertEquals(2, series.entryCount)
                assertEquals(4.5, series.average ?: 0.0, 0.0001)
            }
        }

    @Test
    fun `save normalizes tags and trims text`() =
        runTest {
            val repository = FakeMoodRepository()
            val save = SaveMoodEntry(repository, clock)

            val result =
                save(
                    MoodDraft(
                        level = MoodLevel.GOOD,
                        emotions = listOf(" joy ", "joy", ",", "yoga, matinal"),
                        note = "  hola  ",
                    ),
                )

            assertTrue(result is AppResult.Success)
            val saved = repository.current.single()
            assertEquals(listOf("joy", "yoga matinal"), saved.emotions)
            assertEquals("hola", saved.note)
            assertEquals(Instant.parse("2026-03-10T09:00:00Z"), saved.instant)
        }

    @Test
    fun `save rejects energy outside 1 to 5`() =
        runTest {
            val repository = FakeMoodRepository()

            val result = SaveMoodEntry(repository, clock)(MoodDraft(level = MoodLevel.GOOD, energy = 6))

            assertEquals(AppResult.Failure(AppError.Validation(SaveMoodEntry.REASON_SCALE)), result)
            assertTrue(repository.current.isEmpty())
        }

    @Test
    fun `save with an existing id edits instead of duplicating`() =
        runTest {
            val repository = FakeMoodRepository(listOf(entry(0, MoodLevel.LOW, id = 7)))

            SaveMoodEntry(repository, clock)(MoodDraft(level = MoodLevel.GOOD, id = 7))

            assertEquals(listOf(MoodLevel.GOOD), repository.current.map { it.level })
        }

    @Test
    fun `delete then save with the same id restores the entry`() =
        runTest {
            val original = entry(0, MoodLevel.GOOD, id = 3)
            val repository = FakeMoodRepository(listOf(original))

            assertTrue(DeleteMoodEntry(repository)(original) is AppResult.Success)
            assertTrue(repository.current.isEmpty())
            assertEquals(AppResult.Failure(AppError.NotFound), DeleteMoodEntry(repository)(original))

            repository.save(original)
            assertEquals(listOf(original), repository.current)
        }

    @Test
    fun `custom tags exclude the predefined ones`() =
        runTest {
            val repository =
                FakeMoodRepository(
                    listOf(entry(0, MoodLevel.GOOD, id = 1).copy(emotions = listOf("joy", "nostalgia"), factors = listOf("sport", "yoga"))),
                )

            val history = (GetCustomMoodTags(repository)() as AppResult.Success).value

            assertEquals(listOf("nostalgia"), history.emotions)
            assertEquals(listOf("yoga"), history.factors)
        }

    @Test
    fun `observe entry emits null once deleted`() =
        runTest {
            val repository = FakeMoodRepository(listOf(entry(0, MoodLevel.GOOD, id = 1)))

            ObserveMoodEntry(repository)(1).test {
                assertEquals(1L, awaitItem()?.id)
                repository.delete(1)
                assertNull(awaitItem())
            }
        }
}

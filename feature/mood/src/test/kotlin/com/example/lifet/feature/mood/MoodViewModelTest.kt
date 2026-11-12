package com.example.lifet.feature.mood

import app.cash.turbine.test
import com.example.lifet.core.domain.GetTodayUseCase
import com.example.lifet.core.domain.mood.DeleteMoodEntry
import com.example.lifet.core.domain.mood.GetCustomMoodTags
import com.example.lifet.core.domain.mood.ObserveMoodEntries
import com.example.lifet.core.domain.mood.ObserveMoodSeries
import com.example.lifet.core.domain.mood.ObserveMoodTodaySummary
import com.example.lifet.core.domain.mood.SaveMoodEntry
import com.example.lifet.core.model.DateRange
import com.example.lifet.core.model.DateRangeUnit
import com.example.lifet.core.model.mood.MoodEntry
import com.example.lifet.core.model.mood.MoodLevel
import com.example.lifet.core.testing.MainDispatcherRule
import com.example.lifet.core.testing.TestClock
import com.example.lifet.core.testing.mood.FakeMoodRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class MoodViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = TestClock.fixed("2026-03-10T09:00:00Z")
    private val zone = clock.zone
    private val today = LocalDate.parse("2026-03-10")

    private fun entry(
        daysAgo: Long,
        level: MoodLevel,
        id: Long,
        hour: Int = 8,
    ) = MoodEntry(
        id = id,
        instant =
            today
                .minusDays(daysAgo)
                .atTime(hour, 0)
                .atZone(zone)
                .toInstant(),
        zone = zone,
        level = level,
    )

    private fun viewModel(repository: FakeMoodRepository) =
        MoodViewModel(
            observeEntries = ObserveMoodEntries(repository),
            observeTodaySummary = ObserveMoodTodaySummary(repository, clock),
            observeSeries = ObserveMoodSeries(repository, clock),
            saveEntry = SaveMoodEntry(repository, clock),
            deleteEntry = DeleteMoodEntry(repository),
            getCustomTags = GetCustomMoodTags(repository),
            getToday = GetTodayUseCase(clock),
        )

    @Test
    fun `emits Loading and then Empty without data`() =
        runTest {
            viewModel(FakeMoodRepository()).uiState.test {
                assertEquals(MoodUiState.Loading, awaitItem())
                assertEquals(MoodUiState.Empty, awaitItem())
            }
        }

    @Test
    fun `emits Success with summary, journal grouped by day and calendar`() =
        runTest {
            val repository =
                FakeMoodRepository(
                    listOf(
                        entry(0, MoodLevel.GOOD, id = 1),
                        entry(0, MoodLevel.VERY_GOOD, id = 2, hour = 9),
                        entry(1, MoodLevel.LOW, id = 3),
                    ),
                )

            viewModel(repository).uiState.test {
                skipItems(1)
                val success = awaitItem() as MoodUiState.Success

                assertEquals(4.5, success.summary.averageToday ?: 0.0, 0.0001)
                assertEquals(2, success.summary.checkInsToday)
                assertEquals(listOf(today, today.minusDays(1)), success.journal.map { it.date })
                assertEquals(
                    listOf(2L, 1L),
                    success.journal
                        .first()
                        .entries
                        .map { it.id },
                )
                assertEquals(2, success.calendar.loggedDays)
                assertEquals(today.year, success.calendar.range.start.year)
                assertFalse(success.supportVisible)
            }
        }

    @Test
    fun `emits Error when the data cannot be loaded and recovers on retry`() =
        runTest {
            val repository = FakeMoodRepository(listOf(entry(0, MoodLevel.GOOD, id = 1))).apply { failObserve = true }
            val viewModel = viewModel(repository)

            viewModel.uiState.test {
                skipItems(1)
                assertEquals(MoodUiState.Error(R.string.mood_error_load), awaitItem())

                repository.failObserve = false
                viewModel.onAction(MoodAction.Retry)

                assertEquals(MoodUiState.Loading, awaitItem())
                assertTrue(awaitItem() is MoodUiState.Success)
            }
        }

    @Test
    fun `quick check-in saves immediately and offers undo`() =
        runTest {
            val repository = FakeMoodRepository()
            val viewModel = viewModel(repository)

            viewModel.effects.test {
                viewModel.onAction(MoodAction.QuickCheckIn(MoodLevel.GOOD))

                assertEquals(MoodEffect.Undoable(R.string.mood_quick_saved), awaitItem())
                assertEquals(listOf(MoodLevel.GOOD), repository.current.map { it.level })

                viewModel.onAction(MoodAction.Undo)
                mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
                assertTrue(repository.current.isEmpty())
            }
        }

    @Test
    fun `quick check-in reports a storage failure`() =
        runTest {
            val repository = FakeMoodRepository().apply { failWrites = true }

            val viewModel = viewModel(repository)
            viewModel.effects.test {
                viewModel.onAction(MoodAction.QuickCheckIn(MoodLevel.LOW))

                assertEquals(MoodEffect.Message(R.string.mood_error_save), awaitItem())
            }
        }

    @Test
    fun `detailed form cannot be saved without a level and saves with detail`() =
        runTest {
            val repository = FakeMoodRepository()
            val viewModel = viewModel(repository)

            viewModel.effects.test {
                viewModel.onAction(MoodAction.OpenNewForm)
                viewModel.onAction(MoodAction.SaveForm)
                mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
                assertTrue(repository.current.isEmpty())

                viewModel.onAction(
                    MoodAction.UpdateForm {
                        it
                            .copy(level = MoodLevel.VERY_GOOD, note = "  Buen día  ", factorInput = "yoga")
                            .toggleEmotion("joy")
                            .withEnergy(4)
                    },
                )
                viewModel.onAction(MoodAction.SaveForm)

                assertEquals(MoodEffect.Message(R.string.mood_saved, success = true), awaitItem())
                val saved = repository.current.single()
                assertEquals(MoodLevel.VERY_GOOD, saved.level)
                assertEquals(listOf("joy"), saved.emotions)
                assertEquals(listOf("yoga"), saved.factors)
                assertEquals(4, saved.energy)
                assertEquals("Buen día", saved.note)
                assertEquals(MoodOverlay.None, viewModel.overlay.value)
            }
        }

    @Test
    fun `failed save keeps the form open so nothing is lost`() =
        runTest {
            val repository = FakeMoodRepository().apply { failWrites = true }
            val viewModel = viewModel(repository)

            viewModel.effects.test {
                viewModel.onAction(MoodAction.OpenNewForm)
                viewModel.onAction(MoodAction.UpdateForm { it.copy(level = MoodLevel.LOW, note = "texto") })
                viewModel.onAction(MoodAction.SaveForm)

                assertEquals(MoodEffect.Message(R.string.mood_error_save), awaitItem())
                val form = (viewModel.overlay.value as MoodOverlay.Form).state
                assertEquals("texto", form.note)
                assertFalse(form.saving)
            }
        }

    @Test
    fun `editing keeps the entry id and time`() =
        runTest {
            val original = entry(2, MoodLevel.LOW, id = 5)
            val repository = FakeMoodRepository(listOf(original))
            val viewModel = viewModel(repository)

            viewModel.effects.test {
                viewModel.onAction(MoodAction.OpenEdit(original))
                viewModel.onAction(MoodAction.UpdateForm { it.copy(level = MoodLevel.GOOD) })
                viewModel.onAction(MoodAction.SaveForm)

                assertEquals(MoodEffect.Message(R.string.mood_updated, success = true), awaitItem())
                val saved = repository.current.single()
                assertEquals(5L, saved.id)
                assertEquals(original.instant, saved.instant)
                assertEquals(MoodLevel.GOOD, saved.level)
            }
        }

    @Test
    fun `delete offers undo that restores the same entry`() =
        runTest {
            val original = entry(1, MoodLevel.NEUTRAL, id = 9)
            val repository = FakeMoodRepository(listOf(original))
            val viewModel = viewModel(repository)

            viewModel.effects.test {
                viewModel.onAction(MoodAction.OpenDetail(original))
                viewModel.onAction(MoodAction.Delete(original))

                assertEquals(MoodEffect.Undoable(R.string.mood_deleted), awaitItem())
                assertTrue(repository.current.isEmpty())
                assertEquals(MoodOverlay.None, viewModel.overlay.value)

                viewModel.onAction(MoodAction.Undo)
                mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
                assertEquals(original, repository.current.single())
            }
        }

    @Test
    fun `support card appears after a low streak and can be dismissed`() =
        runTest {
            val repository = FakeMoodRepository((1L..5L).map { entry(it, MoodLevel.LOW, id = it) })
            val viewModel = viewModel(repository)

            viewModel.uiState.test {
                skipItems(1)
                assertTrue((awaitItem() as MoodUiState.Success).supportVisible)

                viewModel.onAction(MoodAction.DismissSupport)
                assertFalse((awaitItem() as MoodUiState.Success).supportVisible)
            }
        }

    @Test
    fun `changing the range reloads the series`() =
        runTest {
            val repository = FakeMoodRepository(listOf(entry(0, MoodLevel.GOOD, id = 1)))
            val viewModel = viewModel(repository)

            viewModel.uiState.test {
                skipItems(1)
                val month = (awaitItem() as MoodUiState.Success).series.range
                assertEquals(DateRangeUnit.MONTH, month.unit)

                val week = DateRange.containing(DateRangeUnit.WEEK, today)
                viewModel.onAction(MoodAction.ChangeRange(week))

                assertEquals(week, (awaitItem() as MoodUiState.Success).series.range)
            }
        }

    @Test
    fun `calendar year navigation is bounded by the first entry and today`() =
        runTest {
            val repository = FakeMoodRepository(listOf(entry(80, MoodLevel.GOOD, id = 1)))
            val viewModel = viewModel(repository)

            viewModel.uiState.test {
                skipItems(1)
                val current = (awaitItem() as MoodUiState.Success).calendar
                assertTrue(current.hasPreviousYear)
                assertFalse(current.hasNextYear)

                viewModel.onAction(MoodAction.ChangeYear(today.year - 1))
                val previous = (awaitItem() as MoodUiState.Success).calendar
                assertEquals(today.year - 1, previous.range.start.year)
                assertFalse(previous.hasPreviousYear)
                assertTrue(previous.hasNextYear)
            }
        }

    @Test
    fun `intensity maps 1 to 5 onto 0,1 to 1`() {
        assertEquals(0.1f, moodIntensity(1.0), 0.0001f)
        assertEquals(1f, moodIntensity(5.0), 0.0001f)
        assertEquals(0.55f, moodIntensity(3.0), 0.0001f)
    }
}

package com.example.lifet.feature.dashboard

import app.cash.turbine.test
import com.example.lifet.core.domain.GetTodayUseCase
import com.example.lifet.core.domain.finance.ObserveFinanceTodaySummary
import com.example.lifet.core.domain.mood.ObserveMoodTodaySummary
import com.example.lifet.core.domain.mood.SaveMoodEntry
import com.example.lifet.core.model.mood.MoodLevel
import com.example.lifet.core.testing.MainDispatcherRule
import com.example.lifet.core.testing.TestClock
import com.example.lifet.core.testing.finance.InMemoryFinanceRepository
import com.example.lifet.core.testing.mood.FakeMoodRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.LocalDate

class DashboardViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(
        clock: Clock,
        mood: FakeMoodRepository = FakeMoodRepository(),
    ): DashboardViewModel =
        DashboardViewModel(
            getToday = GetTodayUseCase(clock),
            observeMood = ObserveMoodTodaySummary(mood, clock),
            observeFinance = ObserveFinanceTodaySummary(InMemoryFinanceRepository(), clock),
            saveMood = SaveMoodEntry(mood, clock),
        )

    @Test
    fun `emits Loading and then Success with today's date`() =
        runTest {
            val viewModel = viewModel(TestClock.fixed("2026-03-10T09:00:00Z"))

            viewModel.uiState.test {
                assertEquals(DashboardUiState.Loading, awaitItem())
                val success = awaitItem() as DashboardUiState.Success
                assertEquals(LocalDate.of(2026, 3, 10), success.today)
            }
        }

    @Test
    fun `without data the mood check-in is missing and finance has no data`() =
        runTest {
            val viewModel = viewModel(TestClock.fixed("2026-03-10T09:00:00Z"))

            viewModel.uiState.test {
                skipItems(1)
                val success = awaitItem() as DashboardUiState.Success
                assertTrue(success.mood.missingCheckIn)
                assertFalse(success.finance.hasData)
            }
        }

    @Test
    fun `quick check-in is saved, reflected in the summary and announced`() =
        runTest {
            val viewModel = viewModel(TestClock.fixed("2026-03-10T09:00:00Z"))

            viewModel.uiState.test {
                skipItems(1)
                assertTrue((awaitItem() as DashboardUiState.Success).mood.missingCheckIn)

                viewModel.messages.test {
                    viewModel.checkIn(MoodLevel.GOOD)
                    assertEquals(DashboardMessage.MOOD_SAVED, awaitItem())
                }

                val updated = awaitItem() as DashboardUiState.Success
                assertEquals(1, updated.mood.checkInsToday)
                assertEquals(MoodLevel.GOOD.value.toDouble(), updated.mood.averageToday)
            }
        }

    @Test
    fun `today follows the clock zone`() =
        runTest {
            // 23:30 UTC ya es el día siguiente en Madrid.
            val viewModel = viewModel(TestClock.fixed("2026-03-10T23:30:00Z"))

            viewModel.uiState.test {
                skipItems(1)
                val success = awaitItem() as DashboardUiState.Success
                assertEquals(LocalDate.of(2026, 3, 11), success.today)
            }
        }
}

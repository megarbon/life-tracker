package com.example.lifet.feature.dashboard

import app.cash.turbine.test
import com.example.lifet.core.domain.GetTodayUseCase
import com.example.lifet.core.domain.mood.ObserveMoodTodaySummary
import com.example.lifet.core.testing.MainDispatcherRule
import com.example.lifet.core.testing.TestClock
import com.example.lifet.core.testing.mood.FakeMoodRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class DashboardViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(clock: java.time.Clock): DashboardViewModel =
        DashboardViewModel(GetTodayUseCase(clock), ObserveMoodTodaySummary(FakeMoodRepository(), clock))

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
    fun `without mood entries the check-in is reported as missing`() =
        runTest {
            val viewModel = viewModel(TestClock.fixed("2026-03-10T09:00:00Z"))

            viewModel.uiState.test {
                skipItems(1)
                val success = awaitItem() as DashboardUiState.Success
                assertTrue(success.mood.missingCheckIn)
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

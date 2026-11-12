package com.example.lifet.feature.dashboard

import app.cash.turbine.test
import com.example.lifet.core.domain.GetTodayUseCase
import com.example.lifet.core.testing.MainDispatcherRule
import com.example.lifet.core.testing.TestClock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class DashboardViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `emits Loading and then Success with today's date`() =
        runTest {
            val clock = TestClock.fixed("2026-03-10T09:00:00Z")
            val viewModel = DashboardViewModel(GetTodayUseCase(clock))

            viewModel.uiState.test {
                assertEquals(DashboardUiState.Loading, awaitItem())
                assertEquals(DashboardUiState.Success(LocalDate.of(2026, 3, 10)), awaitItem())
            }
        }

    @Test
    fun `today follows the clock zone`() =
        runTest {
            // 23:30 UTC ya es el día siguiente en Madrid.
            val clock = TestClock.fixed("2026-03-10T23:30:00Z")
            val viewModel = DashboardViewModel(GetTodayUseCase(clock))

            viewModel.uiState.test {
                skipItems(1)
                assertEquals(DashboardUiState.Success(LocalDate.of(2026, 3, 11)), awaitItem())
            }
        }
}

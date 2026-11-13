package com.example.lifet.feature.dashboard

import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.example.lifet.core.domain.GetTodayUseCase
import com.example.lifet.core.domain.ObserveToday
import com.example.lifet.core.domain.finance.ObserveFinanceTodaySummary
import com.example.lifet.core.domain.fitness.ObserveFitnessTodaySummary
import com.example.lifet.core.domain.mood.DeleteMoodEntry
import com.example.lifet.core.domain.mood.ObserveMoodTodaySummary
import com.example.lifet.core.domain.mood.SaveMoodEntry
import com.example.lifet.core.domain.nutrition.AddWater
import com.example.lifet.core.domain.nutrition.ManageDiaryEntries
import com.example.lifet.core.domain.nutrition.ObserveNutritionTodaySummary
import com.example.lifet.core.domain.quit.ObserveQuitTodaySummary
import com.example.lifet.core.domain.work.ObserveWorkTodaySummary
import com.example.lifet.core.model.mood.MoodLevel
import com.example.lifet.core.testing.MainDispatcherRule
import com.example.lifet.core.testing.TestClock
import com.example.lifet.core.testing.finance.InMemoryFinanceRepository
import com.example.lifet.core.testing.fitness.InMemoryFitnessRepository
import com.example.lifet.core.testing.mood.FakeMoodRepository
import com.example.lifet.core.testing.nutrition.FakeNutritionGoalsRepository
import com.example.lifet.core.testing.nutrition.FakeNutritionLogRepository
import com.example.lifet.core.testing.quit.InMemoryQuitRepository
import com.example.lifet.core.testing.work.InMemoryWorkRepository
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

    private class Setup(
        clock: Clock,
        val mood: FakeMoodRepository = FakeMoodRepository(),
        val nutritionLog: FakeNutritionLogRepository = FakeNutritionLogRepository(),
    ) {
        val viewModel =
            DashboardViewModel(
                observers =
                    DashboardObservers(
                        today = ObserveToday(clock),
                        mood = ObserveMoodTodaySummary(mood, clock),
                        finance = ObserveFinanceTodaySummary(InMemoryFinanceRepository(), clock),
                        nutrition =
                            ObserveNutritionTodaySummary(
                                nutritionLog,
                                FakeNutritionGoalsRepository(),
                                GetTodayUseCase(clock),
                            ),
                        fitness = ObserveFitnessTodaySummary(InMemoryFitnessRepository(), clock),
                        quit = ObserveQuitTodaySummary(InMemoryQuitRepository(), clock),
                        work = ObserveWorkTodaySummary(InMemoryWorkRepository(), clock),
                    ),
                saveMood = SaveMoodEntry(mood, clock),
                deleteMood = DeleteMoodEntry(mood),
                addWater = AddWater(nutritionLog, clock),
                diary = ManageDiaryEntries(nutritionLog),
                clock = clock,
            )
    }

    private fun success(state: DashboardUiState): DashboardUiState.Success = state as DashboardUiState.Success

    private fun <T> AreaState<T>.ready(): T = (this as AreaState.Ready<T>).value

    @Test
    fun `emits Loading and then Success with today's date`() =
        runTest {
            val viewModel = Setup(TestClock.fixed("2026-03-10T09:00:00Z")).viewModel

            viewModel.uiState.test {
                assertEquals(DashboardUiState.Loading, awaitItem())
                assertEquals(LocalDate.of(2026, 3, 10), success(awaitItem()).today)
            }
        }

    @Test
    fun `without data the mood check-in is missing and finance has no data`() =
        runTest {
            val viewModel = Setup(TestClock.fixed("2026-03-10T09:00:00Z")).viewModel

            viewModel.uiState.test {
                skipItems(1)
                val state = success(awaitItem())
                assertTrue(state.mood.ready().missingCheckIn)
                assertFalse(state.finance.ready().hasData)
            }
        }

    @Test
    fun `one area failing degrades only its own card and the rest keep working`() =
        runTest {
            val setup = Setup(TestClock.fixed("2026-03-10T09:00:00Z"))
            setup.mood.failObserve = true

            setup.viewModel.uiState.test {
                skipItems(1)
                val state = success(awaitItem())
                assertEquals(AreaState.Unavailable, state.mood)
                assertTrue(state.finance is AreaState.Ready)
                assertTrue(state.nutrition is AreaState.Ready)
                assertTrue(state.work is AreaState.Ready)
            }
        }

    @Test
    fun `a nutrition read failure does not take mood down with it`() =
        runTest {
            val setup = Setup(TestClock.fixed("2026-03-10T09:00:00Z"))
            setup.nutritionLog.failReads = true

            setup.viewModel.uiState.test {
                skipItems(1)
                val state = success(awaitItem())
                assertEquals(AreaState.Unavailable, state.nutrition)
                assertTrue(state.mood is AreaState.Ready)
            }
        }

    @Test
    fun `quick check-in is saved, reflected in the summary and announced with its id for undo`() =
        runTest {
            val setup = Setup(TestClock.fixed("2026-03-10T09:00:00Z"))

            setup.viewModel.uiState.test {
                skipItems(1)
                assertTrue(success(awaitItem()).mood.ready().missingCheckIn)

                setup.viewModel.messages.test {
                    setup.viewModel.checkIn(MoodLevel.GOOD)
                    assertTrue(awaitItem() is DashboardMessage.MoodSaved)
                }

                val updated = success(awaitItem()).mood.ready()
                assertEquals(1, updated.checkInsToday)
                assertEquals(MoodLevel.GOOD.value.toDouble(), updated.averageToday)
            }
        }

    @Test
    fun `undoing a quick check-in removes it again`() =
        runTest {
            val setup = Setup(TestClock.fixed("2026-03-10T09:00:00Z"))

            turbineScope {
                val states = setup.viewModel.uiState.testIn(backgroundScope)
                val messages = setup.viewModel.messages.testIn(backgroundScope)
                states.skipItems(2)

                setup.viewModel.checkIn(MoodLevel.LOW)
                val saved = messages.awaitItem()
                assertEquals(1, success(states.awaitItem()).mood.ready().checkInsToday)

                setup.viewModel.undo(saved)
                assertEquals(0, success(states.awaitItem()).mood.ready().checkInsToday)
                states.cancel()
                messages.cancel()
            }
        }

    @Test
    fun `adding a glass of water is reflected in the summary and can be undone`() =
        runTest {
            val setup = Setup(TestClock.fixed("2026-03-10T09:00:00Z"))

            turbineScope {
                val states = setup.viewModel.uiState.testIn(backgroundScope)
                val messages = setup.viewModel.messages.testIn(backgroundScope)
                states.skipItems(1)
                assertEquals(0, success(states.awaitItem()).nutrition.ready().waterMl)

                setup.viewModel.addGlassOfWater()
                val saved = messages.awaitItem()
                assertTrue(saved is DashboardMessage.WaterSaved)
                assertEquals(250, success(states.awaitItem()).nutrition.ready().waterMl)

                setup.viewModel.undo(saved)
                assertEquals(0, success(states.awaitItem()).nutrition.ready().waterMl)
                states.cancel()
                messages.cancel()
            }
        }

    @Test
    fun `today follows the clock zone`() =
        runTest {
            // 23:30 UTC ya es el día siguiente en Madrid.
            val viewModel = Setup(TestClock.fixed("2026-03-10T23:30:00Z")).viewModel

            viewModel.uiState.test {
                skipItems(1)
                assertEquals(LocalDate.of(2026, 3, 11), success(awaitItem()).today)
            }
        }
}

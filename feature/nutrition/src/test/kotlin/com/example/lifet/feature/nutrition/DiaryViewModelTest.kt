package com.example.lifet.feature.nutrition

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.lifet.core.model.nutrition.MealType
import com.example.lifet.core.testing.MainDispatcherRule
import com.example.lifet.core.testing.nutrition.NutritionTestData
import com.example.lifet.feature.nutrition.diary.DiaryAction
import com.example.lifet.feature.nutrition.diary.DiaryEffect
import com.example.lifet.feature.nutrition.diary.DiaryUiState
import com.example.lifet.feature.nutrition.diary.DiaryViewModel
import com.example.lifet.feature.nutrition.diary.UndoAction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DiaryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val env = NutritionTestEnv()

    private fun viewModel() =
        DiaryViewModel(
            observeDiary = env.observeDiary,
            addWater = env.addWater,
            repeatYesterday = env.repeatYesterday,
            manageEntries = env.manage,
            getToday = env.getToday,
            savedStateHandle = SavedStateHandle(),
        )

    private fun meal(
        id: String,
        grams: Int = 100,
        type: MealType = MealType.BREAKFAST,
        date: java.time.LocalDate = today,
    ) = NutritionTestData.entry(id, NutritionTestData.bread, grams, date, type)

    @Test
    fun `a day without entries is Empty`() =
        runTest {
            viewModel().uiState.test {
                val state = awaitInstance<DiaryUiState.Empty>()

                assertTrue(state.isToday)
                assertEquals(today, state.date)
            }
        }

    @Test
    fun `entries become Success with one section per meal type and the kcal consumed`() =
        runTest {
            env.log.meals.value = listOf(meal("a", type = MealType.BREAKFAST), meal("b", type = MealType.DINNER))

            viewModel().uiState.test {
                val state = awaitInstance<DiaryUiState.Success>()

                assertEquals(MealType.entries, state.sections.map { it.type })
                assertEquals(listOf(1, 0, 1, 0), state.sections.map { it.entries.size })
                assertEquals(530.0, state.consumed.kcal, 1e-6)
                assertEquals(7, state.week.size)
            }
        }

    @Test
    fun `water alone also makes the day Success`() =
        runTest {
            val vm = viewModel()
            vm.onAction(DiaryAction.AddWater)

            vm.uiState.test {
                val state = awaitInstance<DiaryUiState.Success>()

                assertEquals(250, state.waterMl)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a failing read ends in Error and Retry recovers`() =
        runTest {
            env.log.failReads = true
            val vm = viewModel()

            vm.uiState.test {
                awaitInstance<DiaryUiState.Error>()
                env.log.failReads = false
                vm.onAction(DiaryAction.Retry)
                awaitInstance<DiaryUiState.Empty>()
            }
        }

    @Test
    fun `previous day moves back and next day stops at today`() =
        runTest {
            val vm = viewModel()

            vm.onAction(DiaryAction.PreviousDay)
            assertEquals(today.minusDays(1), vm.date.value)
            vm.onAction(DiaryAction.NextDay)
            vm.onAction(DiaryAction.NextDay)
            assertEquals(today, vm.date.value)
            vm.onAction(DiaryAction.PreviousDay)
            vm.onAction(DiaryAction.GoToToday)
            assertEquals(today, vm.date.value)
        }

    @Test
    fun `adding water offers undo that removes the glass`() =
        runTest {
            val vm = viewModel()

            vm.effects.test {
                vm.onAction(DiaryAction.AddWater)
                val effect = awaitItem() as DiaryEffect.ShowUndo
                assertEquals(1, env.log.water.value.size)

                vm.onAction(DiaryAction.Undo(effect.undo))

                assertTrue(
                    env.log.water.value
                        .isEmpty(),
                )
            }
        }

    @Test
    fun `deleting an entry offers undo that restores it`() =
        runTest {
            val entry = meal("a")
            env.log.meals.value = listOf(entry)
            val vm = viewModel()

            vm.effects.test {
                vm.onAction(DiaryAction.Delete(entry))
                val effect = awaitItem() as DiaryEffect.ShowUndo
                assertTrue(
                    env.log.meals.value
                        .isEmpty(),
                )

                vm.onAction(DiaryAction.Undo(effect.undo))

                assertEquals(listOf(entry), env.log.meals.value)
            }
        }

    @Test
    fun `repeat yesterday copies and its undo deletes only the copies`() =
        runTest {
            env.log.meals.value = listOf(meal("old", date = today.minusDays(1)))
            val vm = viewModel()

            vm.effects.test {
                vm.onAction(DiaryAction.RepeatYesterday)
                val effect = awaitItem() as DiaryEffect.ShowUndo
                assertEquals(2, env.log.meals.value.size)

                vm.onAction(DiaryAction.Undo(effect.undo))

                assertEquals(
                    listOf("old"),
                    env.log.meals.value
                        .map { it.id },
                )
            }
        }

    @Test
    fun `repeat yesterday with an empty yesterday reports it`() =
        runTest {
            val vm = viewModel()

            vm.effects.test {
                vm.onAction(DiaryAction.RepeatYesterday)

                val effect = awaitItem() as DiaryEffect.ShowError
                assertEquals(R.string.nutrition_nothing_yesterday, effect.messageRes)
            }
        }

    @Test
    fun `a failed write is reported as an error and not as undo`() =
        runTest {
            env.log.failWrites = true
            val vm = viewModel()

            vm.effects.test {
                vm.onAction(DiaryAction.AddWater)

                assertEquals(R.string.nutrition_error_save, (awaitItem() as DiaryEffect.ShowError).messageRes)
            }
        }

    @Test
    fun `entries added from the detail screen offer undo by id`() =
        runTest {
            val vm = viewModel()

            vm.effects.test {
                vm.onAction(DiaryAction.EntriesAdded(listOf("x")))

                val effect = awaitItem() as DiaryEffect.ShowUndo
                assertEquals(UndoAction.DeleteMeals(listOf("x")), effect.undo)
            }
        }

    @Test
    fun `updating an entry rescales it and can change the meal`() =
        runTest {
            val entry = meal("a", grams = 100)
            env.log.meals.value = listOf(entry)
            val vm = viewModel()

            vm.onAction(DiaryAction.Update(entry, grams = 200, mealType = MealType.DINNER))

            val saved =
                env.log.meals.value
                    .single()
            assertEquals(530.0, saved.nutrients.kcal, 1e-6)
            assertEquals(MealType.DINNER, saved.mealType)
        }
}

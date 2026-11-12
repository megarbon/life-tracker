package com.example.lifet.feature.nutrition

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.lifet.core.model.nutrition.MealType
import com.example.lifet.core.testing.MainDispatcherRule
import com.example.lifet.core.testing.nutrition.NutritionTestData
import com.example.lifet.feature.nutrition.add.QuickAddEvent
import com.example.lifet.feature.nutrition.add.QuickAddViewModel
import com.example.lifet.feature.nutrition.detail.FoodDetailEffect
import com.example.lifet.feature.nutrition.detail.FoodDetailUiState
import com.example.lifet.feature.nutrition.detail.FoodDetailViewModel
import com.example.lifet.feature.nutrition.detail.portionPresets
import com.example.lifet.feature.nutrition.navigation.NavArgs
import com.example.lifet.feature.nutrition.search.SearchUiState
import com.example.lifet.feature.nutrition.search.SearchViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchAndDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val env = NutritionTestEnv()

    private fun search() = SearchViewModel(env.foods, SavedStateHandle())

    private fun detail(foodId: String = NutritionTestData.bread.id) =
        FoodDetailViewModel(
            foods = env.foods,
            logFood = env.logFood,
            savedStateHandle =
                SavedStateHandle(
                    mapOf(
                        NavArgs.FOOD_ID to foodId,
                        NavArgs.MEAL_TYPE to MealType.LUNCH.name,
                        NavArgs.EPOCH_DAY to today.toEpochDay(),
                    ),
                ),
        )

    @Test
    fun `without a query it browses recents favorites and all foods`() =
        runTest {
            env.foods.recentIds.value = listOf(NutritionTestData.apple.id)
            env.foods.setFavorite(NutritionTestData.oil.id, true)

            search().uiState.test {
                val state = awaitInstance<SearchUiState.Browse>()

                assertEquals(listOf(NutritionTestData.apple.id), state.recents.map { it.id })
                assertEquals(listOf(NutritionTestData.oil.id), state.favorites.map { it.id })
                assertEquals(NutritionTestData.foods.size, state.all.size)
            }
        }

    @Test
    fun `typing filters foods case insensitively after the debounce`() =
        runTest {
            val vm = search()

            vm.uiState.test {
                awaitInstance<SearchUiState.Browse>()
                vm.onQueryChange("MANZ")
                advanceTimeBy(DEBOUNCE_PLUS)

                val state = awaitInstance<SearchUiState.Results>()
                assertEquals(listOf(NutritionTestData.apple.id), state.foods.map { it.id })
            }
        }

    @Test
    fun `a query with no matches is NoResults with the typed text`() =
        runTest {
            val vm = search()

            vm.uiState.test {
                awaitInstance<SearchUiState.Browse>()
                vm.onQueryChange("tarta de queso")
                advanceTimeBy(DEBOUNCE_PLUS)

                assertEquals("tarta de queso", awaitInstance<SearchUiState.NoResults>().query)
            }
        }

    @Test
    fun `toggling a favorite flips it in the repository`() =
        runTest {
            val vm = search()

            vm.onToggleFavorite(NutritionTestData.bread)

            assertTrue(
                env.foods.foods.value
                    .first { it.id == NutritionTestData.bread.id }
                    .isFavorite,
            )
        }

    @Test
    fun `detail starts on the usual serving with the meal from the route`() =
        runTest {
            detail().uiState.test {
                val state = awaitInstance<FoodDetailUiState.Success>()

                assertEquals(NutritionTestData.bread.servingGrams, state.grams)
                assertEquals(MealType.LUNCH, state.mealType)
                assertEquals(106.0, state.nutrients.kcal, 1e-6)
            }
        }

    @Test
    fun `changing the grams recalculates the nutrients and invalid text blocks adding`() =
        runTest {
            val vm = detail()

            vm.uiState.test {
                awaitInstance<FoodDetailUiState.Success>()
                vm.onGramsChange("200")
                assertEquals(530.0, awaitInstance<FoodDetailUiState.Success>().nutrients.kcal, 1e-6)

                vm.onGramsChange("0")
                val invalid = awaitInstance<FoodDetailUiState.Success>()
                assertNull(invalid.grams)
                vm.onGramsChange("")
                awaitInstance<FoodDetailUiState.Success>()
                cancelAndIgnoreRemainingEvents()
            }
            vm.onAdd()
            assertTrue(
                env.log.meals.value
                    .isEmpty(),
            )
        }

    @Test
    fun `adding stores the entry in the chosen meal and reports its id`() =
        runTest {
            val vm = detail()

            vm.effects.test {
                vm.uiState.test {
                    awaitInstance<FoodDetailUiState.Success>()
                    vm.onGramsChange("150")
                    awaitInstance<FoodDetailUiState.Success>()
                    vm.onAdd()
                    cancelAndIgnoreRemainingEvents()
                }

                val added = awaitItem() as FoodDetailEffect.Added
                val saved =
                    env.log.meals.value
                        .single()
                assertEquals(added.id, saved.id)
                assertEquals(150, saved.grams)
                assertEquals(MealType.LUNCH, saved.mealType)
                assertEquals(today, saved.date)
            }
        }

    @Test
    fun `an unknown food id is NotFound`() =
        runTest {
            detail("no-existe").uiState.test {
                assertEquals(FoodDetailUiState.NotFound, awaitInstance<FoodDetailUiState.NotFound>())
            }
        }

    @Test
    fun `only custom foods can be deleted from the detail screen`() =
        runTest {
            val vm = detail()
            vm.uiState.test {
                awaitInstance<FoodDetailUiState.Success>()
                vm.onDeleteFood()
                cancelAndIgnoreRemainingEvents()
            }

            assertTrue(
                env.foods.foods.value
                    .any { it.id == NutritionTestData.bread.id },
            )
        }

    @Test
    fun `portion presets include half the usual serving the serving itself double and 100 g`() {
        assertEquals(listOf(20, 40, 80, 100), portionPresets(NutritionTestData.bread))
    }

    @Test
    fun `quick add logs one usual serving of a recent and reports its id`() =
        runTest {
            env.foods.recentIds.value = listOf(NutritionTestData.apple.id)
            val vm = QuickAddViewModel(env.foods, env.logFood, testClock)

            vm.events.test {
                // recents arranca vacío y se rellena al suscribirse: se espera a que haya contenido.
                vm.recents.first { it.isNotEmpty() }
                vm.onRecentPicked(NutritionTestData.apple.name, MealType.SNACK, today)

                val event = awaitItem() as QuickAddEvent.Added
                val saved =
                    env.log.meals.value
                        .single()
                assertEquals(event.id, saved.id)
                assertEquals(NutritionTestData.apple.servingGrams, saved.grams)
                assertEquals(MealType.SNACK, saved.mealType)
            }
        }

    @Test
    fun `quick add default meal follows the clock`() {
        // El reloj de test marca las 10:00 en Madrid.
        assertEquals(MealType.BREAKFAST, QuickAddViewModel(env.foods, env.logFood, testClock).defaultMealType)
    }

    private companion object {
        const val DEBOUNCE_PLUS = 400L
    }
}

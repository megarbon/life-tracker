package com.example.lifet.feature.nutrition

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.lifet.core.model.nutrition.NutritionGoals
import com.example.lifet.core.testing.MainDispatcherRule
import com.example.lifet.core.testing.nutrition.NutritionTestData
import com.example.lifet.feature.nutrition.food.CustomFoodEffect
import com.example.lifet.feature.nutrition.food.CustomFoodForm
import com.example.lifet.feature.nutrition.food.CustomFoodUiState
import com.example.lifet.feature.nutrition.food.CustomFoodViewModel
import com.example.lifet.feature.nutrition.food.toForm
import com.example.lifet.feature.nutrition.food.validate
import com.example.lifet.feature.nutrition.goals.GoalsEffect
import com.example.lifet.feature.nutrition.goals.GoalsForm
import com.example.lifet.feature.nutrition.goals.GoalsUiState
import com.example.lifet.feature.nutrition.goals.GoalsViewModel
import com.example.lifet.feature.nutrition.goals.errors
import com.example.lifet.feature.nutrition.goals.toGoals
import com.example.lifet.feature.nutrition.navigation.NavArgs
import com.example.lifet.feature.nutrition.trends.TrendsUiState
import com.example.lifet.feature.nutrition.trends.TrendsViewModel
import com.example.lifet.feature.nutrition.ui.parseDecimal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FormsAndTrendsTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val env = NutritionTestEnv()

    private val validForm =
        CustomFoodForm(name = "  Bizcocho  ", kcal = "390,5", protein = "6", carbs = "52", fat = "17", serving = "60")

    @Test
    fun `a valid form accepts comma decimals and optional empty fields`() {
        val result = validForm.validate()

        assertFalse(result.errors.hasErrors)
        val food = requireNotNull(result.food)
        assertEquals("Bizcocho", food.name)
        assertEquals(390.5, food.per100g.kcal, 1e-6)
        assertEquals(0.0, food.per100g.fiberG, 1e-6)
        assertEquals(60, food.servingGrams)
        assertTrue(food.isCustom)
    }

    @Test
    fun `empty or invalid fields are reported per field and block the food`() {
        val result = CustomFoodForm(name = " ", kcal = "abc", protein = "", fat = "-3", serving = "0").validate()

        assertNull(result.food)
        assertNotNull(result.errors.name)
        assertNotNull(result.errors.kcal)
        assertNotNull(result.errors.protein)
        assertNotNull(result.errors.fat)
        assertNotNull(result.errors.carbs)
        assertNotNull(result.errors.serving)
        assertNull(result.errors.fiber)
    }

    @Test
    fun `decimal parsing rejects text and infinities`() {
        assertEquals(1.5, parseDecimal("1,5") ?: -1.0, 1e-9)
        assertNull(parseDecimal("1e999"))
        assertNull(parseDecimal("NaN"))
        assertNull(parseDecimal(""))
    }

    @Test
    fun `saving a valid form creates a custom food with a generated id`() =
        runTest {
            val vm = CustomFoodViewModel(env.foods, SavedStateHandle())

            vm.effects.test {
                vm.onFormChange(validForm)
                vm.onSave()

                assertEquals(CustomFoodEffect.Saved, awaitItem())
                val created =
                    env.foods.foods.value
                        .single { it.name == "Bizcocho" }
                assertTrue(created.isCustom)
                assertTrue(created.id.startsWith("custom_"))
            }
        }

    @Test
    fun `saving an invalid form keeps the errors in the state and saves nothing`() =
        runTest {
            val vm = CustomFoodViewModel(env.foods, SavedStateHandle())
            val before = env.foods.foods.value.size

            vm.onSave()

            assertTrue(vm.uiState.value.errors.hasErrors)
            assertEquals(before, env.foods.foods.value.size)
        }

    @Test
    fun `editing loads the food into the form and keeps its id`() =
        runTest {
            val custom = NutritionTestData.apple.copy(id = "custom_1", name = "Mi manzana", isCustom = true)
            env.foods.foods.value = env.foods.foods.value + custom
            val vm = CustomFoodViewModel(env.foods, SavedStateHandle(mapOf(NavArgs.FOOD_ID to "custom_1")))

            val state: CustomFoodUiState = vm.uiState.value
            assertTrue(state.isEditing)
            assertEquals(custom.toForm(), state.form)
            vm.onFormChange(state.form.copy(name = "Mi manzana roja"))
            vm.onSave()

            assertEquals(
                "Mi manzana roja",
                env.foods.foods.value
                    .single { it.id == "custom_1" }
                    .name,
            )
        }

    @Test
    fun `goals load the current values and save valid edits`() =
        runTest {
            val vm = GoalsViewModel(env.goals)

            vm.effects.test {
                val editing = vm.uiState.value as GoalsUiState.Editing
                assertEquals("2000", editing.form.kcal)
                vm.onFormChange(editing.form.copy(kcal = "2300", water = "2500"))
                vm.onSave()

                assertEquals(GoalsEffect.Saved, awaitItem())
                assertEquals(2_300, env.goals.goals.value.kcal)
                assertEquals(2_500, env.goals.goals.value.waterMl)
            }
        }

    @Test
    fun `goals out of range are flagged and not saved`() =
        runTest {
            val vm = GoalsViewModel(env.goals)
            val form = (vm.uiState.value as GoalsUiState.Editing).form

            vm.onFormChange(form.copy(kcal = "100", protein = ""))
            vm.onSave()

            val state = vm.uiState.value as GoalsUiState.Editing
            assertTrue(state.errors.kcal)
            assertTrue(state.errors.protein)
            assertFalse(state.errors.carbs)
            assertEquals(NutritionGoals.DEFAULT, env.goals.goals.value)
        }

    @Test
    fun `reset restores the default goals in the form`() =
        runTest {
            val vm = GoalsViewModel(env.goals)
            val form = (vm.uiState.value as GoalsUiState.Editing).form
            vm.onFormChange(form.copy(kcal = "3000"))

            vm.onReset()

            assertEquals("2000", (vm.uiState.value as GoalsUiState.Editing).form.kcal)
        }

    @Test
    fun `goals form helpers parse numbers and warn about macro energy`() {
        val form = GoalsForm("2000", "100", "250", "65", "2000")

        assertEquals(NutritionGoals.DEFAULT, form.toGoals())
        assertNull(form.copy(fat = "x").toGoals())
        assertFalse(form.errors().any)
        assertTrue(form.copy(water = "99999").errors().water)
    }

    @Test
    fun `typing non digits in a goal is ignored`() =
        runTest {
            val vm = GoalsViewModel(env.goals)
            val form = (vm.uiState.value as GoalsUiState.Editing).form

            vm.onFormChange(form.copy(kcal = "2a0b00"))

            assertEquals("2000", (vm.uiState.value as GoalsUiState.Editing).form.kcal)
        }

    @Test
    fun `trends with fewer than two logged days are Empty`() =
        runTest {
            env.log.meals.value = listOf(NutritionTestData.entry("a", NutritionTestData.bread, 100, today))

            TrendsViewModel(env.observeSeries, env.getToday).uiState.test {
                awaitInstance<TrendsUiState.Empty>()
            }
        }

    @Test
    fun `trends with enough days are Success over 30 days with the macro split`() =
        runTest {
            env.log.meals.value =
                (0L..9L).map { NutritionTestData.entry("m$it", NutritionTestData.chicken, 150, today.minusDays(it)) }

            TrendsViewModel(env.observeSeries, env.getToday).uiState.test {
                val state = awaitInstance<TrendsUiState.Success>()

                assertEquals(30, state.series.days.size)
                assertEquals(10, state.series.loggedDayCount)
                assertTrue(state.macroSplit.protein > state.macroSplit.carbs)
            }
        }

    @Test
    fun `trends error then retry recovers`() =
        runTest {
            env.log.failReads = true
            val vm = TrendsViewModel(env.observeSeries, env.getToday)

            vm.uiState.test {
                awaitInstance<TrendsUiState.Error>()
                env.log.failReads = false
                vm.onRetry()
                awaitInstance<TrendsUiState.Empty>()
            }
        }
}

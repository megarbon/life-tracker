package com.example.lifet.feature.nutrition.detail

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.data.nutrition.FoodRepository
import com.example.lifet.core.domain.nutrition.LogFood
import com.example.lifet.core.model.nutrition.FoodItem
import com.example.lifet.core.model.nutrition.MealEntry
import com.example.lifet.core.model.nutrition.MealType
import com.example.lifet.core.model.nutrition.Nutrients
import com.example.lifet.feature.nutrition.R
import com.example.lifet.feature.nutrition.navigation.NavArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

internal sealed interface FoodDetailUiState {
    data object Loading : FoodDetailUiState

    /** El alimento se borró (o el id es inválido): no hay nada que mostrar. */
    data object NotFound : FoodDetailUiState

    @Immutable
    data class Success(
        val food: FoodItem,
        val mealType: MealType,
        val gramsText: String,
        val grams: Int?,
        val nutrients: Nutrients,
    ) : FoodDetailUiState

    data class Error(
        @param:StringRes val messageRes: Int,
    ) : FoodDetailUiState
}

internal sealed interface FoodDetailEffect {
    /** Se añadió al diario: la pantalla se cierra y el Diario ofrece "Deshacer" con este id. */
    data class Added(
        val id: String,
    ) : FoodDetailEffect

    data class ShowError(
        @param:StringRes val messageRes: Int,
    ) : FoodDetailEffect

    data object Deleted : FoodDetailEffect
}

@HiltViewModel
internal class FoodDetailViewModel
    @Inject
    constructor(
        private val foods: FoodRepository,
        private val logFood: LogFood,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val foodId: String = savedStateHandle[NavArgs.FOOD_ID] ?: ""
        private val date: LocalDate = LocalDate.ofEpochDay(savedStateHandle[NavArgs.EPOCH_DAY] ?: 0L)
        private val initialMeal: MealType =
            MealType.fromName(savedStateHandle[NavArgs.MEAL_TYPE]) ?: MealType.SNACK

        // null = "la ración habitual del alimento"; en cuanto el usuario teclea, manda su texto.
        private val typedGrams = savedStateHandle.getStateFlow<String?>(KEY_GRAMS, null)
        private val mealName = savedStateHandle.getStateFlow(KEY_MEAL, initialMeal.name)

        private val _effects = Channel<FoodDetailEffect>(Channel.BUFFERED)
        val effects: Flow<FoodDetailEffect> = _effects.receiveAsFlow()

        val uiState: StateFlow<FoodDetailUiState> =
            combine(foods.observeFood(foodId), typedGrams, mealName) { food, typed, meal ->
                if (food == null) {
                    FoodDetailUiState.NotFound
                } else {
                    val text = typed ?: food.servingGrams.toString()
                    val grams = parseGrams(text)
                    FoodDetailUiState.Success(
                        food = food,
                        mealType = MealType.fromName(meal) ?: initialMeal,
                        gramsText = text,
                        grams = grams,
                        nutrients = food.nutrientsFor(grams ?: 0),
                    )
                }
            }.catch { emit(FoodDetailUiState.Error(R.string.nutrition_error_generic)) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), FoodDetailUiState.Loading)

        fun onGramsChange(text: String) {
            savedStateHandle[KEY_GRAMS] = text.filter(Char::isDigit).take(MAX_DIGITS)
        }

        fun onMealChange(type: MealType) {
            savedStateHandle[KEY_MEAL] = type.name
        }

        fun onToggleFavorite() {
            val food = (uiState.value as? FoodDetailUiState.Success)?.food ?: return
            viewModelScope.launch { foods.setFavorite(food.id, !food.isFavorite) }
        }

        fun onAdd() {
            val state = uiState.value as? FoodDetailUiState.Success ?: return
            val grams = state.grams ?: return
            viewModelScope.launch {
                when (val result = logFood(state.food, grams, state.mealType, date)) {
                    is AppResult.Success -> _effects.trySend(FoodDetailEffect.Added(result.value.id))
                    is AppResult.Failure -> _effects.trySend(FoodDetailEffect.ShowError(R.string.nutrition_error_save))
                }
            }
        }

        fun onDeleteFood() {
            val food = (uiState.value as? FoodDetailUiState.Success)?.food ?: return
            if (!food.isCustom) return
            viewModelScope.launch {
                when (foods.deleteCustomFood(food.id)) {
                    is AppResult.Success -> _effects.trySend(FoodDetailEffect.Deleted)
                    is AppResult.Failure -> _effects.trySend(FoodDetailEffect.ShowError(R.string.nutrition_error_save))
                }
            }
        }

        private fun parseGrams(text: String): Int? =
            text.toIntOrNull()?.takeIf { it in MealEntry.MIN_GRAMS..MealEntry.MAX_GRAMS }

        private companion object {
            const val KEY_GRAMS = "grams"
            const val KEY_MEAL = "meal"
            const val MAX_DIGITS = 4
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }

/** Raciones rápidas que se ofrecen además de la habitual: media, doble y 100 g de referencia. */
internal fun portionPresets(food: FoodItem): List<Int> =
    listOf(HALF, 1.0, DOUBLE)
        .map { (food.servingGrams * it).toInt() }
        .plus(REFERENCE_GRAMS)
        .filter { it in MealEntry.MIN_GRAMS..MealEntry.MAX_GRAMS }
        .distinct()
        .sorted()

private const val HALF = 0.5
private const val DOUBLE = 2.0
private const val REFERENCE_GRAMS = 100

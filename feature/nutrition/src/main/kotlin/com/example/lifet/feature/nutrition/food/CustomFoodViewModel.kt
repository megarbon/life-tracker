package com.example.lifet.feature.nutrition.food

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.data.nutrition.FoodRepository
import com.example.lifet.feature.nutrition.R
import com.example.lifet.feature.nutrition.navigation.NavArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

internal sealed interface CustomFoodEffect {
    data object Saved : CustomFoodEffect

    data class ShowError(
        @param:StringRes val messageRes: Int,
    ) : CustomFoodEffect
}

/** Estado completo de la pantalla: formulario y errores. */
internal data class CustomFoodUiState(
    val form: CustomFoodForm = CustomFoodForm(),
    val errors: FieldErrors = FieldErrors(),
    val isEditing: Boolean = false,
)

@HiltViewModel
internal class CustomFoodViewModel
    @Inject
    constructor(
        private val foods: FoodRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val editingId: String? = savedStateHandle[NavArgs.FOOD_ID]

        private val _uiState = MutableStateFlow(CustomFoodUiState(isEditing = editingId != null))
        val uiState: StateFlow<CustomFoodUiState> = _uiState.asStateFlow()

        private val _effects = Channel<CustomFoodEffect>(Channel.BUFFERED)
        val effects: Flow<CustomFoodEffect> = _effects.receiveAsFlow()

        init {
            if (editingId != null) {
                viewModelScope.launch {
                    val food = foods.observeFood(editingId).first() ?: return@launch
                    _uiState.update { it.copy(form = food.toForm()) }
                }
            }
        }

        fun onFormChange(form: CustomFoodForm) {
            _uiState.update { it.copy(form = form) }
        }

        fun onSave() {
            val result = _uiState.value.form.validate()
            _uiState.update { it.copy(errors = result.errors) }
            val validated = result.food ?: return
            viewModelScope.launch {
                val food = validated.copy(id = editingId ?: "custom_${UUID.randomUUID()}")
                when (foods.saveCustomFood(food)) {
                    is AppResult.Success -> _effects.trySend(CustomFoodEffect.Saved)
                    is AppResult.Failure -> _effects.trySend(CustomFoodEffect.ShowError(R.string.nutrition_error_save))
                }
            }
        }
    }

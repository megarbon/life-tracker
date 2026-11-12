package com.example.lifet.feature.nutrition.goals

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.data.nutrition.NutritionGoalsRepository
import com.example.lifet.core.model.nutrition.NutritionGoals
import com.example.lifet.feature.nutrition.R
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
import javax.inject.Inject

/** Los cinco objetivos como texto, tal y como los teclea el usuario. */
@Immutable
internal data class GoalsForm(
    val kcal: String = "",
    val protein: String = "",
    val carbs: String = "",
    val fat: String = "",
    val water: String = "",
)

internal sealed interface GoalsUiState {
    data object Loading : GoalsUiState

    @Immutable
    data class Editing(
        val form: GoalsForm,
        val errors: GoalsErrors = GoalsErrors(),
    ) : GoalsUiState {
        /** Energía que suman los macros escritos, para avisar si no cuadra con las kcal; `null` si no son números. */
        val macroKcal: Int?
            get() {
                val goals = form.toGoals() ?: return null
                return goals.macroKcal
            }
    }
}

/** `true` en cada campo cuyo valor está vacío, no es un número o sale del rango permitido. */
@Immutable
internal data class GoalsErrors(
    val kcal: Boolean = false,
    val protein: Boolean = false,
    val carbs: Boolean = false,
    val fat: Boolean = false,
    val water: Boolean = false,
) {
    val any: Boolean get() = kcal || protein || carbs || fat || water
}

internal sealed interface GoalsEffect {
    data object Saved : GoalsEffect

    data class ShowError(
        @param:StringRes val messageRes: Int,
    ) : GoalsEffect
}

internal fun GoalsForm.toGoals(): NutritionGoals? {
    val fields = listOf(kcal, protein, carbs, fat, water).map { it.asInt() }
    return if (fields.all { it != null }) fields.toGoals() else null
}

/** Los cinco valores, en este orden: kcal, proteínas, hidratos, grasas y agua. */
private fun List<Int?>.toGoals(): NutritionGoals {
    val values = map { it ?: 0 }
    return NutritionGoals(
        kcal = values.getOrElse(IDX_KCAL) { 0 },
        proteinG = values.getOrElse(IDX_PROTEIN) { 0 },
        carbsG = values.getOrElse(IDX_CARBS) { 0 },
        fatG = values.getOrElse(IDX_FAT) { 0 },
        waterMl = values.getOrElse(IDX_WATER) { 0 },
    )
}

private const val IDX_KCAL = 0
private const val IDX_PROTEIN = 1
private const val IDX_CARBS = 2
private const val IDX_FAT = 3
private const val IDX_WATER = 4

private fun String.asInt(): Int? = trim().toIntOrNull()

internal fun GoalsForm.errors(): GoalsErrors =
    GoalsErrors(
        kcal = kcal.trim().toIntOrNull() !in NutritionGoals.KCAL_RANGE,
        protein = protein.trim().toIntOrNull() !in NutritionGoals.MACRO_RANGE,
        carbs = carbs.trim().toIntOrNull() !in NutritionGoals.MACRO_RANGE,
        fat = fat.trim().toIntOrNull() !in NutritionGoals.MACRO_RANGE,
        water = water.trim().toIntOrNull() !in NutritionGoals.WATER_RANGE,
    )

internal fun NutritionGoals.toForm(): GoalsForm =
    GoalsForm(kcal.toString(), proteinG.toString(), carbsG.toString(), fatG.toString(), waterMl.toString())

@HiltViewModel
internal class GoalsViewModel
    @Inject
    constructor(
        private val goals: NutritionGoalsRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<GoalsUiState>(GoalsUiState.Loading)
        val uiState: StateFlow<GoalsUiState> = _uiState.asStateFlow()

        private val _effects = Channel<GoalsEffect>(Channel.BUFFERED)
        val effects: Flow<GoalsEffect> = _effects.receiveAsFlow()

        init {
            viewModelScope.launch { _uiState.value = GoalsUiState.Editing(goals.observeGoals().first().toForm()) }
        }

        fun onFormChange(form: GoalsForm) {
            _uiState.update { current -> (current as? GoalsUiState.Editing)?.copy(form = form.digitsOnly()) ?: current }
        }

        fun onReset() {
            _uiState.value = GoalsUiState.Editing(NutritionGoals.DEFAULT.toForm())
        }

        fun onSave() {
            val current = _uiState.value as? GoalsUiState.Editing ?: return
            val errors = current.form.errors()
            _uiState.value = current.copy(errors = errors)
            val parsed = current.form.toGoals()
            if (errors.any || parsed == null) return
            viewModelScope.launch {
                when (goals.setGoals(parsed)) {
                    is AppResult.Success -> _effects.trySend(GoalsEffect.Saved)
                    is AppResult.Failure -> _effects.trySend(GoalsEffect.ShowError(R.string.nutrition_error_save))
                }
            }
        }
    }

private const val MAX_DIGITS = 5

private fun GoalsForm.digitsOnly(): GoalsForm =
    GoalsForm(
        kcal = kcal.filter(Char::isDigit).take(MAX_DIGITS),
        protein = protein.filter(Char::isDigit).take(MAX_DIGITS),
        carbs = carbs.filter(Char::isDigit).take(MAX_DIGITS),
        fat = fat.filter(Char::isDigit).take(MAX_DIGITS),
        water = water.filter(Char::isDigit).take(MAX_DIGITS),
    )

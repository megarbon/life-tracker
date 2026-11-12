package com.example.lifet.feature.nutrition.trends

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifet.core.domain.GetTodayUseCase
import com.example.lifet.core.domain.nutrition.ObserveNutritionSeries
import com.example.lifet.core.model.nutrition.MacroSplit
import com.example.lifet.core.model.nutrition.NutritionSeries
import com.example.lifet.feature.nutrition.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

internal sealed interface TrendsUiState {
    data object Loading : TrendsUiState

    /** Menos de dos días registrados en 30: no hay tendencia que dibujar. */
    data object Empty : TrendsUiState

    @Immutable
    data class Success(
        val series: NutritionSeries,
        val macroSplit: MacroSplit,
    ) : TrendsUiState

    data class Error(
        @param:StringRes val messageRes: Int,
    ) : TrendsUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class TrendsViewModel
    @Inject
    constructor(
        observeSeries: ObserveNutritionSeries,
        getToday: GetTodayUseCase,
    ) : ViewModel() {
        private val retry = MutableStateFlow(0)

        val uiState: StateFlow<TrendsUiState> =
            retry
                .flatMapLatest {
                    val today = getToday()
                    observeSeries(today.minusDays(WINDOW_DAYS - 1), today.plusDays(1))
                }.map { series -> series.toUiState() }
                .catch { emit(TrendsUiState.Error(R.string.nutrition_error_generic)) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), TrendsUiState.Loading)

        fun onRetry() {
            retry.value += 1
        }

        private fun NutritionSeries.toUiState(): TrendsUiState =
            if (loggedDayCount < MIN_LOGGED_DAYS) {
                TrendsUiState.Empty
            } else {
                TrendsUiState.Success(this, averageMacroSplit)
            }

        private companion object {
            const val WINDOW_DAYS = 30L
            const val MIN_LOGGED_DAYS = 2
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }

package com.example.lifet.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifet.core.domain.GetTodayUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
internal class DashboardViewModel
    @Inject
    constructor(
        getToday: GetTodayUseCase,
    ) : ViewModel() {
        // Se leerá de repositorios cuando existan las features; por ahora sólo depende del día actual.
        private val today = MutableStateFlow(getToday())

        val uiState: StateFlow<DashboardUiState> =
            today
                .map<_, DashboardUiState> { DashboardUiState.Success(it) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DashboardUiState.Loading)

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }

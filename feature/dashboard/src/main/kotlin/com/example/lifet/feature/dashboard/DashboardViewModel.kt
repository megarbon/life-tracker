package com.example.lifet.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifet.core.domain.GetTodayUseCase
import com.example.lifet.core.domain.mood.ObserveMoodTodaySummary
import dagger.hilt.android.lifecycle.HiltViewModel
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
        observeMood: ObserveMoodTodaySummary,
    ) : ViewModel() {
        private val today = getToday()

        val uiState: StateFlow<DashboardUiState> =
            observeMood()
                .map<_, DashboardUiState> { mood -> DashboardUiState.Success(today = today, mood = mood) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DashboardUiState.Loading)

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }

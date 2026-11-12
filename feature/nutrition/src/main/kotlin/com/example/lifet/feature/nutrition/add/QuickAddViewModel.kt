package com.example.lifet.feature.nutrition.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.data.nutrition.FoodRepository
import com.example.lifet.core.domain.nutrition.LogFood
import com.example.lifet.core.model.nutrition.FoodItem
import com.example.lifet.core.model.nutrition.MealType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/** Resultado de añadir un reciente desde la hoja rápida. */
internal sealed interface QuickAddEvent {
    data class Added(
        val id: String,
    ) : QuickAddEvent

    data object Failed : QuickAddEvent
}

/** Recientes de la hoja "Añadir al diario": un toque registra una ración habitual del alimento. */
@HiltViewModel
internal class QuickAddViewModel
    @Inject
    constructor(
        foods: FoodRepository,
        private val logFood: LogFood,
        clock: Clock,
    ) : ViewModel() {
        /** Comida probable a esta hora, para dejarla preseleccionada. */
        val defaultMealType: MealType = MealType.forTime(LocalTime.now(clock))

        val recents: StateFlow<ImmutableList<FoodItem>> =
            foods
                .observeRecentFoods(RECENT_LIMIT)
                .map { it.toImmutableList() }
                .catch { emit(persistentListOf()) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), persistentListOf())

        private val _events = Channel<QuickAddEvent>(Channel.BUFFERED)
        val events: Flow<QuickAddEvent> = _events.receiveAsFlow()

        fun onRecentPicked(
            foodName: String,
            mealType: MealType,
            date: LocalDate,
        ) {
            val food = recents.value.firstOrNull { it.name == foodName } ?: return
            viewModelScope.launch {
                val result = logFood(food, food.servingGrams, mealType, date)
                val event =
                    if (result is AppResult.Success) QuickAddEvent.Added(result.value.id) else QuickAddEvent.Failed
                _events.trySend(event)
            }
        }

        private companion object {
            const val RECENT_LIMIT = 8
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }

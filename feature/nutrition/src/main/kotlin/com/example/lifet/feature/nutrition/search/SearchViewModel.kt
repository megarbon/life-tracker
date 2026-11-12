package com.example.lifet.feature.nutrition.search

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifet.core.data.nutrition.FoodRepository
import com.example.lifet.core.model.nutrition.FoodItem
import com.example.lifet.feature.nutrition.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

internal sealed interface SearchUiState {
    data object Loading : SearchUiState

    /** Sin texto: recientes y favoritos arriba y el resto de la base debajo. */
    @Immutable
    data class Browse(
        val recents: ImmutableList<FoodItem>,
        val favorites: ImmutableList<FoodItem>,
        val all: ImmutableList<FoodItem>,
    ) : SearchUiState

    @Immutable
    data class Results(
        val query: String,
        val foods: ImmutableList<FoodItem>,
    ) : SearchUiState

    data class NoResults(
        val query: String,
    ) : SearchUiState

    data class Error(
        @param:StringRes val messageRes: Int,
    ) : SearchUiState
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class SearchViewModel
    @Inject
    constructor(
        private val foods: FoodRepository,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        val query: StateFlow<String> = savedStateHandle.getStateFlow(KEY_QUERY, "")

        val uiState: StateFlow<SearchUiState> =
            query
                .debounce(DEBOUNCE_MS)
                .distinctUntilChanged()
                .flatMapLatest { text -> stateFor(text.trim()) }
                .catch { emit(SearchUiState.Error(R.string.nutrition_error_generic)) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SearchUiState.Loading)

        fun onQueryChange(text: String) {
            savedStateHandle[KEY_QUERY] = text.take(MAX_QUERY)
        }

        fun onToggleFavorite(food: FoodItem) {
            viewModelScope.launch { foods.setFavorite(food.id, !food.isFavorite) }
        }

        private fun stateFor(text: String): Flow<SearchUiState> =
            if (text.isEmpty()) {
                combine(
                    foods.observeRecentFoods(RECENT_LIMIT),
                    foods.observeFavoriteFoods(),
                    foods.searchFoods(""),
                ) { recents, favorites, all ->
                    SearchUiState.Browse(recents.toImmutableList(), favorites.toImmutableList(), all.toImmutableList())
                }
            } else {
                foods.searchFoods(text).map { found ->
                    if (found.isEmpty()) {
                        SearchUiState.NoResults(text)
                    } else {
                        SearchUiState.Results(text, found.toImmutableList())
                    }
                }
            }

        private companion object {
            const val KEY_QUERY = "query"
            const val MAX_QUERY = 60
            const val DEBOUNCE_MS = 200L
            const val RECENT_LIMIT = 8
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }

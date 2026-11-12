package com.example.lifet.feature.nutrition

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.lifet.core.designsystem.preview.LTPreviews
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.model.nutrition.MealType
import com.example.lifet.feature.nutrition.detail.FoodDetailActions
import com.example.lifet.feature.nutrition.detail.FoodDetailCallbacks
import com.example.lifet.feature.nutrition.detail.FoodDetailScreen
import com.example.lifet.feature.nutrition.detail.FoodDetailUiState
import com.example.lifet.feature.nutrition.food.CustomFoodForm
import com.example.lifet.feature.nutrition.food.CustomFoodScreen
import com.example.lifet.feature.nutrition.food.CustomFoodUiState
import com.example.lifet.feature.nutrition.food.validate
import com.example.lifet.feature.nutrition.goals.GoalsScreen
import com.example.lifet.feature.nutrition.goals.GoalsUiState
import com.example.lifet.feature.nutrition.goals.toForm
import com.example.lifet.feature.nutrition.search.SearchCallbacks
import com.example.lifet.feature.nutrition.search.SearchScreen
import com.example.lifet.feature.nutrition.search.SearchUiState
import com.example.lifet.feature.nutrition.trends.TrendsScreen
import com.example.lifet.feature.nutrition.trends.TrendsUiState
import com.example.lifet.feature.nutrition.ui.PreviewData
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

private val searchCallbacks = SearchCallbacks({}, {}, {}, {}, {})

@LTPreviews
@Composable
private fun SearchBrowsePreview() {
    LifeTrackerTheme {
        val state =
            SearchUiState.Browse(
                recents = persistentListOf(PreviewData.bread),
                favorites = persistentListOf(PreviewData.longName),
                all = PreviewData.foods.toImmutableList(),
            )
        SearchScreen(state = state, query = "", mealType = MealType.LUNCH, callbacks = searchCallbacks)
    }
}

@LTPreviews
@Composable
private fun SearchNoResultsPreview() {
    LifeTrackerTheme {
        SearchScreen(SearchUiState.NoResults("tarta de queso"), "tarta de queso", MealType.SNACK, searchCallbacks)
    }
}

@LTPreviews
@Composable
private fun FoodDetailPreview() {
    LifeTrackerTheme {
        val food = PreviewData.paella
        val grams = PreviewData.PORTION_GRAMS
        FoodDetailScreen(
            state = FoodDetailUiState.Success(food, MealType.LUNCH, grams.toString(), grams, food.nutrientsFor(grams)),
            snackbarHostState = remember { SnackbarHostState() },
            callbacks = FoodDetailCallbacks({}, {}, {}),
            actions = FoodDetailActions({}, {}, {}, {}, {}),
        )
    }
}

@LTPreviews
@Composable
private fun CustomFoodErrorsPreview() {
    LifeTrackerTheme {
        val form = CustomFoodForm(name = "", kcal = "abc", serving = "0")
        CustomFoodScreen(
            state = CustomFoodUiState(form = form, errors = form.validate().errors),
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onFormChange = {},
            onSave = {},
        )
    }
}

@LTPreviews
@Composable
private fun GoalsPreview() {
    LifeTrackerTheme {
        GoalsScreen(
            state = GoalsUiState.Editing(PreviewData.goals.toForm()),
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onFormChange = {},
            onReset = {},
            onSave = {},
        )
    }
}

@LTPreviews
@Composable
private fun TrendsPreview() {
    LifeTrackerTheme {
        TrendsScreen(
            state = TrendsUiState.Success(PreviewData.series, PreviewData.series.averageMacroSplit),
            onBack = {},
            onRetry = {},
        )
    }
}

@LTPreviews
@Composable
private fun TrendsEmptyPreview() {
    LifeTrackerTheme { TrendsScreen(state = TrendsUiState.Empty, onBack = {}, onRetry = {}) }
}

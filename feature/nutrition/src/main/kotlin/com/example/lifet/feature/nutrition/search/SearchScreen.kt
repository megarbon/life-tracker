package com.example.lifet.feature.nutrition.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifet.core.designsystem.component.LTEmptyState
import com.example.lifet.core.designsystem.component.LTErrorState
import com.example.lifet.core.designsystem.component.LTLoadingSkeleton
import com.example.lifet.core.designsystem.component.LTSectionHeader
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.theme.MinTouchTarget
import com.example.lifet.core.model.nutrition.FoodItem
import com.example.lifet.core.model.nutrition.MealType
import com.example.lifet.feature.nutrition.R
import com.example.lifet.feature.nutrition.ui.BackButton
import com.example.lifet.feature.nutrition.ui.FabClearance
import com.example.lifet.feature.nutrition.ui.labelRes
import com.example.lifet.feature.nutrition.ui.rememberNutritionFormatter

@Composable
internal fun SearchRoute(
    mealType: MealType,
    onBack: () -> Unit,
    onFoodClick: (FoodItem) -> Unit,
    onCreateFood: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    SearchScreen(
        state = state,
        query = query,
        mealType = mealType,
        callbacks =
            SearchCallbacks(
                onBack = onBack,
                onQueryChange = viewModel::onQueryChange,
                onFoodClick = onFoodClick,
                onToggleFavorite = viewModel::onToggleFavorite,
                onCreateFood = onCreateFood,
            ),
        modifier = modifier,
    )
}

internal class SearchCallbacks(
    val onBack: () -> Unit,
    val onQueryChange: (String) -> Unit,
    val onFoodClick: (FoodItem) -> Unit,
    val onToggleFavorite: (FoodItem) -> Unit,
    val onCreateFood: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchScreen(
    state: SearchUiState,
    query: String,
    mealType: MealType,
    callbacks: SearchCallbacks,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nutrition_search_title, stringResource(mealType.labelRes))) },
                navigationIcon = { BackButton(callbacks.onBack) },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = callbacks.onCreateFood,
                text = { Text(stringResource(R.string.nutrition_create_food)) },
                icon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            )
        },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            SearchField(query, callbacks.onQueryChange)
            SearchBody(state, callbacks)
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = LifeTrackerTheme.spacing.md),
        label = { Text(stringResource(R.string.nutrition_search_hint)) },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Rounded.Clear, contentDescription = stringResource(R.string.nutrition_search_clear))
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    )
}

@Composable
private fun SearchBody(
    state: SearchUiState,
    callbacks: SearchCallbacks,
) {
    when (state) {
        SearchUiState.Loading -> {
            LTLoadingSkeleton()
        }

        is SearchUiState.Error -> {
            LTErrorState(
                message = stringResource(state.messageRes),
                retryLabel = stringResource(R.string.nutrition_retry),
                onRetry = { callbacks.onQueryChange("") },
            )
        }

        is SearchUiState.NoResults -> {
            LTEmptyState(
                icon = Icons.Rounded.Search,
                title = stringResource(R.string.nutrition_no_results_title, state.query),
                message = stringResource(R.string.nutrition_no_results_message),
                actionLabel = stringResource(R.string.nutrition_create_food),
                onAction = callbacks.onCreateFood,
            )
        }

        is SearchUiState.Results -> {
            FoodList(listOf(Section(R.string.nutrition_results, state.foods)), callbacks)
        }

        is SearchUiState.Browse -> {
            FoodList(
                listOf(
                    Section(R.string.nutrition_recent, state.recents),
                    Section(R.string.nutrition_favorites, state.favorites),
                    Section(R.string.nutrition_all_foods, state.all),
                ).filter { it.foods.isNotEmpty() },
                callbacks,
            )
        }
    }
}

private class Section(
    val titleRes: Int,
    val foods: List<FoodItem>,
)

@Composable
private fun FoodList(
    sections: List<Section>,
    callbacks: SearchCallbacks,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = FabClearance),
        verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.xs),
    ) {
        sections.forEach { section ->
            item(key = "header-${section.titleRes}") {
                LTSectionHeader(
                    title = stringResource(section.titleRes),
                    modifier = Modifier.padding(horizontal = LifeTrackerTheme.spacing.sm),
                )
            }
            // La misma comida puede salir en recientes y en "todos": la clave lleva la sección para no repetirse.
            items(section.foods, key = { "${section.titleRes}-${it.id}" }) { food ->
                FoodRow(food, { callbacks.onFoodClick(food) }, { callbacks.onToggleFavorite(food) })
            }
        }
    }
}

@Composable
private fun FoodRow(
    food: FoodItem,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val format = rememberNutritionFormatter()
    val kcal = format.kcal(food.per100g.kcal)
    val description = stringResource(R.string.nutrition_food_row_description, food.name, kcal)
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = MinTouchTarget),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .heightIn(min = MinTouchTarget)
                    .clickable(onClick = onClick)
                    .padding(horizontal = LifeTrackerTheme.spacing.md, vertical = LifeTrackerTheme.spacing.xs)
                    .semantics(mergeDescendants = true) { contentDescription = description },
            verticalArrangement = Arrangement.Center,
        ) {
            Text(food.name, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val detail = stringResource(R.string.nutrition_per_100g, kcal)
            Text(
                text = if (food.isCustom) "$detail · ${stringResource(R.string.nutrition_custom_badge)}" else detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onToggleFavorite) {
            val label =
                stringResource(
                    if (food.isFavorite) R.string.nutrition_favorite_remove else R.string.nutrition_favorite_add,
                    food.name,
                )
            Icon(
                imageVector = if (food.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                contentDescription = label,
                tint =
                    if (food.isFavorite) {
                        LifeTrackerTheme.areaColors.nutrition
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
        }
    }
}

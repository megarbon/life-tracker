package com.example.lifet.feature.nutrition.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifet.core.designsystem.component.LTChipGroup
import com.example.lifet.core.designsystem.component.LTConfirmDialog
import com.example.lifet.core.designsystem.component.LTEmptyState
import com.example.lifet.core.designsystem.component.LTErrorState
import com.example.lifet.core.designsystem.component.LTLoadingSkeleton
import com.example.lifet.core.designsystem.component.LTStatTile
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.theme.MinTouchTarget
import com.example.lifet.core.model.nutrition.MealType
import com.example.lifet.feature.nutrition.R
import com.example.lifet.feature.nutrition.ui.BackButton
import com.example.lifet.feature.nutrition.ui.labelRes
import com.example.lifet.feature.nutrition.ui.rememberEffectMessages
import com.example.lifet.feature.nutrition.ui.rememberNutritionFormatter

internal class FoodDetailCallbacks(
    val onBack: () -> Unit,
    val onAdded: (id: String) -> Unit,
    val onEdit: (foodId: String) -> Unit,
)

@Composable
internal fun FoodDetailRoute(
    callbacks: FoodDetailCallbacks,
    modifier: Modifier = Modifier,
    viewModel: FoodDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val messages = rememberEffectMessages(R.string.nutrition_error_save)
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is FoodDetailEffect.Added -> callbacks.onAdded(effect.id)
                FoodDetailEffect.Deleted -> callbacks.onBack()
                is FoodDetailEffect.ShowError -> snackbar.showSnackbar(messages.get(effect.messageRes))
            }
        }
    }
    FoodDetailScreen(
        state = state,
        snackbarHostState = snackbar,
        callbacks = callbacks,
        actions =
            FoodDetailActions(
                onGramsChange = viewModel::onGramsChange,
                onMealChange = viewModel::onMealChange,
                onToggleFavorite = viewModel::onToggleFavorite,
                onAdd = viewModel::onAdd,
                onDeleteFood = viewModel::onDeleteFood,
            ),
        modifier = modifier,
    )
}

internal class FoodDetailActions(
    val onGramsChange: (String) -> Unit,
    val onMealChange: (MealType) -> Unit,
    val onToggleFavorite: () -> Unit,
    val onAdd: () -> Unit,
    val onDeleteFood: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FoodDetailScreen(
    state: FoodDetailUiState,
    snackbarHostState: SnackbarHostState,
    callbacks: FoodDetailCallbacks,
    actions: FoodDetailActions,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nutrition_detail_title)) },
                navigationIcon = { BackButton(callbacks.onBack) },
                actions = { DetailActions(state, callbacks, actions) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            when (state) {
                FoodDetailUiState.Loading -> {
                    LTLoadingSkeleton()
                }

                FoodDetailUiState.NotFound -> {
                    LTEmptyState(
                        icon = Icons.Rounded.Restaurant,
                        title = stringResource(R.string.nutrition_food_not_found),
                        message = stringResource(R.string.nutrition_food_not_found_message),
                        actionLabel = stringResource(R.string.nutrition_back),
                        onAction = callbacks.onBack,
                    )
                }

                is FoodDetailUiState.Error -> {
                    LTErrorState(
                        message = stringResource(state.messageRes),
                        retryLabel = stringResource(R.string.nutrition_retry),
                        onRetry = callbacks.onBack,
                    )
                }

                is FoodDetailUiState.Success -> {
                    DetailContent(state, actions)
                }
            }
        }
    }
}

@Composable
private fun DetailActions(
    state: FoodDetailUiState,
    callbacks: FoodDetailCallbacks,
    actions: FoodDetailActions,
) {
    val success = state as? FoodDetailUiState.Success ?: return
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    IconButton(onClick = actions.onToggleFavorite) {
        val name = success.food.name
        val favorite = success.food.isFavorite
        Icon(
            imageVector = if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
            contentDescription =
                stringResource(
                    if (favorite) R.string.nutrition_favorite_remove else R.string.nutrition_favorite_add,
                    name,
                ),
        )
    }
    if (success.food.isCustom) {
        IconButton(onClick = { callbacks.onEdit(success.food.id) }) {
            Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.nutrition_edit_food))
        }
        IconButton(onClick = { confirmDelete = true }) {
            Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.nutrition_delete_food))
        }
    }
    if (confirmDelete) {
        LTConfirmDialog(
            title = stringResource(R.string.nutrition_delete_food_title),
            message = stringResource(R.string.nutrition_delete_food_message),
            onConfirm = {
                confirmDelete = false
                actions.onDeleteFood()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun DetailContent(
    state: FoodDetailUiState.Success,
    actions: FoodDetailActions,
) {
    val format = rememberNutritionFormatter()
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(LifeTrackerTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.md),
    ) {
        Text(state.food.name, style = MaterialTheme.typography.headlineSmall)
        NutrientsCard(state, format.kcal(state.nutrients.kcal))
        PortionPicker(state, actions)
        val mealLabels = MealType.entries.associateWith { stringResource(it.labelRes) }
        LTChipGroup(
            options = MealType.entries,
            selected = state.mealType,
            label = { mealLabels.getValue(it) },
            onSelect = actions.onMealChange,
        )
        Button(
            onClick = actions.onAdd,
            enabled = state.grams != null,
            modifier = Modifier.fillMaxWidth().heightIn(min = MinTouchTarget),
        ) { Text(stringResource(R.string.nutrition_add_button)) }
    }
}

@Composable
private fun NutrientsCard(
    state: FoodDetailUiState.Success,
    kcal: String,
) {
    val format = rememberNutritionFormatter()
    val n = state.nutrients
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(LifeTrackerTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm),
        ) {
            Text(
                text = stringResource(R.string.nutrition_nutrients_for, state.grams?.let(format::amount) ?: "—"),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.nutrition_kcal_value, kcal),
                style = MaterialTheme.typography.displaySmall,
                color = LifeTrackerTheme.areaColors.nutrition,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm)) {
                val weight = Modifier.weight(1f)
                MacroTile(stringResource(R.string.nutrition_macro_protein), format.grams(n.proteinG), weight)
                MacroTile(stringResource(R.string.nutrition_macro_carbs), format.grams(n.carbsG), weight)
                MacroTile(stringResource(R.string.nutrition_macro_fat), format.grams(n.fatG), weight)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm)) {
                MacroTile(stringResource(R.string.nutrition_fiber), format.grams(n.fiberG), Modifier.weight(1f))
                MacroTile(stringResource(R.string.nutrition_sugar), format.grams(n.sugarG), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MacroTile(
    label: String,
    grams: String,
    modifier: Modifier,
) {
    LTStatTile(label = label, value = stringResource(R.string.nutrition_grams_value, grams), modifier = modifier)
}

@Composable
private fun PortionPicker(
    state: FoodDetailUiState.Success,
    actions: FoodDetailActions,
) {
    val format = rememberNutritionFormatter()
    val presets = remember(state.food) { portionPresets(state.food) }
    val gramsError: @Composable () -> Unit = { Text(stringResource(R.string.nutrition_grams_error)) }
    Column(verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm)) {
        Text(stringResource(R.string.nutrition_serving), style = MaterialTheme.typography.titleMedium)
        val presetLabels =
            presets.associateWith { grams ->
                if (grams == state.food.servingGrams) {
                    stringResource(R.string.nutrition_serving_one, format.amount(grams))
                } else {
                    stringResource(R.string.nutrition_grams_value, format.amount(grams))
                }
            }
        LTChipGroup(
            options = presets,
            selected = state.grams,
            label = { presetLabels.getValue(it) },
            onSelect = { actions.onGramsChange(it.toString()) },
        )
        OutlinedTextField(
            value = state.gramsText,
            onValueChange = actions.onGramsChange,
            label = { Text(stringResource(R.string.nutrition_grams_label)) },
            isError = state.grams == null,
            supportingText = if (state.grams == null) gramsError else null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

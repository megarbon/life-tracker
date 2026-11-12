package com.example.lifet.feature.nutrition.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifet.core.designsystem.component.LTLoadingSkeleton
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.theme.MinTouchTarget
import com.example.lifet.core.model.nutrition.NutritionGoals
import com.example.lifet.feature.nutrition.R
import com.example.lifet.feature.nutrition.ui.BackButton
import com.example.lifet.feature.nutrition.ui.rememberNutritionFormatter

@Composable
internal fun GoalsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GoalsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                GoalsEffect.Saved -> onBack()
                is GoalsEffect.ShowError -> snackbar.showSnackbar(context.getString(effect.messageRes))
            }
        }
    }
    GoalsScreen(
        state = state,
        snackbarHostState = snackbar,
        onBack = onBack,
        onFormChange = viewModel::onFormChange,
        onReset = viewModel::onReset,
        onSave = viewModel::onSave,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GoalsScreen(
    state: GoalsUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onFormChange: (GoalsForm) -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nutrition_goals_title)) },
                navigationIcon = { BackButton(onBack) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when (state) {
            GoalsUiState.Loading -> {
                LTLoadingSkeleton(Modifier.padding(innerPadding))
            }

            is GoalsUiState.Editing -> {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState())
                            .padding(LifeTrackerTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.md),
                ) {
                    GoalsFields(state, onFormChange)
                    GoalsButtons(onReset, onSave)
                }
            }
        }
    }
}

@Composable
private fun GoalsFields(
    state: GoalsUiState.Editing,
    onChange: (GoalsForm) -> Unit,
) {
    val form = state.form
    val errors = state.errors
    val format = rememberNutritionFormatter()
    Text(
        text = stringResource(R.string.nutrition_goals_intro),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val macros = NutritionGoals.MACRO_RANGE
    GoalField(form.kcal, R.string.nutrition_goals_kcal, errors.kcal, NutritionGoals.KCAL_RANGE) {
        onChange(form.copy(kcal = it))
    }
    GoalField(form.protein, R.string.nutrition_goals_protein, errors.protein, macros) {
        onChange(form.copy(protein = it))
    }
    GoalField(form.carbs, R.string.nutrition_goals_carbs, errors.carbs, macros) { onChange(form.copy(carbs = it)) }
    GoalField(form.fat, R.string.nutrition_goals_fat, errors.fat, macros) { onChange(form.copy(fat = it)) }
    state.macroKcal?.let { macroKcal ->
        Text(
            text = stringResource(R.string.nutrition_goals_macro_hint, format.amount(macroKcal)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    GoalField(form.water, R.string.nutrition_goals_water, errors.water, NutritionGoals.WATER_RANGE) {
        onChange(form.copy(water = it))
    }
}

@Composable
private fun GoalField(
    value: String,
    labelRes: Int,
    isError: Boolean,
    range: IntRange,
    onChange: (String) -> Unit,
) {
    val format = rememberNutritionFormatter()
    val rangeHint: @Composable () -> Unit = {
        Text(stringResource(R.string.nutrition_goals_range, format.amount(range.first), format.amount(range.last)))
    }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(labelRes)) },
        isError = isError,
        supportingText = if (isError) rangeHint else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun GoalsButtons(
    onReset: () -> Unit,
    onSave: () -> Unit,
) {
    Button(onClick = onSave, modifier = Modifier.fillMaxWidth().heightIn(min = MinTouchTarget)) {
        Text(stringResource(R.string.nutrition_save))
    }
    OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth().heightIn(min = MinTouchTarget)) {
        Text(stringResource(R.string.nutrition_goals_reset))
    }
}

package com.example.lifet.feature.nutrition.food

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.theme.MinTouchTarget
import com.example.lifet.feature.nutrition.R
import com.example.lifet.feature.nutrition.ui.BackButton
import com.example.lifet.feature.nutrition.ui.rememberEffectMessages

@Composable
internal fun CustomFoodRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CustomFoodViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val messages = rememberEffectMessages(R.string.nutrition_error_save)
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                CustomFoodEffect.Saved -> onBack()
                is CustomFoodEffect.ShowError -> snackbar.showSnackbar(messages.get(effect.messageRes))
            }
        }
    }
    CustomFoodScreen(
        state = state,
        snackbarHostState = snackbar,
        onBack = onBack,
        onFormChange = viewModel::onFormChange,
        onSave = viewModel::onSave,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CustomFoodScreen(
    state: CustomFoodUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onFormChange: (CustomFoodForm) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    val editing = state.isEditing
                    Text(
                        stringResource(
                            if (editing) R.string.nutrition_custom_title_edit else R.string.nutrition_custom_title_new,
                        ),
                    )
                },
                navigationIcon = { BackButton(onBack) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(LifeTrackerTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.md),
        ) {
            FormFields(state, onFormChange)
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().heightIn(min = MinTouchTarget),
            ) { Text(stringResource(R.string.nutrition_save)) }
        }
    }
}

@Composable
private fun FormFields(
    state: CustomFoodUiState,
    onChange: (CustomFoodForm) -> Unit,
) {
    val form = state.form
    val errors = state.errors
    FormField(form.name, R.string.nutrition_custom_name, errors.name, KeyboardType.Text) {
        onChange(form.copy(name = it.take(MAX_NAME)))
    }
    FormField(form.kcal, R.string.nutrition_custom_kcal, errors.kcal) { onChange(form.copy(kcal = it)) }
    FormField(form.protein, R.string.nutrition_custom_protein, errors.protein) { onChange(form.copy(protein = it)) }
    FormField(form.carbs, R.string.nutrition_custom_carbs, errors.carbs) { onChange(form.copy(carbs = it)) }
    FormField(form.fat, R.string.nutrition_custom_fat, errors.fat) { onChange(form.copy(fat = it)) }
    FormField(form.fiber, R.string.nutrition_custom_fiber, errors.fiber) { onChange(form.copy(fiber = it)) }
    FormField(form.sugar, R.string.nutrition_custom_sugar, errors.sugar) { onChange(form.copy(sugar = it)) }
    FormField(form.serving, R.string.nutrition_custom_serving, errors.serving) { onChange(form.copy(serving = it)) }
}

@Composable
private fun FormField(
    value: String,
    labelRes: Int,
    errorRes: Int?,
    keyboardType: KeyboardType = KeyboardType.Decimal,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(labelRes)) },
        isError = errorRes != null,
        supportingText = errorRes?.let { { Text(stringResource(it)) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
    )
}

private const val MAX_NAME = 80

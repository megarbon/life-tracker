@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.lifet.feature.nutrition.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifet.core.designsystem.component.LTEmptyState
import com.example.lifet.core.designsystem.component.LTErrorState
import com.example.lifet.core.designsystem.component.LTLoadingSkeleton
import com.example.lifet.core.designsystem.component.LTTopAppBar
import com.example.lifet.core.designsystem.component.showUndo
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.model.Area
import com.example.lifet.core.model.nutrition.MealEntry
import com.example.lifet.core.model.nutrition.MealType
import com.example.lifet.core.ui.icon
import com.example.lifet.feature.nutrition.R
import com.example.lifet.feature.nutrition.add.QuickAddActions
import com.example.lifet.feature.nutrition.add.QuickAddSheetRoute
import com.example.lifet.feature.nutrition.ui.rememberEffectMessages
import java.time.LocalDate

@Composable
internal fun DiaryRoute(
    navigation: DiaryNavigation,
    addedIds: List<String>,
    onAddedIdsConsumed: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiaryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val date by viewModel.date.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    val undoLabel = stringResource(R.string.nutrition_undo)
    val messages =
        rememberEffectMessages(
            R.string.nutrition_water_added,
            R.string.nutrition_water_removed,
            R.string.nutrition_repeated,
            R.string.nutrition_entry_deleted,
            R.string.nutrition_entry_added,
            R.string.nutrition_nothing_yesterday,
            R.string.nutrition_error_save,
        )

    LaunchedEffect(addedIds) {
        if (addedIds.isNotEmpty()) {
            viewModel.onAction(DiaryAction.EntriesAdded(addedIds))
            onAddedIdsConsumed()
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is DiaryEffect.ShowUndo -> {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    snackbarHostState.showUndo(messages.get(effect.messageRes), undoLabel) {
                        viewModel.onAction(DiaryAction.Undo(effect.undo))
                    }
                }

                is DiaryEffect.ShowError -> {
                    snackbarHostState.showSnackbar(messages.get(effect.messageRes))
                }
            }
        }
    }
    DiaryScreen(
        state = state,
        date = date,
        snackbarHostState = snackbarHostState,
        navigation = navigation,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@Composable
internal fun DiaryScreen(
    state: DiaryUiState,
    date: LocalDate,
    snackbarHostState: SnackbarHostState,
    navigation: DiaryNavigation,
    onAction: (DiaryAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showQuickAdd by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<MealEntry?>(null) }
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LTTopAppBar(
                title = stringResource(R.string.nutrition_title),
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = navigation.onTrends) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
                            contentDescription = stringResource(R.string.nutrition_trends),
                        )
                    }
                    IconButton(onClick = navigation.onGoals) {
                        Icon(Icons.Rounded.Flag, contentDescription = stringResource(R.string.nutrition_goals))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showQuickAdd = true },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.nutrition_add_food)) },
            )
        },
    ) { innerPadding ->
        DiaryBody(state, date, innerPadding, navigation, onAction, onEdit = { editing = it })
    }
    if (showQuickAdd) {
        val actions =
            QuickAddActions(
                onDismiss = { showQuickAdd = false },
                onSearch = { navigation.onSearch(it, date) },
                onRepeatYesterday = { onAction(DiaryAction.RepeatYesterday) },
                onAddWater = { onAction(DiaryAction.AddWater) },
                onEntriesAdded = { onAction(DiaryAction.EntriesAdded(it)) },
            )
        QuickAddSheetRoute(date = date, actions = actions)
    }
    editing?.let { entry ->
        EditEntryDialog(
            entry = entry,
            onDismiss = { editing = null },
            onSave = { grams, type ->
                onAction(DiaryAction.Update(entry, grams, type))
                editing = null
            },
        )
    }
}

@Composable
private fun DiaryBody(
    state: DiaryUiState,
    date: LocalDate,
    padding: PaddingValues,
    navigation: DiaryNavigation,
    onAction: (DiaryAction) -> Unit,
    onEdit: (MealEntry) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(padding)) {
        val isToday = (state as? DiaryUiState.Success)?.isToday ?: (state as? DiaryUiState.Empty)?.isToday ?: true
        DayNavigator(
            date = date,
            isToday = isToday,
            onPrevious = { onAction(DiaryAction.PreviousDay) },
            onNext = { onAction(DiaryAction.NextDay) },
            onGoToToday = { onAction(DiaryAction.GoToToday) },
            modifier = Modifier.padding(horizontal = LifeTrackerTheme.spacing.xs),
        )
        when (state) {
            DiaryUiState.Loading -> {
                LTLoadingSkeleton()
            }

            is DiaryUiState.Error -> {
                LTErrorState(
                    message = stringResource(state.messageRes),
                    retryLabel = stringResource(R.string.nutrition_retry),
                    onRetry = { onAction(DiaryAction.Retry) },
                )
            }

            is DiaryUiState.Empty -> {
                EmptyDay(state, date, navigation)
            }

            is DiaryUiState.Success -> {
                DiaryContent(state, navigation, onAction, onEdit)
            }
        }
    }
}

@Composable
private fun EmptyDay(
    state: DiaryUiState.Empty,
    date: LocalDate,
    navigation: DiaryNavigation,
) {
    LTEmptyState(
        icon = Area.NUTRITION.icon,
        title =
            stringResource(
                if (state.isToday) R.string.nutrition_empty_today_title else R.string.nutrition_empty_day_title,
            ),
        message = stringResource(R.string.nutrition_empty_message),
        actionLabel = stringResource(R.string.nutrition_add_food),
        onAction = { navigation.onSearch(MealType.BREAKFAST, date) },
    )
}

@Composable
private fun DiaryContent(
    state: DiaryUiState.Success,
    navigation: DiaryNavigation,
    onAction: (DiaryAction) -> Unit,
    onEdit: (MealEntry) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(LifeTrackerTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.md),
    ) {
        item { KcalHero(consumed = state.consumed, goals = state.goals) }
        item { MacroBars(consumed = state.consumed, goals = state.goals) }
        items(state.sections, key = { it.type }) { section ->
            MealSectionCard(
                section = section,
                onAdd = { type -> navigation.onSearch(type, state.date) },
                onEdit = onEdit,
                onDelete = { onAction(DiaryAction.Delete(it)) },
            )
        }
        item {
            WaterCard(
                waterMl = state.waterMl,
                goalMl = state.goals.waterMl,
                canRemove = state.water.isNotEmpty(),
                onAdd = { onAction(DiaryAction.AddWater) },
                onRemoveLast = { onAction(DiaryAction.RemoveLastWater) },
            )
        }
        item {
            OutlinedButton(onClick = { onAction(DiaryAction.RepeatYesterday) }) {
                Text(stringResource(R.string.nutrition_repeat_yesterday))
            }
        }
        item { DiaryWeekChart(state) }
    }
}

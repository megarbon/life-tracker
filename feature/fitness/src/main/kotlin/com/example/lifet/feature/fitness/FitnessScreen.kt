package com.example.lifet.feature.fitness

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifet.core.designsystem.component.LTEmptyState
import com.example.lifet.core.designsystem.component.LTErrorState
import com.example.lifet.core.designsystem.component.LTLoadingSkeleton
import com.example.lifet.core.designsystem.component.LTTopAppBar
import com.example.lifet.core.designsystem.component.showUndo
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.model.Area
import com.example.lifet.core.model.fitness.FitnessOverview
import com.example.lifet.core.ui.icon
import com.example.lifet.feature.fitness.ui.BodyTab
import com.example.lifet.feature.fitness.ui.ExerciseDetailSheet
import com.example.lifet.feature.fitness.ui.ExercisesTab
import com.example.lifet.feature.fitness.ui.HistoryTab
import com.example.lifet.feature.fitness.ui.StepsSheet
import com.example.lifet.feature.fitness.ui.SummaryTab
import com.example.lifet.feature.fitness.ui.WeightSheet
import com.example.lifet.feature.fitness.ui.WorkoutSheet
import kotlinx.coroutines.flow.Flow
import com.example.lifet.core.designsystem.R as DesignR

/** Espacio libre al final de las listas para que el FAB no tape el último elemento. */
private val FAB_CLEARANCE = 88.dp

private const val KG_SCALE = 3

internal enum class FitnessTab(
    @param:StringRes val labelRes: Int,
) {
    SUMMARY(R.string.fitness_tab_summary),
    HISTORY(R.string.fitness_tab_history),
    EXERCISES(R.string.fitness_tab_exercises),
    BODY(R.string.fitness_tab_body),
}

/** Punto de entrada de la ruta de Deporte: conecta el ViewModel con la pantalla sin estado. */
@Composable
internal fun FitnessRoute(
    modifier: Modifier = Modifier,
    viewModel: FitnessViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val editor by viewModel.editor.collectAsStateWithLifecycle()
    FitnessScreen(
        state = state,
        editor = editor,
        effects = viewModel.effects,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

/** Pantalla sin ViewModel: recibe estado y emite [FitnessAction]; así se previsualiza y se prueba sola. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FitnessScreen(
    state: FitnessUiState,
    editor: FitnessEditor?,
    effects: Flow<FitnessEffect>,
    onAction: (FitnessAction) -> Unit,
    modifier: Modifier = Modifier,
    initialTab: FitnessTab = FitnessTab.SUMMARY,
) {
    var tabIndex by rememberSaveable { mutableIntStateOf(initialTab.ordinal) }
    val snackbar = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    EffectsHandler(effects, snackbar, onAction)
    val overview = state.overviewOrNull()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { LTTopAppBar(title = stringResource(R.string.fitness_title), scrollBehavior = scrollBehavior) },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (overview != null) {
                ExtendedFloatingActionButton(
                    onClick = { onAction(FitnessAction.OpenWorkout()) },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.fitness_add)) },
                )
            }
        },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            FitnessBody(state, FitnessTab.entries[tabIndex], { tabIndex = it.ordinal }, onAction)
        }
    }
    if (overview != null) EditorHost(overview, editor, onAction)
}

@Composable
private fun FitnessBody(
    state: FitnessUiState,
    tab: FitnessTab,
    onTabChange: (FitnessTab) -> Unit,
    onAction: (FitnessAction) -> Unit,
) {
    when (state) {
        FitnessUiState.Loading -> {
            LTLoadingSkeleton()
        }

        is FitnessUiState.Error -> {
            LTErrorState(
                message = stringResource(state.messageRes),
                retryLabel = stringResource(R.string.fitness_retry),
                onRetry = { onAction(FitnessAction.Retry) },
            )
        }

        is FitnessUiState.Empty -> {
            // Sin entrenos todavía: invita a registrar el primero, pero deja a mano el peso y los pasos.
            if (tab == FitnessTab.BODY) {
                Tabs(state.overview, tab, onTabChange, onAction)
            } else {
                LTEmptyState(
                    icon = Area.FITNESS.icon,
                    title = stringResource(R.string.fitness_empty_title),
                    message = stringResource(R.string.fitness_empty_message),
                    actionLabel = stringResource(R.string.fitness_empty_cta),
                    onAction = { onAction(FitnessAction.OpenWorkout()) },
                )
            }
        }

        is FitnessUiState.Success -> {
            Tabs(state.overview, tab, onTabChange, onAction)
        }
    }
}

@Composable
private fun Tabs(
    overview: FitnessOverview,
    tab: FitnessTab,
    onTabChange: (FitnessTab) -> Unit,
    onAction: (FitnessAction) -> Unit,
) {
    val spacing = LifeTrackerTheme.spacing
    val padding = PaddingValues(start = spacing.md, end = spacing.md, top = spacing.md, bottom = FAB_CLEARANCE)
    PrimaryTabRow(selectedTabIndex = tab.ordinal) {
        FitnessTab.entries.forEach { option ->
            Tab(
                selected = option == tab,
                onClick = { onTabChange(option) },
                text = { Text(stringResource(option.labelRes)) },
            )
        }
    }
    when (tab) {
        FitnessTab.SUMMARY -> {
            SummaryTab(
                overview = overview,
                contentPadding = padding,
                onRepeat = { onAction(FitnessAction.OpenWorkout(seed = WorkoutSeed.FromWorkout(it))) },
                onUseTemplate = { onAction(FitnessAction.OpenWorkout(seed = WorkoutSeed.FromTemplate(it))) },
            )
        }

        FitnessTab.HISTORY -> {
            HistoryTab(
                overview = overview,
                contentPadding = padding,
                onOpen = { onAction(FitnessAction.OpenWorkout(existing = it)) },
            )
        }

        FitnessTab.EXERCISES -> {
            ExercisesTab(
                overview = overview,
                contentPadding = padding,
                onOpen = { onAction(FitnessAction.Open(FitnessEditor.ExerciseDetail(it.key))) },
            )
        }

        FitnessTab.BODY -> {
            BodyTab(
                overview = overview,
                contentPadding = padding,
                onAddWeight = { onAction(FitnessAction.Open(FitnessEditor.EditWeight())) },
                onEditWeight = { onAction(FitnessAction.Open(FitnessEditor.EditWeight(it))) },
                onAddSteps = { onAction(FitnessAction.Open(FitnessEditor.EditSteps())) },
                onEditSteps = { onAction(FitnessAction.Open(FitnessEditor.EditSteps(it))) },
            )
        }
    }
}

@Composable
private fun EffectsHandler(
    effects: Flow<FitnessEffect>,
    snackbar: SnackbarHostState,
    onAction: (FitnessAction) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val undoLabel = stringResource(DesignR.string.lt_undo)
    val resources = LocalResources.current
    LaunchedEffect(effects) {
        effects.collect { effect ->
            when (effect) {
                is FitnessEffect.Message -> {
                    snackbar.showSnackbar(resources.getString(effect.messageRes))
                }

                is FitnessEffect.Deleted -> {
                    snackbar.showUndo(resources.getString(effect.messageRes), undoLabel) {
                        onAction(FitnessAction.Undo(effect.item))
                    }
                }

                FitnessEffect.Saved -> {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                }
            }
        }
    }
}

@Composable
private fun EditorHost(
    overview: FitnessOverview,
    editor: FitnessEditor?,
    onAction: (FitnessAction) -> Unit,
) {
    val dismiss = { onAction(FitnessAction.Open(null)) }
    when (editor) {
        null -> {
            Unit
        }

        is FitnessEditor.EditWorkout -> {
            WorkoutSheet(
                overview = overview,
                editor = editor,
                onDismiss = dismiss,
                onSave = { onAction(FitnessAction.Save(FitnessForm.Workout(it, editor.existing))) },
                onDelete =
                    editor.existing?.let { workout ->
                        { onAction(FitnessAction.Delete(FitnessItem.Workout(workout))) }
                    },
            )
        }

        is FitnessEditor.EditWeight -> {
            WeightSheet(
                existing = editor.existing,
                today = overview.today,
                suggestedKilos =
                    overview.summary.latestWeightGrams
                        ?.toEditableText(KG_SCALE)
                        .orEmpty(),
                onDismiss = dismiss,
                onSave = { onAction(FitnessAction.Save(FitnessForm.Weight(it, editor.existing))) },
                onDelete =
                    editor.existing?.let { weight ->
                        { onAction(FitnessAction.Delete(FitnessItem.Weight(weight))) }
                    },
            )
        }

        is FitnessEditor.EditSteps -> {
            StepsSheet(
                existing = editor.existing,
                today = overview.today,
                onDismiss = dismiss,
                onSave = { onAction(FitnessAction.Save(FitnessForm.Steps(it, editor.existing))) },
                onDelete =
                    editor.existing?.let { steps ->
                        { onAction(FitnessAction.Delete(FitnessItem.Steps(steps))) }
                    },
            )
        }

        is FitnessEditor.ExerciseDetail -> {
            ExerciseDetailSheet(
                exercise = overview.history.exercises.firstOrNull { it.key == editor.key },
                onDismiss = dismiss,
            )
        }
    }
}

private fun FitnessUiState.overviewOrNull(): FitnessOverview? =
    when (this) {
        is FitnessUiState.Success -> overview
        is FitnessUiState.Empty -> overview
        else -> null
    }

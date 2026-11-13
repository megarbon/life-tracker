package com.example.lifet.feature.quit

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
import androidx.compose.runtime.mutableStateOf
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
import com.example.lifet.core.model.quit.CopingStrategy
import com.example.lifet.core.model.quit.HabitProgress
import com.example.lifet.core.model.quit.QuitConsumption
import com.example.lifet.core.model.quit.QuitCraving
import com.example.lifet.core.model.quit.QuitOverview
import com.example.lifet.core.ui.icon
import com.example.lifet.feature.quit.ui.EventSheet
import com.example.lifet.feature.quit.ui.HabitSheet
import com.example.lifet.feature.quit.ui.HistoryTab
import com.example.lifet.feature.quit.ui.MilestoneCelebration
import com.example.lifet.feature.quit.ui.MilestonesTab
import com.example.lifet.feature.quit.ui.QuitSaved
import com.example.lifet.feature.quit.ui.SosSheet
import com.example.lifet.feature.quit.ui.SummaryCallbacks
import com.example.lifet.feature.quit.ui.SummaryTab
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import com.example.lifet.core.designsystem.R as DesignR

/** Espacio libre al final de las listas para que el FAB no tape el último elemento. */
private val FAB_CLEARANCE = 88.dp

internal enum class QuitTab(
    @param:StringRes val labelRes: Int,
) {
    SUMMARY(R.string.quit_tab_summary),
    MILESTONES(R.string.quit_tab_milestones),
    HISTORY(R.string.quit_tab_history),
}

/** Punto de entrada de la ruta: conecta el ViewModel con la pantalla sin estado. */
@Composable
internal fun QuitRoute(
    modifier: Modifier = Modifier,
    viewModel: QuitViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val editor by viewModel.editor.collectAsStateWithLifecycle()
    QuitScreen(
        state = state,
        editor = editor,
        effects = viewModel.effects,
        clock = viewModel.clock,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

/** Pantalla sin ViewModel: recibe estado y emite [QuitAction]; así se previsualiza y se prueba sola. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QuitScreen(
    state: QuitUiState,
    editor: QuitEditor?,
    effects: Flow<QuitEffect>,
    clock: Clock,
    onAction: (QuitAction) -> Unit,
    modifier: Modifier = Modifier,
    initialTab: QuitTab = QuitTab.SUMMARY,
) {
    var tabIndex by rememberSaveable { mutableIntStateOf(initialTab.ordinal) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    EffectsHandler(effects, snackbar, onAction)
    val overview = (state as? QuitUiState.Success)?.overview
    val current = overview?.let { selected(it, selectedId) }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { LTTopAppBar(title = stringResource(R.string.quit_title), scrollBehavior = scrollBehavior) },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (current != null) {
                ExtendedFloatingActionButton(
                    onClick = { onAction(QuitAction.OpenEvent(current.habit.id)) },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.quit_add)) },
                )
            }
        },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            QuitBody(
                state = state,
                tab = QuitTab.entries[tabIndex],
                selectedId = current?.habit?.id,
                clock = clock,
                onTabChange = { tabIndex = it.ordinal },
                onSelect = { selectedId = it },
                onAction = onAction,
            )
        }
    }
    CelebrationHost(current, clock)
    if (overview != null || editor is QuitEditor.Habit) EditorHost(overview, editor, onAction)
}

/** El hábito elegido, o el principal si no se ha elegido ninguno (o el elegido ya no existe). */
private fun selected(
    overview: QuitOverview,
    selectedId: String?,
): HabitProgress? =
    overview.habits.firstOrNull { it.habit.id == selectedId }
        ?: overview.habits.firstOrNull { it.habit.id == overview.primaryId }
        ?: overview.habits.firstOrNull()

@Composable
private fun QuitBody(
    state: QuitUiState,
    tab: QuitTab,
    selectedId: String?,
    clock: Clock,
    onTabChange: (QuitTab) -> Unit,
    onSelect: (String) -> Unit,
    onAction: (QuitAction) -> Unit,
) {
    when (state) {
        QuitUiState.Loading -> {
            LTLoadingSkeleton()
        }

        is QuitUiState.Error -> {
            LTErrorState(
                message = stringResource(state.messageRes),
                retryLabel = stringResource(R.string.quit_retry),
                onRetry = { onAction(QuitAction.Retry) },
            )
        }

        QuitUiState.Empty -> {
            LTEmptyState(
                icon = Area.QUIT.icon,
                title = stringResource(R.string.quit_empty_title),
                message = stringResource(R.string.quit_empty_message),
                actionLabel = stringResource(R.string.quit_empty_cta),
                onAction = { onAction(QuitAction.OpenHabit()) },
            )
        }

        is QuitUiState.Success -> {
            Tabs(state.overview, selectedId, tab, clock, onTabChange, onSelect, onAction)
        }
    }
}

@Composable
private fun Tabs(
    overview: QuitOverview,
    selectedId: String?,
    tab: QuitTab,
    clock: Clock,
    onTabChange: (QuitTab) -> Unit,
    onSelect: (String) -> Unit,
    onAction: (QuitAction) -> Unit,
) {
    val spacing = LifeTrackerTheme.spacing
    val padding = PaddingValues(start = spacing.md, end = spacing.md, top = spacing.md, bottom = FAB_CLEARANCE)
    val progress = selected(overview, selectedId) ?: return
    PrimaryTabRow(selectedTabIndex = tab.ordinal) {
        QuitTab.entries.forEach { option ->
            Tab(
                selected = option == tab,
                onClick = { onTabChange(option) },
                text = { Text(stringResource(option.labelRes)) },
            )
        }
    }
    when (tab) {
        QuitTab.SUMMARY -> {
            SummaryTab(
                overview = overview,
                progress = progress,
                selectedId = progress.habit.id,
                clock = clock,
                contentPadding = padding,
                callbacks =
                    SummaryCallbacks(
                        onSelectHabit = onSelect,
                        onAddHabit = { onAction(QuitAction.OpenHabit()) },
                        onEditHabit = { onAction(QuitAction.OpenHabit(existing = it.habit)) },
                        onCraving = { onAction(QuitAction.OpenEvent(it)) },
                        onConsumption = { onAction(QuitAction.OpenEvent(it, asConsumption = true)) },
                        onSos = { onAction(QuitAction.OpenSos(it)) },
                    ),
            )
        }

        QuitTab.MILESTONES -> {
            MilestonesTab(progress = progress, clock = clock, contentPadding = padding)
        }

        QuitTab.HISTORY -> {
            HistoryTab(
                overview = overview,
                contentPadding = padding,
                onOpen = { onAction(QuitAction.OpenEvent(it.habitId, existing = it)) },
            )
        }
    }
}

/**
 * Si hay un hito que la racha actual alcanzó hace poco y aún no se ha celebrado, lo celebra una vez con un diálogo
 * sobrio. Se recuerda por hábito, hito y racha para no repetirse al volver a la pantalla ni al rotar.
 */
@Composable
private fun CelebrationHost(
    progress: HabitProgress?,
    clock: Clock,
) {
    val now = clock.instant()
    val pending = progress?.milestones?.lastOrNull { it.isRecent(now) }
    val tag = pending?.let { "${progress.habit.id}|${it.key}|${progress.cleanSince.epochSecond}" }
    var celebrated by rememberSaveable { mutableStateOf<String?>(null) }
    if (pending != null && tag != null && celebrated != tag) {
        MilestoneCelebration(key = pending.key, onDismiss = { celebrated = tag })
    }
}

@Composable
private fun EffectsHandler(
    effects: Flow<QuitEffect>,
    snackbar: SnackbarHostState,
    onAction: (QuitAction) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val undoLabel = stringResource(DesignR.string.lt_undo)
    val resources = LocalResources.current
    LaunchedEffect(effects) {
        effects.collect { effect ->
            when (effect) {
                is QuitEffect.Message -> {
                    snackbar.showSnackbar(resources.getString(effect.messageRes))
                }

                is QuitEffect.Deleted -> {
                    snackbar.showUndo(resources.getString(effect.messageRes), undoLabel) {
                        onAction(QuitAction.Undo(effect.item))
                    }
                }

                is QuitEffect.Saved -> {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    snackbar.showSnackbar(resources.getString(effect.messageRes))
                }
            }
        }
    }
}

@Composable
private fun EditorHost(
    overview: QuitOverview?,
    editor: QuitEditor?,
    onAction: (QuitAction) -> Unit,
) {
    val dismiss = { onAction(QuitAction.Dismiss) }
    when (editor) {
        null -> {
            Unit
        }

        is QuitEditor.Event -> {
            val habit = overview?.habits?.firstOrNull { it.habit.id == editor.habitId }?.habit
            if (overview != null && habit != null) {
                EventSheet(
                    overview = overview,
                    editor = editor,
                    habit = habit,
                    onDismiss = dismiss,
                    onSave = { saved ->
                        val form =
                            when (saved) {
                                is QuitSaved.Craving -> {
                                    QuitForm.Craving(saved.draft, editor.existing as? QuitCraving)
                                }

                                is QuitSaved.Consumption -> {
                                    QuitForm.Consumption(saved.draft, editor.existing as? QuitConsumption)
                                }
                            }
                        onAction(QuitAction.Save(form))
                    },
                    onDelete =
                        editor.existing?.let { event ->
                            { onAction(QuitAction.Delete(event.toItem())) }
                        },
                )
            }
        }

        is QuitEditor.Habit -> {
            HabitSheet(
                editor = editor,
                onDismiss = dismiss,
                onSave = { onAction(QuitAction.Save(QuitForm.Habit(it, editor.existing))) },
                onDelete =
                    editor.existing?.let { habit ->
                        overview.habitItem(habit.id)?.let { item -> { onAction(QuitAction.Delete(item)) } }
                    },
            )
        }

        is QuitEditor.Sos -> {
            SosSheet(
                onDismiss = dismiss,
                onLog = {
                    onAction(QuitAction.OpenEvent(editor.habitId, seedStrategy = CopingStrategy.BREATHING))
                },
            )
        }
    }
}

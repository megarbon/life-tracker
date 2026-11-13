package com.example.lifet.feature.work

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
import androidx.compose.runtime.CompositionLocalProvider
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
import com.example.lifet.core.model.work.FocusKind
import com.example.lifet.core.model.work.WorkOverview
import com.example.lifet.core.model.work.WorkWeekReport
import com.example.lifet.core.ui.icon
import com.example.lifet.feature.work.ui.CloseShiftSheet
import com.example.lifet.feature.work.ui.FocusTab
import com.example.lifet.feature.work.ui.NoteSheet
import com.example.lifet.feature.work.ui.ProjectSheet
import com.example.lifet.feature.work.ui.ProjectsTab
import com.example.lifet.feature.work.ui.SettingsSheet
import com.example.lifet.feature.work.ui.ShiftSheet
import com.example.lifet.feature.work.ui.TodayTab
import com.example.lifet.feature.work.ui.WeekTab
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import com.example.lifet.core.designsystem.R as DesignR

/** Espacio libre al final de las listas para que el FAB no tape el último elemento. */
private val FAB_CLEARANCE = 88.dp

internal enum class WorkTab(
    @param:StringRes val labelRes: Int,
) {
    TODAY(R.string.work_tab_today),
    FOCUS(R.string.work_tab_focus),
    WEEK(R.string.work_tab_week),
    PROJECTS(R.string.work_tab_projects),
}

/** Punto de entrada de la ruta de Trabajo: conecta el ViewModel con la pantalla sin estado. */
@Composable
internal fun WorkRoute(
    modifier: Modifier = Modifier,
    viewModel: WorkViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val editor by viewModel.editor.collectAsStateWithLifecycle()
    WorkScreen(
        state = state,
        editor = editor,
        effects = viewModel.effects,
        clock = viewModel.clock,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

/** Pantalla sin ViewModel: recibe estado y emite [WorkAction]; así se previsualiza y se prueba sola. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WorkScreen(
    state: WorkUiState,
    editor: WorkEditor?,
    effects: Flow<WorkEffect>,
    clock: Clock,
    onAction: (WorkAction) -> Unit,
    modifier: Modifier = Modifier,
    initialTab: WorkTab = WorkTab.TODAY,
) {
    var tabIndex by rememberSaveable { mutableIntStateOf(initialTab.ordinal) }
    val tab = WorkTab.entries[tabIndex]
    val snackbar = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val overview = state.overviewOrNull()
    EffectsHandler(effects, snackbar, onAction)

    CompositionLocalProvider(LocalWorkClock provides clock) {
        Scaffold(
            modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = { LTTopAppBar(title = stringResource(R.string.work_title), scrollBehavior = scrollBehavior) },
            snackbarHost = { SnackbarHost(snackbar) },
            floatingActionButton = { if (overview != null) AddButton(tab, onAction) },
        ) { innerPadding ->
            Column(Modifier.fillMaxSize().padding(innerPadding)) {
                WorkBody(state, tab, clock, { tabIndex = it.ordinal }, onAction)
            }
        }
        EditorHost(overview, editor, onAction)
    }
}

@Composable
private fun AddButton(
    tab: WorkTab,
    onAction: (WorkAction) -> Unit,
) {
    // Foco no tiene alta: se empieza con el botón del propio temporizador.
    if (tab == WorkTab.FOCUS) return
    val label = if (tab == WorkTab.PROJECTS) R.string.work_add_project else R.string.work_add_shift
    ExtendedFloatingActionButton(
        onClick = { onAction(if (tab == WorkTab.PROJECTS) WorkAction.OpenProject() else WorkAction.OpenShift()) },
        icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
        text = { Text(stringResource(label)) },
    )
}

@Composable
private fun WorkBody(
    state: WorkUiState,
    tab: WorkTab,
    clock: Clock,
    onTabChange: (WorkTab) -> Unit,
    onAction: (WorkAction) -> Unit,
) {
    when (state) {
        WorkUiState.Loading -> {
            LTLoadingSkeleton()
        }

        is WorkUiState.Error -> {
            LTErrorState(
                message = stringResource(state.messageRes),
                retryLabel = stringResource(R.string.work_retry),
                onRetry = { onAction(WorkAction.Retry) },
            )
        }

        is WorkUiState.Empty -> {
            // Sin jornadas todavía: Hoy y Semana invitan a fichar, pero Foco y Proyectos siguen siendo útiles.
            val empty = tab == WorkTab.TODAY || tab == WorkTab.WEEK
            Tabs(state.overview, state.week, tab, clock, onTabChange, onAction, showEmpty = empty)
        }

        is WorkUiState.Success -> {
            Tabs(state.overview, state.week, tab, clock, onTabChange, onAction, showEmpty = false)
        }
    }
}

@Composable
private fun Tabs(
    overview: WorkOverview,
    week: WorkWeekReport,
    tab: WorkTab,
    clock: Clock,
    onTabChange: (WorkTab) -> Unit,
    onAction: (WorkAction) -> Unit,
    showEmpty: Boolean,
) {
    val spacing = LifeTrackerTheme.spacing
    val padding = PaddingValues(start = spacing.md, end = spacing.md, top = spacing.md, bottom = FAB_CLEARANCE)
    PrimaryTabRow(selectedTabIndex = tab.ordinal) {
        WorkTab.entries.forEach { option ->
            Tab(
                selected = option == tab,
                onClick = { onTabChange(option) },
                text = { Text(stringResource(option.labelRes)) },
            )
        }
    }
    when {
        showEmpty -> {
            LTEmptyState(
                icon = Area.WORK.icon,
                title = stringResource(R.string.work_empty_title),
                message = stringResource(R.string.work_empty_message),
                actionLabel = stringResource(R.string.work_empty_cta),
                onAction = { onAction(WorkAction.ClockIn(null)) },
            )
        }

        tab == WorkTab.TODAY -> {
            TodayTab(overview, week, clock.zone, padding, onAction)
        }

        tab == WorkTab.FOCUS -> {
            FocusTab(overview, padding, onAction)
        }

        tab == WorkTab.WEEK -> {
            WeekTab(overview, week, padding, onAction)
        }

        else -> {
            ProjectsTab(overview, padding, onAction)
        }
    }
}

@Composable
private fun EffectsHandler(
    effects: Flow<WorkEffect>,
    snackbar: SnackbarHostState,
    onAction: (WorkAction) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val undoLabel = stringResource(DesignR.string.lt_undo)
    val resources = LocalResources.current
    LaunchedEffect(effects) {
        effects.collect { effect ->
            when (effect) {
                is WorkEffect.Message -> {
                    haptic.performHapticFeedback(HapticFeedbackType.Reject)
                    snackbar.showSnackbar(resources.getString(effect.messageRes))
                }

                is WorkEffect.Deleted -> {
                    snackbar.showUndo(resources.getString(effect.messageRes), undoLabel) {
                        onAction(WorkAction.Undo(effect.item))
                    }
                }

                WorkEffect.Saved -> {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                }

                is WorkEffect.FocusFinished -> {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    val message =
                        if (effect.kind == FocusKind.FOCUS) {
                            R.string.work_focus_finished_focus
                        } else {
                            R.string.work_focus_finished_break
                        }
                    snackbar.showSnackbar(resources.getString(message))
                }
            }
        }
    }
}

@Composable
private fun EditorHost(
    overview: WorkOverview?,
    editor: WorkEditor?,
    onAction: (WorkAction) -> Unit,
) {
    if (overview == null || editor == null) return
    val dismiss = { onAction(WorkAction.Dismiss) }
    val projects = overview.projects.map { it.project }
    when (editor) {
        is WorkEditor.EditShift -> {
            ShiftSheet(
                editor = editor,
                projects = projects,
                onDismiss = dismiss,
                onSave = { onAction(WorkAction.Save(WorkForm.Shift(it, editor.existing, editor.zone))) },
                onDelete = editor.existing?.let { shift -> { onAction(WorkAction.Delete(WorkItem.Shift(shift))) } },
            )
        }

        is WorkEditor.CloseShift -> {
            CloseShiftSheet(
                editor = editor,
                onDismiss = dismiss,
                onSave = { date, minute, zone ->
                    onAction(WorkAction.Save(WorkForm.Close(editor.shift, date, minute, zone)))
                },
            )
        }

        is WorkEditor.EditProject -> {
            ProjectSheet(
                editor = editor,
                onDismiss = dismiss,
                onSave = { onAction(WorkAction.Save(WorkForm.Project(it))) },
                onDelete =
                    editor.existing?.let { project ->
                        { onAction(WorkAction.Delete(WorkItem.Project(project))) }
                    },
            )
        }

        is WorkEditor.EditNote -> {
            NoteSheet(
                editor = editor,
                onDismiss = dismiss,
                onSave = { onAction(WorkAction.Save(WorkForm.Note(editor.date, it))) },
            )
        }

        WorkEditor.EditSettings -> {
            SettingsSheet(
                initial = overview.settings.toDraft(),
                onDismiss = dismiss,
                onSave = { onAction(WorkAction.Save(WorkForm.Settings(it))) },
            )
        }
    }
}

private fun WorkUiState.overviewOrNull(): WorkOverview? =
    when (this) {
        is WorkUiState.Success -> overview
        is WorkUiState.Empty -> overview
        else -> null
    }

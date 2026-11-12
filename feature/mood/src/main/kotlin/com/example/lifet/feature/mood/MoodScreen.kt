package com.example.lifet.feature.mood

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
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import com.example.lifet.core.ui.icon
import com.example.lifet.feature.mood.ui.MoodCalendarTab
import com.example.lifet.feature.mood.ui.MoodCheckInSheet
import com.example.lifet.feature.mood.ui.MoodDetailSheet
import com.example.lifet.feature.mood.ui.MoodJournalTab
import com.example.lifet.feature.mood.ui.MoodOverviewTab

/** Espacio libre al final de las listas para que el FAB no tape el último elemento. */
private val FAB_CLEARANCE = 88.dp

internal enum class MoodTab(
    @param:StringRes val labelRes: Int,
) {
    OVERVIEW(R.string.mood_tab_overview),
    JOURNAL(R.string.mood_tab_journal),
    CALENDAR(R.string.mood_tab_calendar),
}

@Composable
internal fun MoodRoute(
    modifier: Modifier = Modifier,
    viewModel: MoodViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val overlay by viewModel.overlay.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is MoodEffect.Message -> {
                    snackbarHost.showSnackbar(resources.getString(effect.messageRes))
                }

                is MoodEffect.Undoable -> {
                    snackbarHost.showUndo(
                        message = resources.getString(effect.messageRes),
                        undoLabel = resources.getString(R.string.mood_undo),
                        onUndo = { viewModel.onAction(MoodAction.Undo) },
                    )
                }
            }
        }
    }

    MoodScreen(
        state = state,
        overlay = overlay,
        snackbarHost = snackbarHost,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

/** Pantalla sin ViewModel: recibe estado y emite [MoodAction]; así se previsualiza y se prueba sola. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MoodScreen(
    state: MoodUiState,
    overlay: MoodOverlay,
    onAction: (MoodAction) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHost: SnackbarHostState = remember { SnackbarHostState() },
    initialTab: MoodTab = MoodTab.OVERVIEW,
) {
    var tabIndex by rememberSaveable { mutableIntStateOf(initialTab.ordinal) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { LTTopAppBar(title = stringResource(R.string.mood_title), scrollBehavior = scrollBehavior) },
        snackbarHost = { SnackbarHost(snackbarHost) },
        floatingActionButton = {
            if (state !is MoodUiState.Loading && state !is MoodUiState.Error) {
                ExtendedFloatingActionButton(
                    onClick = { onAction(MoodAction.OpenNewForm) },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.mood_add)) },
                )
            }
        },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            when (state) {
                MoodUiState.Loading -> {
                    LTLoadingSkeleton()
                }

                is MoodUiState.Error -> {
                    LTErrorState(
                        message = stringResource(state.messageRes),
                        retryLabel = stringResource(R.string.mood_retry),
                        onRetry = { onAction(MoodAction.Retry) },
                    )
                }

                MoodUiState.Empty -> {
                    LTEmptyState(
                        icon = Area.MOOD.icon,
                        title = stringResource(R.string.mood_empty_title),
                        message = stringResource(R.string.mood_empty_message),
                        actionLabel = stringResource(R.string.mood_add),
                        onAction = { onAction(MoodAction.OpenNewForm) },
                    )
                }

                is MoodUiState.Success -> {
                    MoodTabs(
                        state = state,
                        tab = MoodTab.entries[tabIndex],
                        onTabChange = { tabIndex = it.ordinal },
                        onAction = onAction,
                    )
                }
            }
        }
    }
    MoodOverlays(overlay = overlay, onAction = onAction)
}

@Composable
private fun MoodTabs(
    state: MoodUiState.Success,
    tab: MoodTab,
    onTabChange: (MoodTab) -> Unit,
    onAction: (MoodAction) -> Unit,
) {
    val formatters = rememberMoodFormatters()
    val spacing = LifeTrackerTheme.spacing
    val padding = PaddingValues(start = spacing.md, end = spacing.md, top = spacing.md, bottom = FAB_CLEARANCE)
    PrimaryTabRow(selectedTabIndex = tab.ordinal) {
        MoodTab.entries.forEach { option ->
            Tab(
                selected = option == tab,
                onClick = { onTabChange(option) },
                text = { Text(stringResource(option.labelRes)) },
            )
        }
    }
    when (tab) {
        MoodTab.OVERVIEW -> {
            MoodOverviewTab(state = state, formatters = formatters, onAction = onAction, contentPadding = padding)
        }

        MoodTab.JOURNAL -> {
            MoodJournalTab(
                journal = state.journal,
                formatters = formatters,
                onOpen = { onAction(MoodAction.OpenDetail(it)) },
                contentPadding = padding,
            )
        }

        MoodTab.CALENDAR -> {
            MoodCalendarTab(
                calendar = state.calendar,
                formatters = formatters,
                onYearChange = { onAction(MoodAction.ChangeYear(it)) },
                contentPadding = padding,
            )
        }
    }
}

@Composable
private fun MoodOverlays(
    overlay: MoodOverlay,
    onAction: (MoodAction) -> Unit,
) {
    when (overlay) {
        MoodOverlay.None -> {
            Unit
        }

        is MoodOverlay.Form -> {
            MoodCheckInSheet(
                form = overlay.state,
                onChange = { onAction(MoodAction.UpdateForm(it)) },
                onSave = { onAction(MoodAction.SaveForm) },
                onDismiss = { onAction(MoodAction.DismissOverlay) },
            )
        }

        is MoodOverlay.Detail -> {
            MoodDetailSheet(
                entry = overlay.entry,
                formatters = rememberMoodFormatters(),
                onEdit = { onAction(MoodAction.OpenEdit(overlay.entry)) },
                onDelete = { onAction(MoodAction.Delete(overlay.entry)) },
                onDismiss = { onAction(MoodAction.DismissOverlay) },
            )
        }
    }
}

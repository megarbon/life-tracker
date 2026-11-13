package com.example.lifet.dev

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifet.R
import com.example.lifet.core.demo.DemoScenario
import com.example.lifet.core.designsystem.component.LTConfirmDialog
import com.example.lifet.core.designsystem.component.LTSectionHeader
import com.example.lifet.core.designsystem.component.LTTopAppBar
import com.example.lifet.core.designsystem.preview.LTPreviews
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.theme.MinTouchTarget

@Composable
internal fun DevOptionsRoute(
    onOpenGallery: () -> Unit,
    onBack: () -> Unit,
    viewModel: DevOptionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DevOptionsScreen(
        state = state,
        onOpenGallery = onOpenGallery,
        onBack = onBack,
        onLoad = viewModel::load,
        onClear = viewModel::clearAll,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DevOptionsScreen(
    state: DevOptionsUiState,
    onOpenGallery: () -> Unit,
    onBack: () -> Unit,
    onLoad: (DemoScenario) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = { LTTopAppBar(stringResource(R.string.dev_title), onBack = onBack, scrollBehavior = scroll) },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = LifeTrackerTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm),
        ) {
            Button(onClick = onOpenGallery, modifier = Modifier.fillMaxWidth().heightIn(min = MinTouchTarget)) {
                Text(stringResource(R.string.dev_open_gallery))
            }
            DemoDataSection(state = state, onLoad = onLoad, onClear = onClear)
        }
    }
}

/** Qué se va a hacer cuando el usuario confirme el aviso de sustitución de datos. */
private sealed interface PendingDemoAction {
    data class Load(
        val scenario: DemoScenario,
    ) : PendingDemoAction

    data object Clear : PendingDemoAction
}

/**
 * Cargar un escenario o borrar sustituye los datos de todas las áreas, también los reales. Por eso nada se ejecuta
 * al primer toque: hay que confirmar, y los botones se desactivan mientras otra operación está en marcha.
 */
@Composable
private fun DemoDataSection(
    state: DevOptionsUiState,
    onLoad: (DemoScenario) -> Unit,
    onClear: () -> Unit,
) {
    var pending by rememberSaveableAction()
    val idle = state.status != DevStatus.Working
    LTSectionHeader(stringResource(R.string.dev_demo_title))
    Text(stringResource(R.string.dev_demo_registered, state.registeredAreas))
    Text(
        text = stringResource(R.string.dev_demo_warning),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    DemoScenario.entries.forEach { scenario ->
        OutlinedButton(
            onClick = { pending = PendingDemoAction.Load(scenario) },
            enabled = idle,
            modifier = Modifier.fillMaxWidth().heightIn(min = MinTouchTarget),
        ) { Text(stringResource(R.string.dev_load_scenario, scenario.name)) }
    }
    OutlinedButton(
        onClick = { pending = PendingDemoAction.Clear },
        enabled = idle,
        modifier = Modifier.fillMaxWidth().heightIn(min = MinTouchTarget),
    ) { Text(stringResource(R.string.dev_clear_all)) }
    val statusText =
        when (val status = state.status) {
            DevStatus.Idle -> null
            DevStatus.Working -> stringResource(R.string.dev_working)
            DevStatus.Done -> stringResource(R.string.dev_done)
            is DevStatus.Failed -> stringResource(R.string.dev_failed, status.message)
        }
    if (statusText != null) Text(statusText, color = MaterialTheme.colorScheme.primary)

    pending?.let { action ->
        LTConfirmDialog(
            title = stringResource(R.string.dev_confirm_title),
            message = stringResource(R.string.dev_confirm_message),
            confirmLabel = stringResource(R.string.dev_confirm_action),
            onConfirm = {
                pending = null
                when (action) {
                    is PendingDemoAction.Load -> onLoad(action.scenario)
                    PendingDemoAction.Clear -> onClear()
                }
            },
            onDismiss = { pending = null },
        )
    }
}

@Composable
private fun rememberSaveableAction(): MutableState<PendingDemoAction?> = remember { mutableStateOf(null) }

@LTPreviews
@Composable
private fun DevOptionsScreenPreview() {
    LifeTrackerTheme {
        DevOptionsScreen(DevOptionsUiState(registeredAreas = 3), {}, {}, {}, {})
    }
}

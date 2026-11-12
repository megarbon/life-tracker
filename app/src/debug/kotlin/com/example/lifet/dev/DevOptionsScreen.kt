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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifet.R
import com.example.lifet.core.demo.DemoScenario
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
            LTSectionHeader(stringResource(R.string.dev_demo_title))
            Text(stringResource(R.string.dev_demo_registered, state.registeredAreas))
            DemoScenario.entries.forEach { scenario ->
                OutlinedButton(
                    onClick = { onLoad(scenario) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = MinTouchTarget),
                ) { Text(stringResource(R.string.dev_load_scenario, scenario.name)) }
            }
            OutlinedButton(onClick = onClear, modifier = Modifier.fillMaxWidth().heightIn(min = MinTouchTarget)) {
                Text(stringResource(R.string.dev_clear_all))
            }
            val statusText =
                when (val status = state.status) {
                    DevStatus.Idle -> null
                    DevStatus.Working -> stringResource(R.string.dev_working)
                    DevStatus.Done -> stringResource(R.string.dev_done)
                    is DevStatus.Failed -> stringResource(R.string.dev_failed, status.message)
                }
            if (statusText != null) Text(statusText, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@LTPreviews
@Composable
private fun DevOptionsScreenPreview() {
    LifeTrackerTheme {
        DevOptionsScreen(DevOptionsUiState(registeredAreas = 3), {}, {}, {}, {})
    }
}

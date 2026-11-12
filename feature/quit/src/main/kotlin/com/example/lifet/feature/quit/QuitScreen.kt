package com.example.lifet.feature.quit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.lifet.core.designsystem.component.LTEmptyState
import com.example.lifet.core.designsystem.preview.LTPreviews
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme

/** Placeholder: la pantalla real se construye con el flujo feature-workflow. */
@Composable
internal fun QuitRoute(modifier: Modifier = Modifier) {
    QuitScreen(modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QuitScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.quit_title)) }) },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            LTEmptyState(
                icon = Icons.Rounded.Home,
                title = stringResource(R.string.quit_empty_title),
                message = stringResource(R.string.quit_empty_message),
            )
        }
    }
}

@LTPreviews
@Composable
private fun QuitScreenPreview() {
    LifeTrackerTheme { QuitScreen() }
}

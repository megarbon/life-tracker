package com.example.lifet.feature.mood

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
internal fun MoodRoute(modifier: Modifier = Modifier) {
    MoodScreen(modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MoodScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.mood_title)) }) },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            LTEmptyState(
                icon = Icons.Rounded.Home,
                title = stringResource(R.string.mood_empty_title),
                message = stringResource(R.string.mood_empty_message),
            )
        }
    }
}

@LTPreviews
@Composable
private fun MoodScreenPreview() {
    LifeTrackerTheme { MoodScreen() }
}

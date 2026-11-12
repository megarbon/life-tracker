package com.example.lifet.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.example.lifet.core.designsystem.preview.LTPreviews
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme

/** Estado de error con mensaje humano y acción para reintentar. */
@Composable
public fun LTErrorState(
    message: String,
    retryLabel: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(LifeTrackerTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        OutlinedButton(onClick = onRetry) { Text(retryLabel) }
    }
}

@LTPreviews
@Composable
private fun LTErrorStatePreview() {
    LifeTrackerTheme {
        LTErrorState(
            message = "No se han podido cargar tus datos.",
            retryLabel = "Reintentar",
            onRetry = {},
        )
    }
}

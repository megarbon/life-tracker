package com.example.lifet.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.lifet.core.designsystem.preview.LTPreviews
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme

/** Dato pequeño (etiqueta + valor) para filas de estadísticas dentro de una pantalla. */
@Composable
public fun LTStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
) {
    val summary = listOfNotNull(label, value, supporting).joinToString(". ")
    Surface(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = summary },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(LifeTrackerTheme.spacing.md)) {
            val muted = MaterialTheme.colorScheme.onSurfaceVariant
            Text(label, style = MaterialTheme.typography.labelMedium, color = muted)
            Text(value, style = MaterialTheme.typography.titleLarge)
            if (supporting != null) {
                Text(supporting, style = MaterialTheme.typography.bodySmall, color = muted)
            }
        }
    }
}

@LTPreviews
@Composable
private fun LTStatTilePreview() {
    LifeTrackerTheme { LTStatTile(label = "Racha", value = "47 días", supporting = "Récord: 52") }
}

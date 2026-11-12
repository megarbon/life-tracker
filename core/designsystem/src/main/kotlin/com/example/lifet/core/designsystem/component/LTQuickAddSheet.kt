package com.example.lifet.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.example.lifet.core.designsystem.R
import com.example.lifet.core.designsystem.preview.LTPreviews
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.theme.MinTouchTarget

/**
 * Hoja inferior para registrar algo en pocos toques.
 *
 * Arriba muestra los [recents] como chips (un toque repite un registro anterior) y debajo el
 * [content] del formulario. La feature decide qué hace cada chip en [onRecentPick] y gestiona
 * el botón de guardar dentro de [content].
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
public fun LTQuickAddSheet(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    recents: List<String> = emptyList(),
    onRecentPick: (String) -> Unit = {},
    content: @Composable () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = LifeTrackerTheme.spacing.md)
                    .padding(bottom = LifeTrackerTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.md),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            if (recents.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.lt_recent),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.xs),
                ) {
                    recents.forEach { recent ->
                        AssistChip(
                            onClick = { onRecentPick(recent) },
                            label = { Text(recent) },
                            modifier = Modifier.heightIn(min = MinTouchTarget),
                        )
                    }
                }
            }
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@LTPreviews
@Composable
private fun LTQuickAddSheetPreview() {
    LifeTrackerTheme {
        LTQuickAddSheet(
            title = "Añadir comida",
            onDismiss = {},
            recents = listOf("Avena con fruta", "Café con leche", "Tortilla"),
        ) {
            Text("Formulario")
        }
    }
}

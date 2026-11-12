package com.example.lifet.feature.mood.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.lifet.core.designsystem.component.LTConfirmDialog
import com.example.lifet.core.designsystem.preview.LTPreviews
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.theme.MinTouchTarget
import com.example.lifet.core.model.mood.MoodEntry
import com.example.lifet.core.model.mood.MoodLevel
import com.example.lifet.feature.mood.MoodFormatters
import com.example.lifet.feature.mood.R
import com.example.lifet.feature.mood.emotionLabel
import com.example.lifet.feature.mood.factorLabel
import com.example.lifet.feature.mood.icon
import com.example.lifet.feature.mood.labelRes
import com.example.lifet.feature.mood.rememberMoodFormatters
import java.time.Instant
import java.time.ZoneId

/** Detalle de un check-in con Editar y Borrar. Borrar pide confirmación y luego ofrece "Deshacer". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MoodDetailSheet(
    entry: MoodEntry,
    formatters: MoodFormatters,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        MoodDetailContent(entry, formatters, onEdit, onDelete)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MoodDetailContent(
    entry: MoodEntry,
    formatters: MoodFormatters,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    val spacing = LifeTrackerTheme.spacing
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = spacing.md).padding(bottom = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        DetailHeader(entry, formatters)
        val scales =
            listOfNotNull(
                entry.energy?.let { stringResource(R.string.mood_detail_energy, it) },
                entry.stress?.let { stringResource(R.string.mood_detail_stress, it) },
            )
        if (scales.isNotEmpty()) Text(scales.joinToString(" · "), style = MaterialTheme.typography.bodyLarge)
        TagRow(entry.emotions) { emotionLabel(it) }
        TagRow(entry.factors) { factorLabel(it) }
        TextBlock(stringResource(R.string.mood_detail_note), entry.note)
        TextBlock(stringResource(R.string.mood_detail_gratitude), entry.gratitude)
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            OutlinedButton(onClick = onEdit, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                Icon(Icons.Rounded.Edit, contentDescription = null)
                Text(stringResource(R.string.mood_detail_edit))
            }
            TextButton(onClick = { confirmDelete = true }, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                Icon(Icons.Rounded.Delete, contentDescription = null)
                Text(stringResource(R.string.mood_detail_delete))
            }
        }
    }
    if (confirmDelete) {
        LTConfirmDialog(
            title = stringResource(R.string.mood_delete_title),
            message = stringResource(R.string.mood_delete_message),
            confirmLabel = stringResource(R.string.mood_detail_delete),
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun DetailHeader(
    entry: MoodEntry,
    formatters: MoodFormatters,
) {
    val zoned = entry.instant.atZone(entry.zone)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.md),
    ) {
        Icon(
            imageVector = entry.level.icon,
            contentDescription = null,
            tint = LifeTrackerTheme.areaColors.mood,
            modifier = Modifier.size(48.dp),
        )
        Column {
            Text(
                text = stringResource(entry.level.labelRes),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = "${zoned.format(formatters.fullDate)} · ${zoned.format(formatters.time)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagRow(
    tags: List<String>,
    label: @Composable (String) -> String,
) {
    if (tags.isEmpty()) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.xs),
    ) {
        tags.forEach { tag ->
            val text = label(tag)
            AssistChip(onClick = {}, label = { Text(text) }, enabled = false)
        }
    }
}

@Composable
private fun TextBlock(
    title: String,
    text: String,
) {
    if (text.isBlank()) return
    Column {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@LTPreviews
@Composable
private fun MoodDetailContentPreview() {
    LifeTrackerTheme {
        MoodDetailContent(
            entry =
                MoodEntry(
                    id = 1,
                    instant = Instant.parse("2026-03-10T09:30:00Z"),
                    zone = ZoneId.of("Europe/Madrid"),
                    level = MoodLevel.GOOD,
                    emotions = listOf("joy", "calm"),
                    energy = 4,
                    stress = 2,
                    factors = listOf("sport", "sleep"),
                    note = "Paseo largo al atardecer",
                    gratitude = "El sol de la tarde",
                ),
            formatters = rememberMoodFormatters(),
            onEdit = {},
            onDelete = {},
        )
    }
}

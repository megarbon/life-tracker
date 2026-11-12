package com.example.lifet.feature.mood.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lifet.core.designsystem.preview.LTPreviews
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.theme.MinTouchTarget
import com.example.lifet.core.model.mood.MoodEntry
import com.example.lifet.feature.mood.MoodDayGroup
import com.example.lifet.feature.mood.MoodFormatters
import com.example.lifet.feature.mood.R
import com.example.lifet.feature.mood.emotionLabel
import com.example.lifet.feature.mood.icon
import com.example.lifet.feature.mood.labelRes
import com.example.lifet.feature.mood.rememberMoodFormatters
import kotlinx.collections.immutable.ImmutableList

private const val NOTE_MAX_LINES = 2

/** Timeline agrupado por día, con la media del día y cada check-in como fila tocable. */
@Composable
internal fun MoodJournalTab(
    journal: ImmutableList<MoodDayGroup>,
    formatters: MoodFormatters,
    onOpen: (MoodEntry) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier, contentPadding = contentPadding) {
        journal.forEach { group ->
            item(key = "day-${group.date}", contentType = "day") { DayHeader(group, formatters) }
            items(group.entries, key = { it.id }, contentType = { "entry" }) { entry ->
                EntryRow(entry, formatters, onClick = { onOpen(entry) })
            }
        }
    }
}

@Composable
private fun DayHeader(
    group: MoodDayGroup,
    formatters: MoodFormatters,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = LifeTrackerTheme.spacing.md, bottom = LifeTrackerTheme.spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = group.date.format(formatters.fullDate),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f, fill = false).semantics { heading() },
        )
        Text(
            text = stringResource(R.string.mood_journal_day_average, formatters.average(group.average)),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = LifeTrackerTheme.spacing.sm),
        )
    }
}

@Composable
private fun EntryRow(
    entry: MoodEntry,
    formatters: MoodFormatters,
    onClick: () -> Unit,
) {
    val time = entry.instant.atZone(entry.zone).format(formatters.time)
    val level = stringResource(entry.level.labelRes)
    val summary = entry.summaryText()
    val description = stringResource(R.string.mood_entry_description, time, level)
    val spoken = if (summary.isBlank()) description else "$description. $summary"
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = MinTouchTarget)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics(mergeDescendants = true) { contentDescription = spoken }
                .padding(vertical = LifeTrackerTheme.spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = entry.level.icon,
            contentDescription = null,
            tint = LifeTrackerTheme.areaColors.mood,
            modifier = Modifier.size(32.dp),
        )
        Column(Modifier.weight(1f).clearAndSetSemantics {}) {
            Text(text = "$time · $level", style = MaterialTheme.typography.titleSmall)
            if (summary.isNotBlank()) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = NOTE_MAX_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Emociones traducidas y nota, en una línea de contexto para la lista. */
@Composable
private fun MoodEntry.summaryText(): String {
    val labels = emotions.map { emotionLabel(it) }
    return listOf(labels.joinToString(", "), note.trim()).filter { it.isNotBlank() }.joinToString(" · ")
}

@LTPreviews
@Composable
private fun EntryRowPreview() {
    LifeTrackerTheme {
        EntryRow(
            entry =
                MoodEntry(
                    id = 1,
                    instant = java.time.Instant.parse("2026-03-10T09:30:00Z"),
                    zone = java.time.ZoneId.of("Europe/Madrid"),
                    level = com.example.lifet.core.model.mood.MoodLevel.GOOD,
                    emotions = listOf("joy", "calm"),
                    note = "Paseo largo al atardecer",
                ),
            formatters = rememberMoodFormatters(),
            onClick = {},
        )
    }
}

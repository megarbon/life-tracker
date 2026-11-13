package com.example.lifet.feature.quit.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.model.quit.HabitMode
import com.example.lifet.core.model.quit.HabitProgress
import com.example.lifet.core.model.quit.Milestone
import com.example.lifet.core.model.quit.MilestoneKey
import com.example.lifet.feature.quit.QuitFormatters
import com.example.lifet.feature.quit.R
import com.example.lifet.feature.quit.labelRes
import com.example.lifet.feature.quit.liveProgress
import com.example.lifet.feature.quit.millisUntilNextTick
import com.example.lifet.feature.quit.noteRes
import com.example.lifet.feature.quit.rememberLiveNow
import com.example.lifet.feature.quit.rememberQuitFormatters
import com.example.lifet.feature.quit.spanText
import java.time.Clock
import java.time.Instant

private val ICON_BADGE = 40.dp
private val BADGE_ICON = 20.dp

/** Tarjeta del siguiente hito con su avance en vivo. Si ya los alcanzó todos, lo celebra con calma. */
@Composable
internal fun NextMilestoneCard(
    progress: HabitProgress,
    clock: Clock,
    modifier: Modifier = Modifier,
) {
    val now = rememberLiveNow(clock) { millisUntilNextTick(progress.cleanSince, it) }
    val next = liveProgress(progress, now.value).next
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(LifeTrackerTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm),
        ) {
            if (next == null) {
                Text(stringResource(R.string.quit_all_milestones), style = MaterialTheme.typography.titleMedium)
            } else {
                Text(
                    text = stringResource(R.string.quit_next_milestone, stringResource(next.key.labelRes)),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.quit_next_milestone_remaining, spanText(next.remainingSeconds)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LinearProgressIndicator(
                    progress = { next.progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = LifeTrackerTheme.areaColors.quit,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
            }
        }
    }
}

/**
 * Pestaña de hitos: todos los hitos de tiempo con lo que ya se ha conseguido y lo que falta. Un hito que una racha
 * anterior ya alcanzó se queda conseguido: una recaída no quita nada.
 */
@Composable
internal fun MilestonesTab(
    progress: HabitProgress,
    clock: Clock,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val formatters = rememberQuitFormatters()
    val now = rememberLiveNow(clock) { millisUntilNextTick(progress.cleanSince, it) }
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm),
    ) {
        item(key = "intro") {
            Text(
                text =
                    stringResource(
                        if (progress.habit.mode == HabitMode.REDUCE) {
                            R.string.quit_milestones_reduce
                        } else {
                            R.string.quit_milestones_intro
                        },
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(progress.milestones, key = { it.key.name }, contentType = { "milestone" }) { milestone ->
            MilestoneRow(milestone, formatters, now.value)
        }
    }
}

@Composable
private fun MilestoneRow(
    milestone: Milestone,
    formatters: QuitFormatters,
    now: Instant,
) {
    val accent = LifeTrackerTheme.areaColors.quit
    val name = stringResource(milestone.key.labelRes)
    val reached = milestone.reachedNow || milestone.reachedEver
    val status = milestoneStatus(milestone, formatters, now)
    val note = stringResource(milestone.key.noteRes)
    val description = listOf(name, status, note.takeIf { reached }).filterNotNull().joinToString(". ")
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = LifeTrackerTheme.spacing.xl + LifeTrackerTheme.spacing.lg)
                .clearAndSetSemantics { contentDescription = description }
                .padding(vertical = LifeTrackerTheme.spacing.sm, horizontal = LifeTrackerTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.md),
    ) {
        Surface(
            shape = CircleShape,
            color =
                if (reached) {
                    accent.copy(
                        alpha = BADGE_FILL_ALPHA,
                    )
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
            modifier = Modifier.size(ICON_BADGE),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(
                    imageVector = if (reached) Icons.Rounded.CheckCircle else Icons.Rounded.Flag,
                    contentDescription = null,
                    tint = if (reached) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(BADGE_ICON).padding(top = LifeTrackerTheme.spacing.sm),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = if (reached) note else status,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!reached && milestone.progress > 0f) {
                LinearProgressIndicator(
                    progress = { milestone.progress },
                    modifier = Modifier.fillMaxWidth().padding(top = LifeTrackerTheme.spacing.xs),
                    color = accent,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
            }
        }
        if (reached) {
            // La palabra acompaña al icono: el logro nunca depende sólo del color.
            Text(
                text = if (milestone.isRecent(now)) stringResource(R.string.quit_milestone_new) else status,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Estado en palabras: conseguido ahora, conseguido en una racha anterior o lo que falta. */
@Composable
private fun milestoneStatus(
    milestone: Milestone,
    formatters: QuitFormatters,
    now: Instant,
): String =
    when {
        milestone.reachedNow && milestone.isRecent(now) -> stringResource(R.string.quit_milestone_new)
        milestone.reachedNow -> stringResource(R.string.quit_milestone_reached_now)
        milestone.reachedEver -> stringResource(R.string.quit_milestone_reached_before)
        else -> stringResource(R.string.quit_milestone_in, spanText(milestone.remainingSeconds))
    }

/**
 * Celebración sobria de un hito recién alcanzado: un diálogo tranquilo con un texto amable, sin confeti ni sonidos.
 * No hay animación que respetar: es estático.
 */
@Composable
internal fun MilestoneCelebration(
    key: MilestoneKey,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = LifeTrackerTheme.areaColors.quit) },
        title = { Text(stringResource(R.string.quit_celebration_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm)) {
                Text(stringResource(R.string.quit_celebration_message, stringResource(key.labelRes)))
                Text(
                    text = stringResource(key.noteRes),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.quit_celebration_dismiss)) } },
        modifier = Modifier.semantics { },
    )
}

private const val BADGE_FILL_ALPHA = 0.16f

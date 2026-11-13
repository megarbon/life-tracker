package com.example.lifet.feature.work.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifet.core.designsystem.component.LTChipGroup
import com.example.lifet.core.designsystem.component.LTProgressRing
import com.example.lifet.core.designsystem.component.LTSectionHeader
import com.example.lifet.core.designsystem.component.RingSpec
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.theme.MinTouchTarget
import com.example.lifet.core.model.work.FocusKind
import com.example.lifet.core.model.work.FocusSession
import com.example.lifet.core.model.work.WorkOverview
import com.example.lifet.core.model.work.WorkRules
import com.example.lifet.feature.work.DurationFormat
import com.example.lifet.feature.work.R
import com.example.lifet.feature.work.WorkAction
import com.example.lifet.feature.work.WorkFormatters
import com.example.lifet.feature.work.labelRes
import com.example.lifet.feature.work.rememberDurationFormat
import com.example.lifet.feature.work.rememberLiveNow
import com.example.lifet.feature.work.rememberWorkFormatters
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val RING_SIZE = 240.dp
private val RING_STROKE = 16.dp

/**
 * Foco: temporizador tipo pomodoro con anillo. El intervalo en marcha es una sesión guardada con su hora de inicio;
 * la cuenta atrás es `fin previsto - ahora`, así que no se pierde al rotar ni al irse a segundo plano. Aún no hay
 * servicio foreground ni notificación: se dice en pantalla, sin alarmar.
 */
@Composable
internal fun FocusTab(
    overview: WorkOverview,
    contentPadding: PaddingValues,
    onAction: (WorkAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatters = rememberWorkFormatters()
    val durations = rememberDurationFormat()
    val focus = overview.focus
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.md),
    ) {
        item(key = "timer", contentType = "timer") {
            val running = focus.running
            if (running != null) {
                RunningTimer(running, formatters, onAction)
            } else {
                IdleTimer(overview, onAction)
            }
        }
        item(key = "today", contentType = "today") { TodayFocusCard(overview, durations) }
        item(key = "recent-header", contentType = "header") {
            LTSectionHeader(title = stringResource(R.string.work_focus_recent))
        }
        if (focus.recent.isEmpty()) {
            item(key = "recent-empty", contentType = "text") {
                Text(
                    text = stringResource(R.string.work_focus_recent_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(focus.recent, key = { "recent-${it.id}" }, contentType = { "recent" }) { session ->
            RecentSessionRow(session, overview, formatters, durations)
        }
    }
}

/**
 * Temporizador en marcha. La tarjeta (nota y botón de parar) es estática; sólo [LiveRing] lee el reloj, así que cada
 * segundo se recompone el anillo y su cifra, no la tarjeta ni la pantalla.
 */
@Composable
private fun RunningTimer(
    session: FocusSession,
    formatters: WorkFormatters,
    onAction: (WorkAction) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(LifeTrackerTheme.spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.md),
        ) {
            LiveRing(session, formatters, onFinished = { onAction(WorkAction.FinishFocus) })
            Text(
                text = stringResource(R.string.work_focus_background_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            OutlinedButton(
                onClick = { onAction(WorkAction.StopFocus) },
                modifier = Modifier.fillMaxWidth().heightIn(min = MinTouchTarget),
            ) {
                Icon(Icons.Rounded.Stop, contentDescription = null)
                Text(
                    text = stringResource(R.string.work_focus_stop),
                    modifier = Modifier.padding(start = LifeTrackerTheme.spacing.sm),
                )
            }
        }
    }
}

/**
 * Anillo con la cuenta atrás. Es la única hoja que lee el "ahora" vivo. El progreso se redondea a pasos del 1 %: el
 * anillo anima su arco al cambiar de valor, y sin redondear lo relanzaría en cada segundo y quedaría moviéndose
 * siempre. Al llegar a cero avisa con [onFinished]; si el dominio no pudo cerrarlo, vuelve a intentarlo cada poco en
 * lugar de dejar el contador congelado en 00:00.
 */
@Composable
private fun LiveRing(
    session: FocusSession,
    formatters: WorkFormatters,
    onFinished: () -> Unit,
) {
    val now by rememberLiveNow(enabled = true)
    val due = session.isDueAt(now)
    val currentOnFinished by rememberUpdatedState(onFinished)
    LaunchedEffect(session.id, due) {
        while (due) {
            currentOnFinished()
            delay(FINISH_RETRY_MS)
        }
    }
    val label = stringResource(session.kind.labelRes)
    val countdown = formatters.countdown(session.remainingSecondsAt(now))
    val description = stringResource(R.string.work_focus_ring_description, label, countdown)
    val progress = (session.progressAt(now) * PROGRESS_STEPS).roundToInt() / PROGRESS_STEPS.toFloat()
    LTProgressRing(
        rings = listOf(RingSpec(progress, LifeTrackerTheme.areaColors.work)),
        size = RING_SIZE,
        strokeWidth = RING_STROKE,
        contentDescription = description,
    ) {
        RingCenter(label, countdown, session.title)
    }
}

private const val PROGRESS_STEPS = 100
private const val FINISH_RETRY_MS = 2_000L

/** Centro del anillo. La cifra se encoge hasta caber (fuente 2x, "01:00:00") en lugar de cortarse. */
@Composable
private fun RingCenter(
    label: String,
    countdown: String,
    title: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.width(RING_CONTENT_WIDTH),
    ) {
        Text(
            text = countdown,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.displayMedium.copy(textAlign = TextAlign.Center),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = COUNTDOWN_MIN_SP.sp, maxFontSize = COUNTDOWN_MAX_SP.sp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (title.isNotBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private val RING_CONTENT_WIDTH = 168.dp
private const val COUNTDOWN_MIN_SP = 20
private const val COUNTDOWN_MAX_SP = 45

/** Sin temporizador: elegir tipo, minutos, proyecto y título, y empezar. Recuerda la elección al girar. */
@Composable
private fun IdleTimer(
    overview: WorkOverview,
    onAction: (WorkAction) -> Unit,
) {
    var kind by rememberSaveable(overview.focus.suggestedNext) { mutableStateOf(overview.focus.suggestedNext) }
    var minutesText by rememberSaveable(kind) { mutableStateOf(kind.defaultMinutes.toString()) }
    var title by rememberSaveable { mutableStateOf("") }
    var projectId by rememberSaveable { mutableStateOf<String?>(null) }
    val minutes = minutesText.toIntOrNull()?.takeIf { it in 1..WorkRules.MAX_FOCUS_MINUTES }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(LifeTrackerTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.md),
        ) {
            KindPicker(overview.focus.suggestedNext, kind) { kind = it }
            MinutesField(minutesText, minutes == null) { minutesText = it.filter(Char::isDigit).take(MINUTES_DIGITS) }
            if (kind.isFocus) {
                FocusDetails(overview, title, { title = it }, projectId, { projectId = it })
            }
            StartButton(enabled = minutes != null) {
                minutes?.let { onAction(WorkAction.StartFocus(kind, it, projectId, title)) }
            }
        }
    }
}

@Composable
private fun KindPicker(
    suggested: FocusKind,
    selected: FocusKind,
    onSelect: (FocusKind) -> Unit,
) {
    val labels = FocusKind.entries.associateWith { stringResource(it.labelRes) }
    Text(
        text = stringResource(R.string.work_focus_title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        text = stringResource(R.string.work_focus_suggestion, labels.getValue(suggested)),
        style = MaterialTheme.typography.titleMedium,
    )
    LTChipGroup(
        options = FocusKind.entries,
        selected = selected,
        label = { labels.getValue(it) },
        onSelect = onSelect,
    )
}

@Composable
private fun FocusDetails(
    overview: WorkOverview,
    title: String,
    onTitleChange: (String) -> Unit,
    projectId: String?,
    onProjectChange: (String?) -> Unit,
) {
    TextInput(
        value = title,
        onValueChange = onTitleChange,
        label = stringResource(R.string.work_focus_title_field),
        maxLength = WorkRules.FOCUS_TITLE_MAX_LENGTH,
        modifier = Modifier.fillMaxWidth(),
    )
    if (overview.projects.isNotEmpty()) {
        ProjectPicker(
            projects = overview.projects.map { it.project },
            selectedId = projectId,
            onSelect = onProjectChange,
        )
    }
}

@Composable
private fun StartButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = MinTouchTarget),
    ) {
        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
        Text(
            text = stringResource(R.string.work_focus_start),
            modifier = Modifier.padding(start = LifeTrackerTheme.spacing.sm),
        )
    }
}

private const val MINUTES_DIGITS = 3

@Composable
private fun MinutesField(
    value: String,
    isError: Boolean,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.work_focus_minutes_field)) },
        singleLine = true,
        isError = isError,
        supportingText = if (isError) ({ Text(stringResource(R.string.work_issue_invalid_duration)) }) else null,
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Number,
            ),
    )
}

@Composable
private fun TodayFocusCard(
    overview: WorkOverview,
    durations: DurationFormat,
) {
    val focus = overview.focus
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(LifeTrackerTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.xs),
        ) {
            Text(
                text = stringResource(R.string.work_focus_done_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = durations.format(focus.todayFocusSeconds),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text =
                    pluralStringResource(
                        R.plurals.work_focus_done_sessions,
                        focus.completedFocusToday,
                        focus.completedFocusToday,
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecentSessionRow(
    session: FocusSession,
    overview: WorkOverview,
    formatters: WorkFormatters,
    durations: DurationFormat,
) {
    val now = overview.now
    val project = session.projectId?.let { id -> overview.projects.firstOrNull { it.project.id == id }?.project }
    val title = session.title.ifBlank { stringResource(session.kind.labelRes) }
    val detail =
        listOfNotNull(
            formatters.date(session.startDate),
            durations.format(session.elapsedSecondsAt(now)),
            project?.name,
        ).joinToString(" · ")
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(
                    min = MinTouchTarget,
                ).padding(horizontal = LifeTrackerTheme.spacing.xs),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

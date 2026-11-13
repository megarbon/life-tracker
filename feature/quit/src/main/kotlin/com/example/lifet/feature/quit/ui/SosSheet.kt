package com.example.lifet.feature.quit.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.lifet.core.designsystem.component.LTQuickAddSheet
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.theme.MinTouchTarget
import com.example.lifet.core.designsystem.util.rememberReducedMotion
import com.example.lifet.feature.quit.BreathPhase
import com.example.lifet.feature.quit.BreathState
import com.example.lifet.feature.quit.BreathingCycle
import com.example.lifet.feature.quit.R
import kotlinx.coroutines.delay

private const val SECOND_MS = 1_000L
private const val MIN_SCALE = 0.55f
private const val MAX_SCALE = 1f
private const val DISC_ALPHA = 0.28f
private const val RING_ALPHA = 0.6f
private val CIRCLE_MAX_WIDTH = 240.dp
private val RING_STROKE = 3.dp

/**
 * Hoja de ayuda inmediata: una respiración guiada 4-4-6 con un círculo que crece al inspirar, se queda al mantener y
 * se encoge al soltar. Con "eliminar animaciones" el círculo no se mueve y el ejercicio se sigue con las palabras y la
 * cuenta atrás. Pensada para usarse en medio de un antojo: pocos controles, tono tranquilo y nada que pueda sonar a
 * reproche. Al terminar ofrece anotar el antojo con "respirar" ya marcado.
 */
@Composable
internal fun SosSheet(
    onDismiss: () -> Unit,
    onLog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var running by rememberSaveable { mutableStateOf(false) }
    var elapsed by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(running) {
        while (running) {
            delay(SECOND_MS)
            elapsed += 1
        }
    }
    val state = BreathingCycle.stateAt(elapsed)
    LTQuickAddSheet(title = stringResource(R.string.quit_sos_title), onDismiss = onDismiss, modifier = modifier) {
        SheetColumn {
            Text(
                text = stringResource(R.string.quit_sos_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            BreathCircle(state = state, running = running)
            if (running) {
                Text(
                    text =
                        pluralStringResource(
                            R.plurals.quit_sos_cycles,
                            state.completedCycles,
                            state.completedCycles,
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
            Controls(running = running, onToggle = {
                if (running) {
                    running = false
                } else {
                    elapsed = 0
                    running = true
                }
            }, onLog = onLog, showLog = elapsed > 0)
            Tips()
        }
    }
}

@Composable
private fun Controls(
    running: Boolean,
    onToggle: () -> Unit,
    onLog: () -> Unit,
    showLog: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm)) {
        Button(onClick = onToggle, modifier = Modifier.fillMaxWidth().heightIn(min = MinTouchTarget)) {
            Text(stringResource(if (running) R.string.quit_sos_stop else R.string.quit_sos_start))
        }
        if (showLog && !running) {
            OutlinedButton(onClick = onLog, modifier = Modifier.fillMaxWidth().heightIn(min = MinTouchTarget)) {
                Text(stringResource(R.string.quit_sos_log))
            }
        }
    }
}

/**
 * El círculo respira: su tamaño sigue la fase con un muelle suave. La palabra y los segundos van dentro, en una región
 * "viva" para que TalkBack anuncie cada cambio de fase sin repetir cada segundo.
 */
@Composable
private fun BreathCircle(
    state: BreathState,
    running: Boolean,
) {
    val reduced = rememberReducedMotion()
    val target =
        when {
            !running -> MIN_SCALE
            state.phase == BreathPhase.INHALE || state.phase == BreathPhase.HOLD -> MAX_SCALE
            else -> MIN_SCALE
        }
    val scale by animateFloatAsState(
        targetValue = target,
        animationSpec =
            if (reduced) {
                tween(
                    0,
                )
            } else {
                tween(state.phase.seconds * SECOND_MS.toInt(), easing = FastOutSlowInEasing)
            },
        label = "breath-scale",
    )
    val phaseText = phaseText(state.phase, running)
    val description = stringResource(R.string.quit_breath_circle_description, phaseText, state.secondsLeft)
    val color = LifeTrackerTheme.areaColors.quit
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            modifier =
                Modifier
                    .widthIn(max = CIRCLE_MAX_WIDTH)
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clearAndSetSemantics {
                        contentDescription = if (running) description else phaseText
                        liveRegion = LiveRegionMode.Polite
                    },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.matchParentSize()) {
                val radius = size.minDimension / 2
                drawCircle(
                    color = color.copy(alpha = DISC_ALPHA),
                    radius = radius * scale,
                    center =
                        Offset(
                            size.width / 2,
                            size.height / 2,
                        ),
                )
                drawCircle(
                    color = color.copy(alpha = RING_ALPHA),
                    radius = radius,
                    center = Offset(size.width / 2, size.height / 2),
                    style = Stroke(width = RING_STROKE.toPx()),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = phaseText,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    color = if (running) MaterialTheme.colorScheme.onSurface else Color.Unspecified,
                )
                if (running) {
                    Text(
                        text = state.secondsLeft.toString(),
                        style = MaterialTheme.typography.displaySmall,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
    if (reduced && running) {
        Text(
            text = stringResource(R.string.quit_sos_reduced_motion),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun phaseText(
    phase: BreathPhase,
    running: Boolean,
): String =
    if (!running) {
        stringResource(R.string.quit_sos_keep_going)
    } else {
        stringResource(
            when (phase) {
                BreathPhase.INHALE -> R.string.quit_sos_inhale
                BreathPhase.HOLD -> R.string.quit_sos_hold
                BreathPhase.EXHALE -> R.string.quit_sos_exhale
            },
        )
    }

@Composable
private fun Tips() {
    Column(verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.xs)) {
        Text(
            text = stringResource(R.string.quit_sos_tips_title),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.semantics { },
        )
        listOf(
            R.string.quit_sos_tip_water,
            R.string.quit_sos_tip_move,
            R.string.quit_sos_tip_wait,
            R.string.quit_sos_tip_call,
        ).forEach { tip ->
            Text(
                text = stringResource(tip),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.quit_sos_support),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = LifeTrackerTheme.spacing.sm),
        )
    }
}

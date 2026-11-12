package com.example.lifet.core.designsystem.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.lifet.core.designsystem.R
import com.example.lifet.core.designsystem.preview.LTPreviews
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.util.rememberReducedMotion
import kotlin.math.roundToInt

private const val FULL_SWEEP = 360f
private const val START_ANGLE = -90f
private const val ANIMATION_MS = 800
private const val PERCENT = 100
private val RING_GAP = 4.dp
private const val PREVIEW_OUTER = 0.72f
private const val PREVIEW_INNER = 0.4f

/** Un aro del anillo: [progress] va de 0 a 1 (los valores fuera de rango se acotan). */
public data class RingSpec(
    val progress: Float,
    val color: Color,
)

/**
 * Anillo de progreso animado con uno o varios aros concéntricos (el primero es el exterior).
 * Se anima al entrar, salvo que el usuario haya eliminado las animaciones del sistema.
 * [content] se dibuja en el centro (cifra, icono…).
 */
@Composable
public fun LTProgressRing(
    rings: List<RingSpec>,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
    strokeWidth: Dp = 14.dp,
    contentDescription: String? = null,
    content: @Composable () -> Unit = {},
) {
    val reduced = rememberReducedMotion()
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val percent = rings.firstOrNull()?.let { (it.progress.coerceIn(0f, 1f) * PERCENT).roundToInt() }
    val description = contentDescription ?: percent?.let { stringResource(R.string.lt_progress_percent, it) }
    val animated =
        rings.map { ring ->
            val value by animateFloatAsState(
                targetValue = ring.progress.coerceIn(0f, 1f),
                animationSpec = if (reduced) tween(0) else tween(ANIMATION_MS, easing = FastOutSlowInEasing),
                label = "ring",
            )
            value
        }

    Box(
        modifier =
            modifier
                .size(size)
                .semantics(mergeDescendants = true) { description?.let { this.contentDescription = it } },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size)) {
            val stroke = strokeWidth.toPx()
            val style = Stroke(width = stroke, cap = StrokeCap.Round)
            rings.forEachIndexed { index, ring ->
                val inset = index * (stroke + RING_GAP.toPx()) + stroke / 2
                val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
                if (arcSize.width > 0f) {
                    val topLeft = Offset(inset, inset)
                    drawArc(track, 0f, FULL_SWEEP, false, topLeft, arcSize, style = style)
                    if (animated[index] > 0f) {
                        val sweep = FULL_SWEEP * animated[index]
                        drawArc(ring.color, START_ANGLE, sweep, false, topLeft, arcSize, style = style)
                    }
                }
            }
        }
        content()
    }
}

@LTPreviews
@Composable
private fun LTProgressRingPreview() {
    LifeTrackerTheme {
        val colors = LifeTrackerTheme.areaColors
        LTProgressRing(
            rings = listOf(RingSpec(PREVIEW_OUTER, colors.nutrition), RingSpec(PREVIEW_INNER, colors.fitness)),
        )
    }
}

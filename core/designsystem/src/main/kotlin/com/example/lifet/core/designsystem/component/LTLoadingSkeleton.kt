package com.example.lifet.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.lifet.core.designsystem.R
import com.example.lifet.core.designsystem.preview.LTPreviews
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.util.rememberReducedMotion

private const val SHIMMER_MS = 1100
private const val SHORT_LINE_FRACTION = 0.7f
private const val SHIMMER_MIN_ALPHA = 0.35f
private const val SHIMMER_MAX_ALPHA = 0.9f

/**
 * Esqueleto de carga: bloques que laten suavemente. Si el usuario eliminó las animaciones
 * del sistema, se queda estático. Sustituye a los spinners.
 */
@Composable
public fun LTLoadingSkeleton(
    modifier: Modifier = Modifier,
    lines: Int = 4,
) {
    val reduced = rememberReducedMotion()
    val alpha by
        if (reduced) {
            androidx.compose.runtime.rememberUpdatedState(SHIMMER_MAX_ALPHA)
        } else {
            rememberInfiniteTransition(label = "skeleton").animateFloat(
                initialValue = SHIMMER_MIN_ALPHA,
                targetValue = SHIMMER_MAX_ALPHA,
                animationSpec = infiniteRepeatable(tween(SHIMMER_MS, easing = LinearEasing), RepeatMode.Reverse),
                label = "skeleton-alpha",
            )
        }
    val loading = stringResource(R.string.lt_loading)
    val color = MaterialTheme.colorScheme.surfaceContainerHighest

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(LifeTrackerTheme.spacing.md)
                .semantics { contentDescription = loading },
        verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.md),
    ) {
        SkeletonBlock(height = 120.dp, alpha = alpha, color = color)
        repeat(lines) { index ->
            val width = if (index % 2 == 0) 1f else SHORT_LINE_FRACTION
            SkeletonBlock(height = 20.dp, alpha = alpha, color = color, widthFraction = width)
        }
    }
}

@Composable
private fun SkeletonBlock(
    height: Dp,
    alpha: Float,
    color: androidx.compose.ui.graphics.Color,
    widthFraction: Float = 1f,
) {
    androidx.compose.foundation.layout.Box(
        Modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(MaterialTheme.shapes.medium)
            .background(color.copy(alpha = alpha)),
    )
}

@LTPreviews
@Composable
private fun LTLoadingSkeletonPreview() {
    LifeTrackerTheme { LTLoadingSkeleton() }
}

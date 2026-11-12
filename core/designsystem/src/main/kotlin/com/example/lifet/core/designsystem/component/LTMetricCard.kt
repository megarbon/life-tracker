package com.example.lifet.core.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import com.example.lifet.core.designsystem.preview.LTPreviews
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme

private const val VALUE_ANIMATION_MS = 220
private const val PREVIEW_PROGRESS = 0.65f

/**
 * Tarjeta de resumen de un área: icono con el color del área, título, cifra héroe y contexto.
 * La cifra se anima al cambiar. Si recibe [onClick] es un único objetivo táctil (≥ 48 dp).
 */
@Composable
public fun LTMetricCard(
    icon: ImageVector,
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    subtitle: String? = null,
    progress: Float? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    val summary = listOfNotNull(title, value, subtitle).joinToString(". ")
    val cardModifier = modifier.semantics(mergeDescendants = true) { contentDescription = summary }
    val body: @Composable () -> Unit = {
        MetricCardBody(icon, title, value, accent, subtitle, progress)
    }
    if (onClick != null) {
        Card(onClick = onClick, modifier = cardModifier, colors = colors) { body() }
    } else {
        Card(modifier = cardModifier, colors = colors) { body() }
    }
}

@Composable
private fun MetricCardBody(
    icon: ImageVector,
    title: String,
    value: String,
    accent: Color,
    subtitle: String?,
    progress: Float?,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(LifeTrackerTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm),
        ) {
            Icon(icon, contentDescription = null, tint = accent)
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        AnimatedContent(
            targetState = value,
            transitionSpec = {
                val spec = tween<IntOffset>(VALUE_ANIMATION_MS)
                val fade = tween<Float>(VALUE_ANIMATION_MS)
                val enter = slideInVertically(spec) { it / 2 } + fadeIn(fade)
                val exit = slideOutVertically(spec) { -it / 2 } + fadeOut(fade)
                enter togetherWith exit
            },
            label = "metric-value",
        ) { current ->
            Text(current, style = MaterialTheme.typography.displaySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (progress != null) {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = accent,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
        }
    }
}

@LTPreviews
@Composable
private fun LTMetricCardPreview() {
    LifeTrackerTheme {
        LTMetricCard(
            icon = Icons.Rounded.AccountBalanceWallet,
            title = "Disponible este mes",
            value = "1.240,50 €",
            subtitle = "Quedan 340 € de presupuesto",
            progress = PREVIEW_PROGRESS,
            accent = LifeTrackerTheme.areaColors.finance,
            onClick = {},
        )
    }
}

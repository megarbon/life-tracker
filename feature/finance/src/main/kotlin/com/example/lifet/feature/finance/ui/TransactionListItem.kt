package com.example.lifet.feature.finance.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.theme.MinTouchTarget

private val ICON_BADGE = 40.dp

/**
 * Fila de movimiento: icono de categoría, título, contexto y el importe a la derecha. Los ingresos llevan
 * el color del área y los previstos van en cursiva; el signo del importe nunca depende sólo del color.
 */
@Composable
internal fun TransactionListItem(
    title: String,
    subtitle: String,
    amount: String,
    icon: ImageVector,
    positive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    planned: Boolean = false,
) {
    val accent = LifeTrackerTheme.areaColors.finance
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = MinTouchTarget)
                .clickable(onClick = onClick)
                .clearAndSetSemantics {
                    contentDescription = "$title. $subtitle. $amount"
                    role = Role.Button
                }.padding(vertical = LifeTrackerTheme.spacing.sm, horizontal = LifeTrackerTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.md),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(ICON_BADGE),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = accent)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontStyle = if (planned) FontStyle.Italic else FontStyle.Normal,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = amount,
            style = MaterialTheme.typography.titleMedium,
            color = if (positive) accent else MaterialTheme.colorScheme.onSurface,
            fontStyle = if (planned) FontStyle.Italic else FontStyle.Normal,
        )
    }
}

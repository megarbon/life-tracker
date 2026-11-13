package com.example.lifet.feature.work.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.model.work.WorkProject
import com.example.lifet.feature.work.R
import com.example.lifet.feature.work.color

private val DOT_SIZE = 12.dp

/** Punto con el color de un proyecto. Decorativo: el nombre siempre va al lado. */
@Composable
internal fun ColorDot(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier.size(DOT_SIZE).clip(CircleShape).background(color))
}

/** Selector de proyecto: "Sin proyecto" más un chip por proyecto, con su color y su nombre. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProjectPicker(
    projects: List<WorkProject>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm),
    ) {
        FilterChip(
            selected = selectedId == null,
            onClick = { onSelect(null) },
            label = { Text(stringResource(R.string.work_none)) },
        )
        projects.forEach { project ->
            FilterChip(
                selected = project.id == selectedId,
                onClick = { onSelect(project.id) },
                label = { Text(project.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = { ColorDot(project.color.color) },
            )
        }
    }
}

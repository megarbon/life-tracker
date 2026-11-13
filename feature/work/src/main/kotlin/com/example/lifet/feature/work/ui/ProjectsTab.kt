package com.example.lifet.feature.work.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextOverflow
import com.example.lifet.core.designsystem.component.LTEmptyState
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.theme.MinTouchTarget
import com.example.lifet.core.model.Area
import com.example.lifet.core.model.work.ProjectTotals
import com.example.lifet.core.model.work.WorkOverview
import com.example.lifet.core.ui.icon
import com.example.lifet.feature.work.DurationFormat
import com.example.lifet.feature.work.R
import com.example.lifet.feature.work.WorkAction
import com.example.lifet.feature.work.color
import com.example.lifet.feature.work.labelRes
import com.example.lifet.feature.work.rememberDurationFormat

/** Proyectos: lista con color, horas de la semana y totales; tocar uno lo edita. El alta va por el botón flotante. */
@Composable
internal fun ProjectsTab(
    overview: WorkOverview,
    contentPadding: PaddingValues,
    onAction: (WorkAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val durations = rememberDurationFormat()
    if (overview.projects.isEmpty()) {
        LTEmptyState(
            icon = Area.WORK.icon,
            title = stringResource(R.string.work_projects_empty_title),
            message = stringResource(R.string.work_projects_empty_message),
            actionLabel = stringResource(R.string.work_add_project),
            onAction = { onAction(WorkAction.OpenProject()) },
            modifier = modifier,
        )
        return
    }
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.xs),
    ) {
        item(key = "chart", contentType = "chart") { ProjectSharesChart(overview.projectShares) }
        items(overview.projects, key = { "project-${it.project.id}" }, contentType = { "project" }) { totals ->
            ProjectRow(totals, durations, onClick = { onAction(WorkAction.OpenProject(totals.project)) })
        }
    }
}

@Composable
private fun ProjectRow(
    totals: ProjectTotals,
    durations: DurationFormat,
    onClick: () -> Unit,
) {
    val project = totals.project
    val hours =
        stringResource(
            R.string.work_project_row_hours,
            durations.format(totals.weekSeconds),
            durations.format(totals.totalSeconds),
        )
    val focus =
        totals.focusSeconds
            .takeIf {
                it > 0L
            }?.let { stringResource(R.string.work_project_focus_hours, durations.format(it)) }
    val colorName = stringResource(project.color.labelRes)
    val description = listOfNotNull(project.name, colorName, hours, focus).joinToString(". ")
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = MinTouchTarget)
                .clickable(onClick = onClick)
                .clearAndSetSemantics {
                    contentDescription = description
                    role = Role.Button
                }.padding(vertical = LifeTrackerTheme.spacing.sm, horizontal = LifeTrackerTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.md),
    ) {
        ColorDot(project.color.color)
        Column(Modifier.weight(1f)) {
            Text(
                project.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = hours,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            focus?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

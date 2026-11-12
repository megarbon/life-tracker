package com.example.lifet.feature.fitness.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import com.example.lifet.core.designsystem.component.LTSectionHeader
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.theme.MinTouchTarget
import com.example.lifet.core.model.fitness.BodyWeight
import com.example.lifet.core.model.fitness.DailySteps
import com.example.lifet.core.model.fitness.FitnessOverview
import com.example.lifet.feature.fitness.FitnessFormatters
import com.example.lifet.feature.fitness.R
import com.example.lifet.feature.fitness.rememberFitnessFormatters

/** Filas que se muestran de entrada en cada lista; "Mostrar más" añade otras tantas. */
private const val PAGE = 14

/** Cuerpo: gráfica de peso con media móvil, lista de pesos y pasos manuales. */
@Composable
internal fun BodyTab(
    overview: FitnessOverview,
    contentPadding: PaddingValues,
    onAddWeight: () -> Unit,
    onEditWeight: (BodyWeight) -> Unit,
    onAddSteps: () -> Unit,
    onEditSteps: (DailySteps) -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatters = rememberFitnessFormatters()
    var weightsShown by rememberSaveable { mutableIntStateOf(PAGE) }
    var stepsShown by rememberSaveable { mutableIntStateOf(PAGE) }
    val showMoreWeights = { weightsShown += PAGE }
    val showMoreSteps = { stepsShown += PAGE }
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm),
    ) {
        item(key = "weight-chart") { WeightChart(overview.weight, formatters) }
        weightSection(overview.weightEntries, weightsShown, formatters, onAddWeight, onEditWeight, showMoreWeights)
        stepsSection(overview, stepsShown, formatters, onAddSteps, onEditSteps, showMoreSteps)
    }
}

private fun LazyListScope.weightSection(
    weights: List<BodyWeight>,
    shown: Int,
    formatters: FitnessFormatters,
    onAdd: () -> Unit,
    onEdit: (BodyWeight) -> Unit,
    onShowMore: () -> Unit,
) {
    item(key = "weight-header") {
        LTSectionHeader(
            title = stringResource(R.string.fitness_body_weight_header),
            actionLabel = stringResource(R.string.fitness_body_weight_add),
            onAction = onAdd,
        )
    }
    if (weights.isEmpty()) {
        item(key = "weight-empty") { MutedText(stringResource(R.string.fitness_body_weight_empty)) }
    }
    items(weights.take(shown), key = { "w-${it.date}" }) { entry ->
        BodyRow(
            text =
                stringResource(
                    R.string.fitness_body_row,
                    formatters.date(entry.date),
                    stringResource(R.string.fitness_weight_unit, formatters.kilos(entry.weightGrams)),
                ),
            onClick = { onEdit(entry) },
        )
    }
    if (weights.size > shown) item(key = "weight-more") { ShowMore(onShowMore) }
}

private fun LazyListScope.stepsSection(
    overview: FitnessOverview,
    shown: Int,
    formatters: FitnessFormatters,
    onAdd: () -> Unit,
    onEdit: (DailySteps) -> Unit,
    onShowMore: () -> Unit,
) {
    val steps = overview.stepsEntries
    item(key = "steps-header") {
        LTSectionHeader(
            title = stringResource(R.string.fitness_body_steps_header),
            actionLabel = stringResource(R.string.fitness_body_steps_add),
            onAction = onAdd,
        )
    }
    overview.stepsWeekAverage?.let { average ->
        item(key = "steps-average") {
            MutedText(stringResource(R.string.fitness_body_steps_week, formatters.number(average)))
        }
    }
    if (steps.isEmpty()) {
        item(key = "steps-empty") { MutedText(stringResource(R.string.fitness_body_steps_empty)) }
    }
    items(steps.take(shown), key = { "s-${it.date}" }) { entry ->
        BodyRow(
            text =
                stringResource(
                    R.string.fitness_body_row_steps,
                    formatters.date(entry.date),
                    formatters.number(entry.steps),
                ),
            onClick = { onEdit(entry) },
        )
    }
    if (steps.size > shown) item(key = "steps-more") { ShowMore(onShowMore) }
}

@Composable
private fun BodyRow(
    text: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = MinTouchTarget)
                .clickable(onClick = onClick)
                .clearAndSetSemantics {
                    contentDescription = text
                    role = Role.Button
                }.padding(horizontal = LifeTrackerTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ShowMore(onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.heightIn(min = MinTouchTarget)) {
        Text(stringResource(R.string.fitness_show_more))
    }
}

@Composable
private fun MutedText(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

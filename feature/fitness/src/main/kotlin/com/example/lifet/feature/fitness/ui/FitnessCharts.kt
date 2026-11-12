package com.example.lifet.feature.fitness.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.example.lifet.core.designsystem.component.ChartKind
import com.example.lifet.core.designsystem.component.ChartPoint
import com.example.lifet.core.designsystem.component.LTCalendarHeatmap
import com.example.lifet.core.designsystem.component.LTChartCard
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.model.DateRange
import com.example.lifet.core.model.DateRangeUnit
import com.example.lifet.core.model.fitness.ExerciseProgress
import com.example.lifet.core.model.fitness.FitnessOverview
import com.example.lifet.core.model.fitness.FitnessRules
import com.example.lifet.core.model.fitness.WeightProgress
import com.example.lifet.feature.fitness.FitnessFormatters
import com.example.lifet.feature.fitness.R
import com.example.lifet.feature.fitness.quantity
import com.example.lifet.feature.fitness.rememberFitnessFormatters
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import kotlin.math.absoluteValue
import kotlin.math.roundToLong

/** Por debajo de esta variación de la media móvil (en gramos) se dice que el peso está estable. */
private const val STABLE_WEIGHT_GRAMS = 200L

/** Meses que cubre el calendario de actividad. */
private const val HEATMAP_MONTHS = 3

/** Sesiones de un ejercicio y semanas de peso que dibujan las gráficas; más etiquetas no caben en el eje X. */
private const val EXERCISE_CHART_SESSIONS = 12
private const val WEIGHT_CHART_WEEKS = 16

private const val GRAMS_PER_KILO = FitnessRules.GRAMS_PER_KILO.toDouble()

/** Gráfica de minutos activos por semana (COLUMNS) con una frase que compara con la media de las 4 anteriores. */
@Composable
internal fun WeeklyMinutesChart(
    overview: FitnessOverview,
    modifier: Modifier = Modifier,
) {
    val formatters = rememberFitnessFormatters()
    val points =
        remember(overview.weeks, formatters) {
            overview.weeks.map { ChartPoint(it.weekStart.format(formatters.shortDate), it.minutes.toDouble()) }
        }
    val current = overview.weeks.lastOrNull()?.minutes ?: 0
    val previous = overview.previousWeeksAverage
    val headline =
        when {
            previous == null && current == 0 -> stringResource(R.string.fitness_chart_weeks_none)
            previous == null -> stringResource(R.string.fitness_chart_weeks_current, current)
            current > previous -> stringResource(R.string.fitness_chart_weeks_up, current - previous)
            current < previous -> stringResource(R.string.fitness_chart_weeks_down, previous - current)
            else -> stringResource(R.string.fitness_chart_weeks_same)
        }
    val itemFormat = stringResource(R.string.fitness_chart_item)
    val minutesFormat = stringResource(R.string.fitness_chart_minutes)
    val summary =
        stringResource(
            R.string.fitness_chart_weeks_summary,
            points.joinToString(", ") { itemFormat.format(it.label, minutesFormat.format(it.value.toInt())) },
        )
    LTChartCard(
        title = stringResource(R.string.fitness_chart_weeks_title),
        headline = headline,
        summary = summary,
        points = points,
        kind = ChartKind.COLUMNS,
        color = LifeTrackerTheme.areaColors.fitness,
        valueFormatter = { minutesFormat.format(it.toInt()) },
        modifier = modifier,
    )
}

/**
 * Progresión de un ejercicio (LINE) en sus últimas sesiones: 1RM estimado en kg, o repeticiones si nunca se usó
 * carga. El titular compara la última sesión dibujada con la primera dibujada.
 */
@Composable
internal fun ExerciseProgressChart(
    exercise: ExerciseProgress,
    formatters: FitnessFormatters,
    modifier: Modifier = Modifier,
) {
    val bodyweight = exercise.isBodyweightOnly
    val sessions = remember(exercise) { exercise.sessions.takeLast(EXERCISE_CHART_SESSIONS) }
    val points =
        remember(sessions, formatters) {
            sessions.map { session ->
                val value =
                    if (bodyweight) {
                        (session.bestBodyweightReps ?: 0).toDouble()
                    } else {
                        (session.bestOneRepMaxGrams ?: 0L) / GRAMS_PER_KILO
                    }
                ChartPoint(session.date.format(formatters.shortDate), value)
            }
        }
    val unit = exerciseUnit(bodyweight, formatters)
    val itemFormat = stringResource(R.string.fitness_chart_item)
    LTChartCard(
        title =
            stringResource(
                if (bodyweight) R.string.fitness_chart_exercise_reps_title else R.string.fitness_chart_exercise_title,
                exercise.name,
            ),
        headline = exerciseHeadline(exercise, sessions.size, formatters),
        summary =
            stringResource(
                R.string.fitness_chart_exercise_summary,
                exercise.name,
                points.joinToString(", ") { itemFormat.format(it.label, unit(it.value)) },
            ),
        points = points,
        kind = ChartKind.LINE,
        color = LifeTrackerTheme.areaColors.fitness,
        valueFormatter = unit,
        modifier = modifier,
    )
}

private fun exerciseUnit(
    bodyweight: Boolean,
    formatters: FitnessFormatters,
): (Double) -> String =
    if (bodyweight) {
        { value -> value.toInt().toString() }
    } else {
        { value -> formatters.kilos((value * GRAMS_PER_KILO).roundToLong()) }
    }

@Composable
private fun exerciseHeadline(
    exercise: ExerciseProgress,
    shown: Int,
    formatters: FitnessFormatters,
): String {
    val window = exercise.sessions.takeLast(EXERCISE_CHART_SESSIONS)
    return if (exercise.isBodyweightOnly) {
        val first = window.firstOrNull()?.bestBodyweightReps ?: 0
        val last = window.lastOrNull()?.bestBodyweightReps ?: 0
        when {
            window.size < 2 -> {
                quantity(R.plurals.fitness_chart_reps_one, last)
            }

            last > first -> {
                stringResource(R.string.fitness_chart_reps_up, formatRepetitions(first), formatRepetitions(last))
            }

            else -> {
                quantity(R.plurals.fitness_chart_reps_same, last)
            }
        }
    } else {
        val first = window.firstOrNull()?.bestOneRepMaxGrams ?: 0L
        val last = window.lastOrNull()?.bestOneRepMaxGrams ?: 0L
        val delta = last - first
        when {
            window.size < 2 -> {
                stringResource(R.string.fitness_chart_exercise_one, formatters.kilos(last))
            }

            delta > 0 -> {
                pluralStringResource(R.plurals.fitness_chart_exercise_up, shown, formatters.kilos(delta), shown)
            }

            delta < 0 -> {
                pluralStringResource(R.plurals.fitness_chart_exercise_down, shown, formatters.kilos(-delta), shown)
            }

            else -> {
                stringResource(R.string.fitness_chart_exercise_same, formatters.kilos(last))
            }
        }
    }
}

/**
 * Peso con su media móvil de 7 días (LINE). Se dibuja la media, que es la tendencia y no el ruido diario, con un
 * punto por semana (el último de cada semana) para que las etiquetas del eje X no se amontonen.
 */
@Composable
internal fun WeightChart(
    weight: WeightProgress,
    formatters: FitnessFormatters,
    modifier: Modifier = Modifier,
) {
    val points =
        remember(weight, formatters) {
            weight.points
                .groupBy { it.date.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)) }
                .toSortedMap()
                .values
                .map { it.last() }
                .takeLast(WEIGHT_CHART_WEEKS)
                .map { ChartPoint(it.date.format(formatters.shortDate), it.movingAverageGrams / GRAMS_PER_KILO) }
        }
    val latest = weight.latest
    val change = weight.changeGrams
    val headline =
        when {
            latest == null -> {
                stringResource(R.string.fitness_chart_weight_none)
            }

            change == null -> {
                stringResource(R.string.fitness_chart_weight_current, formatters.kilos(latest.movingAverageGrams))
            }

            change.absoluteValue < STABLE_WEIGHT_GRAMS -> {
                stringResource(R.string.fitness_chart_weight_same)
            }

            change < 0 -> {
                stringResource(R.string.fitness_chart_weight_down, formatters.kilos(-change))
            }

            else -> {
                stringResource(R.string.fitness_chart_weight_up, formatters.kilos(change))
            }
        }
    val itemFormat = stringResource(R.string.fitness_chart_item)
    val unit: (Double) -> String = { value -> formatters.kilos((value * GRAMS_PER_KILO).roundToLong()) }
    LTChartCard(
        title = stringResource(R.string.fitness_chart_weight_title),
        headline = headline,
        summary =
            stringResource(
                R.string.fitness_chart_weight_summary,
                points.joinToString(", ") { itemFormat.format(it.label, unit(it.value)) },
            ),
        points = points,
        kind = ChartKind.LINE,
        color = LifeTrackerTheme.areaColors.fitness,
        valueFormatter = unit,
        modifier = modifier,
    )
}

/**
 * Calendario de actividad de los últimos meses con el color de Deporte. [LTCalendarHeatmap] dibuja un mes, así que
 * se compone mes a mes con su título; cada casilla es un día y su intensidad crece con los minutos entrenados.
 */
@Composable
internal fun ActivityHeatmapCard(
    overview: FitnessOverview,
    modifier: Modifier = Modifier,
) {
    val formatters = rememberFitnessFormatters()
    val today = overview.today
    val months = remember(today) { heatmapMonths(today) }
    val activeDays = months.sumOf { month -> month.daysUpTo(today).count { overview.intensity[it] != null } }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(LifeTrackerTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm),
        ) {
            Text(
                text = stringResource(R.string.fitness_heatmap_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = pluralStringResource(R.plurals.fitness_heatmap_headline, activeDays, activeDays, HEATMAP_MONTHS),
                style = MaterialTheme.typography.titleMedium,
            )
            months.forEach { month ->
                val title = formatters.monthTitle(month)
                val active = month.daysUpTo(today).count { overview.intensity[it] != null }
                Text(text = title, style = MaterialTheme.typography.labelLarge)
                LTCalendarHeatmap(
                    range = DateRange.containing(DateRangeUnit.MONTH, month.atDay(1)),
                    intensity = overview.intensity,
                    summary = pluralStringResource(R.plurals.fitness_heatmap_month_summary, active, title, active),
                    color = LifeTrackerTheme.areaColors.fitness,
                )
            }
        }
    }
}

/** El mes actual y los anteriores hasta cubrir [HEATMAP_MONTHS], del más antiguo al actual. */
internal fun heatmapMonths(today: LocalDate): List<YearMonth> =
    (HEATMAP_MONTHS - 1 downTo 0).map { YearMonth.from(today).minusMonths(it.toLong()) }

private fun YearMonth.daysUpTo(today: LocalDate): List<LocalDate> =
    (1..lengthOfMonth()).map { atDay(it) }.filter { it <= today }

/** "9 repeticiones" (o "1 repetición") como texto suelto, para incrustarlo en una frase. */
@Composable
private fun formatRepetitions(count: Int): String = quantity(R.plurals.fitness_record_reps, count)

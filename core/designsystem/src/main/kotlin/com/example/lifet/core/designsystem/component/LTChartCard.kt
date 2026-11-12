package com.example.lifet.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.lifet.core.designsystem.R
import com.example.lifet.core.designsystem.preview.LTPreviews
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.columnModel
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent

private const val TABLE_MAX_ROWS = 31

/** Un punto de la serie: [label] es la etiqueta del eje X ("lun", "mar", "03/26"). */
@Immutable
public data class ChartPoint(
    val label: String,
    val value: Double,
)

/** Tipo de gráfica que dibuja [LTChartCard]. */
public enum class ChartKind { COLUMNS, LINE }

/**
 * Tarjeta con gráfica (Vico) pensada para leerse sin mirar la gráfica:
 * - [headline]: conclusión en lenguaje natural ("Gastaste un 12 % menos que en febrero").
 * - [summary]: resumen que lee TalkBack en lugar de la gráfica.
 * - Botón "Ver como tabla": alternativa accesible con los mismos datos.
 *
 * Los datos son primitivos ([ChartPoint]); las features nunca dependen de Vico directamente.
 * [valueFormatter] formatea las cifras de la tabla y del eje Y (importes, kcal, horas…).
 */
@Composable
public fun LTChartCard(
    title: String,
    headline: String,
    summary: String,
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    kind: ChartKind = ChartKind.COLUMNS,
    color: Color = MaterialTheme.colorScheme.primary,
    valueFormatter: (Double) -> String = { it.toInt().toString() },
) {
    var showTable by rememberSaveable { mutableStateOf(false) }
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(LifeTrackerTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { heading() },
            )
            Text(text = headline, style = MaterialTheme.typography.titleMedium)
            if (showTable) {
                ChartTable(points, valueFormatter)
            } else {
                ChartCanvas(
                    points = points,
                    kind = kind,
                    color = color,
                    valueFormatter = valueFormatter,
                    modifier = Modifier.clearAndSetSemantics { contentDescription = summary },
                )
            }
            TextButton(onClick = { showTable = !showTable }) {
                Text(stringResource(if (showTable) R.string.lt_hide_table else R.string.lt_show_table))
            }
        }
    }
}

@Composable
private fun ChartCanvas(
    points: List<ChartPoint>,
    kind: ChartKind,
    color: Color,
    valueFormatter: (Double) -> String,
    modifier: Modifier = Modifier,
) {
    val producer = remember { CartesianChartModelProducer() }
    LaunchedEffect(points, kind) {
        // Vico necesita al menos un valor; una serie vacía se dibuja como un único cero.
        val values = points.map { it.value }.ifEmpty { listOf(0.0) }
        producer.runTransaction {
            when (kind) {
                ChartKind.COLUMNS -> columnModel { series(values) }
                ChartKind.LINE -> lineModel { series(values) }
            }
        }
    }
    val labels = points.map { it.label }
    val xFormatter =
        remember(labels) {
            CartesianValueFormatter { _, x, _ -> labels.getOrNull(x.toInt()) ?: "" }
        }
    val yFormatter = remember(valueFormatter) { CartesianValueFormatter { _, y, _ -> valueFormatter(y) } }
    val layer =
        when (kind) {
            ChartKind.COLUMNS -> {
                rememberColumnCartesianLayer(
                    columnProvider =
                        ColumnCartesianLayer.ColumnProvider.series(
                            rememberLineComponent(fill = Fill(color), thickness = 12.dp),
                        ),
                )
            }

            ChartKind.LINE -> {
                rememberLineCartesianLayer(
                    lineProvider =
                        LineCartesianLayer.LineProvider.series(
                            LineCartesianLayer.rememberLine(fill = LineCartesianLayer.LineFill.single(Fill(color))),
                        ),
                )
            }
        }
    CartesianChartHost(
        chart =
            rememberCartesianChart(
                layer,
                startAxis = VerticalAxis.rememberStart(valueFormatter = yFormatter),
                bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = xFormatter),
            ),
        modelProducer = producer,
        modifier = modifier.fillMaxWidth().height(200.dp),
    )
}

@Composable
private fun ChartTable(
    points: List<ChartPoint>,
    valueFormatter: (Double) -> String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.xs)) {
        points.takeLast(TABLE_MAX_ROWS).forEach { point ->
            Row(
                modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(point.label, style = MaterialTheme.typography.bodyMedium)
                Text(valueFormatter(point.value), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@LTPreviews
@Composable
private fun LTChartCardPreview() {
    LifeTrackerTheme {
        LTChartCard(
            title = "Gasto semanal",
            headline = "Gastaste un 12 % menos que la semana pasada",
            summary = "Gasto diario de lunes a domingo, máximo el sábado con 86 euros.",
            points = PREVIEW_POINTS,
            color = LifeTrackerTheme.areaColors.finance,
        )
    }
}

private val PREVIEW_POINTS =
    listOf("lun" to 12.0, "mar" to 30.5, "mié" to 8.0, "jue" to 22.0, "vie" to 41.0, "sáb" to 86.0, "dom" to 15.0)
        .map { (label, value) -> ChartPoint(label, value) }

package com.example.lifet.dev

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import com.example.lifet.R
import com.example.lifet.core.designsystem.component.ChartKind
import com.example.lifet.core.designsystem.component.ChartPoint
import com.example.lifet.core.designsystem.component.LTAmountField
import com.example.lifet.core.designsystem.component.LTCalendarHeatmap
import com.example.lifet.core.designsystem.component.LTChartCard
import com.example.lifet.core.designsystem.component.LTChipGroup
import com.example.lifet.core.designsystem.component.LTDateRangeSelector
import com.example.lifet.core.designsystem.component.LTLoadingSkeleton
import com.example.lifet.core.designsystem.component.LTMetricCard
import com.example.lifet.core.designsystem.component.LTProgressRing
import com.example.lifet.core.designsystem.component.LTQuickAddSheet
import com.example.lifet.core.designsystem.component.LTSectionHeader
import com.example.lifet.core.designsystem.component.LTStatTile
import com.example.lifet.core.designsystem.component.LTTopAppBar
import com.example.lifet.core.designsystem.component.RingSpec
import com.example.lifet.core.designsystem.component.showUndo
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.model.Area
import com.example.lifet.core.model.CurrencyCode
import com.example.lifet.core.model.DateRange
import com.example.lifet.core.model.DateRangeUnit
import com.example.lifet.core.ui.icon
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val RING_OUTER = 0.72f
private const val RING_INNER = 0.4f
private const val HEAT_MODULO = 4
private const val HEAT_STEP = 0.25f
private const val WEIGHT_BASE = 80
private const val WEIGHT_DIVISOR = 20
private val WEEK_POINTS =
    listOf("lun" to 12.0, "mar" to 30.5, "mié" to 8.0, "jue" to 22.0, "vie" to 41.0, "sáb" to 86.0, "dom" to 15.0)
        .map { (label, value) -> ChartPoint(label, value) }

/** Muestra todos los componentes LT* sobre datos de ejemplo para revisarlos en el móvil. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ComponentGalleryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbar = remember { SnackbarHostState() }
    val today = remember { LocalDate.now() }
    var range by remember { mutableStateOf(DateRange.containing(DateRangeUnit.MONTH, today)) }

    Scaffold(
        modifier = modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = { LTTopAppBar(stringResource(R.string.dev_gallery_title), onBack = onBack, scrollBehavior = scroll) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = LifeTrackerTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.md),
        ) {
            MetricsSection()
            ChartsSection(range = range, today = today, onRangeChange = { range = it })
            FormsSection(snackbar)
            LTSectionHeader("Carga")
            LTLoadingSkeleton()
        }
    }
}

@Composable
private fun MetricsSection() {
    val colors = LifeTrackerTheme.areaColors
    LTSectionHeader("Métricas")
    LTMetricCard(
        icon = Area.FINANCE.icon,
        title = "Disponible este mes",
        value = "1.240,50 €",
        subtitle = "Quedan 340 € de presupuesto",
        progress = RING_OUTER,
        accent = colors.finance,
        onClick = {},
    )
    LTStatTile(label = "Pasos", value = "8.432", supporting = "Objetivo 10.000")
    LTProgressRing(rings = listOf(RingSpec(RING_OUTER, colors.nutrition), RingSpec(RING_INNER, colors.fitness)))
}

@Composable
private fun ChartsSection(
    range: DateRange,
    today: LocalDate,
    onRangeChange: (DateRange) -> Unit,
) {
    val colors = LifeTrackerTheme.areaColors
    LTSectionHeader("Gráficas", actionLabel = "Ver todo", onAction = {})
    LTDateRangeSelector(range = range, today = today, onRangeChange = onRangeChange)
    LTChartCard(
        title = "Gasto diario",
        headline = "Gastaste un 12 % menos que la semana pasada",
        summary = "Gasto diario de lunes a domingo, máximo el sábado con 86 euros.",
        points = WEEK_POINTS,
        color = colors.finance,
    )
    LTChartCard(
        title = "Peso",
        headline = "Bajas 0,4 kg por semana",
        summary = "Línea de peso con tendencia descendente.",
        points = WEEK_POINTS.map { it.copy(value = WEIGHT_BASE - it.value / WEIGHT_DIVISOR) },
        kind = ChartKind.LINE,
        color = colors.fitness,
    )
    LTCalendarHeatmap(
        range = range,
        intensity =
            range.days
                .withIndex()
                .filter { it.index % HEAT_MODULO != 0 }
                .associate { it.value to ((it.index % HEAT_MODULO) + 1) * HEAT_STEP },
        summary = "Actividad en la mayoría de los días del mes",
        color = colors.mood,
    )
}

@Composable
private fun FormsSection(snackbar: SnackbarHostState) {
    val scope = rememberCoroutineScope()
    var amount by remember { mutableStateOf("") }
    var meal by remember { mutableStateOf("Comida") }
    var sheetOpen by remember { mutableStateOf(false) }
    LTSectionHeader("Formularios")
    LTAmountField(text = amount, onTextChange = { amount = it }, currency = CurrencyCode.EUR, label = "Importe")
    LTChipGroup(
        options = listOf("Desayuno", "Comida", "Cena", "Snack"),
        selected = meal,
        label = { it },
        onSelect = { meal = it },
    )
    Button(onClick = { sheetOpen = true }, modifier = Modifier.fillMaxWidth()) { Text("Abrir hoja rápida") }
    Button(
        onClick = { scope.launch { snackbar.showUndo("Gasto eliminado", "Deshacer") {} } },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Mostrar snackbar con deshacer") }
    if (sheetOpen) {
        LTQuickAddSheet(
            title = "Añadir comida",
            onDismiss = { sheetOpen = false },
            recents = listOf("Avena con fruta", "Café con leche", "Tortilla"),
        ) { Text("Formulario de ejemplo") }
    }
}

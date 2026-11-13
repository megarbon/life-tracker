package com.example.lifet.feature.dashboard

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifet.core.designsystem.component.LTLoadingSkeleton
import com.example.lifet.core.designsystem.component.LTSectionHeader
import com.example.lifet.core.designsystem.preview.LTPreviews
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.model.CurrencyCode
import com.example.lifet.core.model.Money
import com.example.lifet.core.model.finance.FinanceTodaySummary
import com.example.lifet.core.model.mood.MoodLevel
import com.example.lifet.core.model.mood.MoodTodaySummary
import com.example.lifet.core.model.nutrition.Nutrients
import com.example.lifet.core.model.nutrition.NutritionGoals
import com.example.lifet.core.model.nutrition.NutritionTodaySummary
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun DashboardRoute(
    onOpenMood: () -> Unit,
    onOpenFinance: () -> Unit,
    onOpenNutrition: () -> Unit,
    onOpenFitness: () -> Unit,
    onOpenQuit: () -> Unit,
    onOpenWork: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val texts by rememberUpdatedState(DashboardMessageTexts.rememberTexts())
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            // Un aviso nuevo sustituye al anterior en lugar de encolarse (tres toques en +250 no dan tres avisos).
            snackbar.currentSnackbarData?.dismiss()
            val undoable = texts.isUndoable(message)
            val result =
                snackbar.showSnackbar(
                    message = texts.textFor(message),
                    actionLabel = if (undoable) texts.undo else null,
                    duration = SnackbarDuration.Short,
                )
            if (result == SnackbarResult.ActionPerformed) viewModel.undo(message)
        }
    }
    DashboardScreen(
        state = state,
        snackbar = snackbar,
        clock = viewModel.clock,
        actions =
            DashboardActions(
                onOpenMood = onOpenMood,
                onOpenFinance = onOpenFinance,
                onOpenNutrition = onOpenNutrition,
                onOpenFitness = onOpenFitness,
                onOpenQuit = onOpenQuit,
                onOpenWork = onOpenWork,
                onCheckIn = viewModel::checkIn,
                onAddWater = viewModel::addGlassOfWater,
            ),
        modifier = modifier,
    )
}

/** Acciones que salen de la pantalla Hoy. */
internal data class DashboardActions(
    val onOpenMood: () -> Unit,
    val onOpenFinance: () -> Unit,
    val onOpenNutrition: () -> Unit,
    val onOpenFitness: () -> Unit,
    val onOpenQuit: () -> Unit,
    val onOpenWork: () -> Unit,
    val onCheckIn: (MoodLevel) -> Unit,
    val onAddWater: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DashboardScreen(
    state: DashboardUiState,
    snackbar: SnackbarHostState,
    clock: Clock,
    actions: DashboardActions,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { TopAppBar(title = { Text(stringResource(R.string.dashboard_title)) }) },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (state) {
                DashboardUiState.Loading -> LTLoadingSkeleton(Modifier.padding(LifeTrackerTheme.spacing.md))
                is DashboardUiState.Success -> DashboardContent(state, clock, actions)
            }
        }
    }
}

@Composable
private fun DashboardContent(
    state: DashboardUiState.Success,
    clock: Clock,
    actions: DashboardActions,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LifeTrackerTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.md),
    ) {
        Text(
            text = formatToday(state.today),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LTSectionHeader(stringResource(R.string.dashboard_section_areas))
        AreaCard(state.nutrition, R.string.dashboard_nutrition_title) {
            NutritionTodayCard(summary = it, onOpen = actions.onOpenNutrition, onAddWater = actions.onAddWater)
        }
        AreaCard(state.fitness, R.string.dashboard_fitness_title) {
            FitnessTodayCard(summary = it, onClick = actions.onOpenFitness)
        }
        AreaCard(state.finance, R.string.dashboard_finance_title) {
            FinanceTodayCard(summary = it, onClick = actions.onOpenFinance)
        }
        AreaCard(state.mood, R.string.dashboard_mood_title) {
            MoodTodayCard(summary = it, onOpen = actions.onOpenMood, onCheckIn = actions.onCheckIn)
        }
        AreaCard(state.work, R.string.dashboard_work_title) {
            WorkTodayCard(summary = it, clock = clock, onClick = actions.onOpenWork)
        }
        AreaCard(state.quit, R.string.dashboard_quit_title) {
            QuitTodayCard(summary = it, onClick = actions.onOpenQuit)
        }
    }
}

/** Dibuja la tarjeta del área si se pudo leer; si no, una tarjeta discreta sin afectar a las demás. */
@Composable
private fun <T> AreaCard(
    state: AreaState<T>,
    @StringRes title: Int,
    content: @Composable (T) -> Unit,
) {
    when (state) {
        is AreaState.Ready -> content(state.value)
        AreaState.Unavailable -> UnavailableAreaCard(title = stringResource(title))
    }
}

@Composable
private fun formatToday(date: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale) }
    return formatter.format(date)
}

@LTPreviews
@Composable
private fun DashboardScreenPreview() {
    val eur = CurrencyCode.EUR
    LifeTrackerTheme {
        DashboardScreen(
            state =
                DashboardUiState.Success(
                    today = LocalDate.parse("2026-03-10"),
                    mood = AreaState.Ready(MoodTodaySummary(null, 0, null, null, 0)),
                    finance =
                        AreaState.Ready(
                            FinanceTodaySummary(eur, Money(0, eur), Money(0, eur), null, hasData = false),
                        ),
                    nutrition = AreaState.Ready(previewNutrition()),
                    fitness = AreaState.Unavailable,
                    quit = AreaState.Ready(previewQuit()),
                    work = AreaState.Ready(previewWork()),
                ),
            snackbar = remember { SnackbarHostState() },
            clock = Clock.systemUTC(),
            actions = DashboardActions({}, {}, {}, {}, {}, {}, {}, {}),
        )
    }
}

@LTPreviews
@Composable
private fun DashboardScreenLoadingPreview() {
    LifeTrackerTheme {
        DashboardScreen(
            state = DashboardUiState.Loading,
            snackbar = remember { SnackbarHostState() },
            clock = Clock.systemUTC(),
            actions = DashboardActions({}, {}, {}, {}, {}, {}, {}, {}),
        )
    }
}

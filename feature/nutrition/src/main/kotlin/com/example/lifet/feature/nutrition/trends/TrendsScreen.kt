package com.example.lifet.feature.nutrition.trends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifet.core.designsystem.component.LTEmptyState
import com.example.lifet.core.designsystem.component.LTErrorState
import com.example.lifet.core.designsystem.component.LTLoadingSkeleton
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.model.Area
import com.example.lifet.core.model.nutrition.NutritionSeries
import com.example.lifet.core.ui.icon
import com.example.lifet.feature.nutrition.R
import com.example.lifet.feature.nutrition.ui.BackButton
import com.example.lifet.feature.nutrition.ui.MacroSplitCard
import com.example.lifet.feature.nutrition.ui.TrendChart
import com.example.lifet.feature.nutrition.ui.WeekKcalChart

@Composable
internal fun TrendsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrendsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TrendsScreen(state = state, onBack = onBack, onRetry = viewModel::onRetry, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TrendsScreen(
    state: TrendsUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nutrition_trends)) },
                navigationIcon = { BackButton(onBack) },
            )
        },
    ) { innerPadding ->
        when (state) {
            TrendsUiState.Loading -> {
                LTLoadingSkeleton(Modifier.padding(innerPadding))
            }

            TrendsUiState.Empty -> {
                LTEmptyState(
                    icon = Area.NUTRITION.icon,
                    title = stringResource(R.string.nutrition_trends_empty_title),
                    message = stringResource(R.string.nutrition_trends_empty_message),
                    modifier = Modifier.padding(innerPadding),
                )
            }

            is TrendsUiState.Error -> {
                LTErrorState(
                    message = stringResource(state.messageRes),
                    retryLabel = stringResource(R.string.nutrition_retry),
                    onRetry = onRetry,
                    modifier = Modifier.padding(innerPadding),
                )
            }

            is TrendsUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(LifeTrackerTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.md),
                ) {
                    item { WeekKcalChart(last7Days(state.series)) }
                    item { TrendChart(state.series) }
                    item { MacroSplitCard(state.macroSplit) }
                }
            }
        }
    }
}

/** Los últimos 7 días de la ventana de 30, para reutilizar la gráfica de columnas con los mismos datos. */
private fun last7Days(series: NutritionSeries): NutritionSeries {
    val days = series.days.takeLast(WEEK_DAYS)
    val start = days.firstOrNull()?.date ?: series.start
    val end = days.lastOrNull()?.date?.plusDays(1) ?: series.endExclusive
    return series.copy(start = start, endExclusive = end, days = days)
}

private const val WEEK_DAYS = 7

package com.example.lifet.feature.nutrition.diary

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.example.lifet.core.designsystem.component.ChartKind
import com.example.lifet.core.designsystem.component.ChartPoint
import com.example.lifet.core.designsystem.component.LTChartCard
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.feature.nutrition.R
import com.example.lifet.feature.nutrition.ui.rememberNutritionFormatter
import java.time.format.TextStyle

/** Gráfica de kcal de los últimos 7 días del Diario, construida con los datos que ya trae el estado. */
@Composable
internal fun DiaryWeekChart(
    state: DiaryUiState.Success,
    modifier: Modifier = Modifier,
) {
    val format = rememberNutritionFormatter()
    val locale = LocalConfiguration.current.locales[0]
    val points =
        remember(state.week, locale) {
            state.week.map { ChartPoint(it.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale), it.kcal) }
        }
    val logged = state.week.filter { it.logged }
    val goal = format.kcal(state.goals.kcal.toDouble())
    val loggedText = stringResource(R.string.nutrition_logged_days, logged.size, state.week.size)
    LTChartCard(
        title = stringResource(R.string.nutrition_week_chart_title),
        headline =
            if (logged.isEmpty()) {
                stringResource(R.string.nutrition_week_chart_empty)
            } else {
                val average = format.kcal(logged.map { it.kcal }.average())
                stringResource(R.string.nutrition_week_chart_headline, average, goal)
            },
        summary = stringResource(R.string.nutrition_week_chart_summary, goal, loggedText),
        points = points,
        modifier = modifier,
        kind = ChartKind.COLUMNS,
        color = LifeTrackerTheme.areaColors.nutrition,
        valueFormatter = { format.kcal(it) },
    )
}

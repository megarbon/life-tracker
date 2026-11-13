package com.example.lifet.feature.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.lifet.core.designsystem.component.LTMetricCard
import com.example.lifet.core.designsystem.preview.LTPreviews
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.util.format
import com.example.lifet.core.model.Area
import com.example.lifet.core.model.CurrencyCode
import com.example.lifet.core.model.Money
import com.example.lifet.core.model.quit.HabitKind
import com.example.lifet.core.model.quit.HabitMode
import com.example.lifet.core.model.quit.QuitTodaySummary
import com.example.lifet.core.ui.icon

private const val SECONDS_PER_DAY = 86_400L
private const val PREVIEW_CLEAN_DAYS = 47L
private const val PREVIEW_BEST_DAYS = 47L
private const val PREVIEW_SAVED = 28_200L

/**
 * Tarjeta de Dejar de fumar/beber en Hoy: tiempo limpio en días y dinero ahorrado. Es deliberadamente sobria:
 * nunca avisa en tono de alarma y una recaída no borra el récord. El contador en vivo vive dentro del área.
 */
@Composable
internal fun QuitTodayCard(
    summary: QuitTodaySummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val days = (summary.cleanSeconds / SECONDS_PER_DAY).toInt()
    val reducing = summary.primaryMode == HabitMode.REDUCE && summary.weeklyLimit != null
    val title = summary.primaryHabitName ?: stringResource(R.string.dashboard_quit_title)
    val value =
        if (summary.hasData && reducing) {
            stringResource(
                R.string.dashboard_quit_week_units,
                "${summary.weekUnits ?: 0}",
                "${summary.weeklyLimit ?: 0}",
            )
        } else if (summary.hasData) {
            pluralStringResource(R.plurals.dashboard_quit_days, days, days)
        } else {
            stringResource(R.string.dashboard_quit_empty_value)
        }
    val bestDays = (summary.bestStreakSeconds / SECONDS_PER_DAY).toInt()
    val subtitle =
        when {
            !summary.hasData -> {
                stringResource(R.string.dashboard_quit_empty)
            }

            reducing -> {
                stringResource(R.string.dashboard_quit_reducing_saved, summary.totalSaved.format())
            }

            summary.bestStreakSeconds > summary.cleanSeconds -> {
                stringResource(
                    R.string.dashboard_quit_saved_best,
                    summary.totalSaved.format(),
                    pluralStringResource(R.plurals.dashboard_quit_days, bestDays, bestDays),
                )
            }

            else -> {
                stringResource(R.string.dashboard_quit_saved, summary.totalSaved.format())
            }
        }
    val note = if (summary.hasOtherCurrencies) stringResource(R.string.dashboard_quit_other_currencies) else null
    LTMetricCard(
        icon = Area.QUIT.icon,
        title = title,
        value = value,
        subtitle = listOfNotNull(subtitle, note).joinToString(". "),
        progress = summary.nextMilestone?.progress,
        accent = LifeTrackerTheme.areaColors.quit,
        onClick = onClick,
        modifier = modifier,
    )
}

internal fun previewQuit(): QuitTodaySummary =
    QuitTodaySummary(
        hasData = true,
        primaryHabitName = "Tabaco",
        primaryKind = HabitKind.TOBACCO,
        primaryMode = HabitMode.QUIT,
        cleanSince = null,
        cleanSeconds = PREVIEW_CLEAN_DAYS * SECONDS_PER_DAY,
        bestStreakSeconds = PREVIEW_BEST_DAYS * SECONDS_PER_DAY,
        totalSaved = Money(PREVIEW_SAVED, CurrencyCode.EUR),
        hasOtherCurrencies = false,
        nextMilestone = null,
        cravingToday = false,
        cravingsToday = 0,
        consumedToday = false,
        weekUnits = null,
        weeklyLimit = null,
    )

@LTPreviews
@Composable
private fun QuitTodayCardPreview() {
    LifeTrackerTheme { QuitTodayCard(summary = previewQuit(), onClick = {}) }
}

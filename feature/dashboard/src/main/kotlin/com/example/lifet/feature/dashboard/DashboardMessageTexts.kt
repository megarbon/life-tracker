package com.example.lifet.feature.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource

/** Textos de los avisos de Hoy ya resueltos, para poder mostrarlos desde un efecto sin ser `@Composable`. */
@Immutable
internal class DashboardMessageTexts(
    private val moodSaved: String,
    private val moodFailed: String,
    private val waterSaved: String,
    private val waterFailed: String,
) {
    fun textFor(message: DashboardMessage): String =
        when (message) {
            DashboardMessage.MOOD_SAVED -> moodSaved
            DashboardMessage.MOOD_FAILED -> moodFailed
            DashboardMessage.WATER_SAVED -> waterSaved
            DashboardMessage.WATER_FAILED -> waterFailed
        }

    companion object {
        @Composable
        fun rememberTexts(): DashboardMessageTexts =
            DashboardMessageTexts(
                moodSaved = stringResource(R.string.dashboard_mood_saved),
                moodFailed = stringResource(R.string.dashboard_mood_failed),
                waterSaved = stringResource(R.string.dashboard_water_saved),
                waterFailed = stringResource(R.string.dashboard_water_failed),
            )
    }
}

@file:Suppress("MagicNumber", "TooManyFunctions") // Datos de ejemplo literales y una función por cada @LTPreviews.

package com.example.lifet.feature.work

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.lifet.core.designsystem.preview.LTPreviews
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.model.DateRange
import com.example.lifet.core.model.DateRangeUnit
import com.example.lifet.core.model.work.FocusKind
import com.example.lifet.core.model.work.FocusSession
import com.example.lifet.core.model.work.ProjectColor
import com.example.lifet.core.model.work.WorkBreak
import com.example.lifet.core.model.work.WorkCalculator
import com.example.lifet.core.model.work.WorkDataSet
import com.example.lifet.core.model.work.WorkDayNote
import com.example.lifet.core.model.work.WorkProject
import com.example.lifet.core.model.work.WorkShift
import kotlinx.coroutines.flow.emptyFlow
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

private val PREVIEW_ZONE: ZoneId = ZoneId.of("Europe/Madrid")
private val PREVIEW_NOW: Instant = LocalDateTime.parse("2026-03-12T12:30").atZone(PREVIEW_ZONE).toInstant()
private val PREVIEW_CLOCK: Clock = Clock.fixed(PREVIEW_NOW, PREVIEW_ZONE)
private val PREVIEW_TODAY: LocalDate = LocalDate.of(2026, 3, 12)
private const val PREVIEW_WEEKS = 12L
private const val LONG_TEXT_REPEATS = 8

private fun at(time: String): Instant = LocalDateTime.parse(time).atZone(PREVIEW_ZONE).toInstant()

private fun shift(
    id: String,
    start: String,
    end: String?,
    projectId: String? = null,
    breaks: List<WorkBreak> = emptyList(),
) = WorkShift(id, at(start), PREVIEW_ZONE, end?.let(::at), breaks, projectId)

private val PREVIEW_PROJECTS =
    listOf(
        WorkProject("web", "Rediseño web", ProjectColor.BLUE),
        WorkProject("app", "App móvil", ProjectColor.TEAL),
        WorkProject("admin", "Administración", ProjectColor.AMBER),
    )

/** Doce semanas de lunes a viernes con algo de variación, más la jornada de hoy en curso. */
private fun regularData(longTexts: Boolean): WorkDataSet {
    val firstMonday = LocalDate.of(2026, 3, 9).minusWeeks(PREVIEW_WEEKS)
    val shifts =
        (0 until PREVIEW_WEEKS).flatMap { week ->
            (0L..4L).map { day ->
                val date = firstMonday.plusWeeks(week).plusDays(day)
                val extra = ((week + day) % 3).toInt() * 30
                shift(
                    id = "s-$date",
                    start = "${date}T09:00",
                    end = "${date}T${17 + extra / 60}:${"%02d".format(extra % 60)}",
                    projectId = PREVIEW_PROJECTS[((week + day) % 3).toInt()].id,
                    breaks = listOf(WorkBreak(at("${date}T13:30"), at("${date}T14:15"))),
                )
            }
        }
    val today = shift("today", "2026-03-12T09:00", null, "web")
    val note = "Reunión con el cliente y revisión del presupuesto. ".repeat(if (longTexts) LONG_TEXT_REPEATS else 1)
    val focus =
        FocusSession("f1", FocusKind.FOCUS, at("2026-03-11T10:00"), PREVIEW_ZONE, 1_500, at("2026-03-11T10:25"), true)
    return WorkDataSet(
        shifts = shifts + today,
        projects =
            if (longTexts) {
                PREVIEW_PROJECTS.map { it.copy(name = it.name + " 🎨 ".repeat(LONG_TEXT_REPEATS)) }
            } else {
                PREVIEW_PROJECTS
            },
        focusSessions = listOf(focus),
        notes = listOf(WorkDayNote(PREVIEW_TODAY, note)),
    )
}

private fun stateOf(data: WorkDataSet): WorkUiState {
    val calculator = WorkCalculator(data, PREVIEW_NOW, PREVIEW_ZONE)
    val week = calculator.weekReport(DateRange.containing(DateRangeUnit.WEEK, PREVIEW_TODAY))
    val overview = calculator.overview()
    return if (overview.hasAnyRecord) WorkUiState.Success(overview, week) else WorkUiState.Empty(overview, week)
}

@Composable
private fun PreviewScreen(
    state: WorkUiState,
    tab: WorkTab = WorkTab.TODAY,
    editor: WorkEditor? = null,
) {
    LifeTrackerTheme {
        WorkScreen(
            state = state,
            editor = editor,
            effects = emptyFlow(),
            clock = PREVIEW_CLOCK,
            onAction = {},
            initialTab = tab,
        )
    }
}

@LTPreviews
@Composable
private fun WorkTodayPreview() {
    PreviewScreen(stateOf(regularData(longTexts = false)))
}

@LTPreviews
@Composable
private fun WorkTodayEdgeCasesPreview() {
    PreviewScreen(stateOf(regularData(longTexts = true)))
}

/** Pantalla pequeña (360 x 640): es donde primero se rompe un diseño. */
@Preview(name = "Pequeña", widthDp = 360, heightDp = 640, showBackground = true)
@Preview(name = "Pequeña 2x", widthDp = 360, heightDp = 640, fontScale = 2f, showBackground = true)
@Composable
private fun WorkTodaySmallPreview() {
    PreviewScreen(stateOf(regularData(longTexts = true)))
}

@Preview(name = "Oscuro", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WorkTodayDarkPreview() {
    PreviewScreen(stateOf(regularData(longTexts = false)))
}

@LTPreviews
@Composable
private fun WorkFocusPreview() {
    PreviewScreen(stateOf(regularData(longTexts = false)), tab = WorkTab.FOCUS)
}

@LTPreviews
@Composable
private fun WorkFocusRunningPreview() {
    val running =
        FocusSession("r", FocusKind.FOCUS, at("2026-03-12T12:10"), PREVIEW_ZONE, 1_500, title = "Informe trimestral")
    val data = regularData(longTexts = false)
    PreviewScreen(stateOf(data.copy(focusSessions = data.focusSessions + running)), tab = WorkTab.FOCUS)
}

@LTPreviews
@Composable
private fun WorkWeekPreview() {
    PreviewScreen(stateOf(regularData(longTexts = false)), tab = WorkTab.WEEK)
}

@LTPreviews
@Composable
private fun WorkProjectsPreview() {
    PreviewScreen(stateOf(regularData(longTexts = false)), tab = WorkTab.PROJECTS)
}

@LTPreviews
@Composable
private fun WorkEmptyPreview() {
    PreviewScreen(stateOf(WorkDataSet()))
}

@LTPreviews
@Composable
private fun WorkLoadingPreview() {
    PreviewScreen(WorkUiState.Loading)
}

@LTPreviews
@Composable
private fun WorkErrorPreview() {
    PreviewScreen(WorkUiState.Error(R.string.work_error_load))
}

@LTPreviews
@Composable
private fun WorkForgottenShiftPreview() {
    val data = regularData(longTexts = false)
    PreviewScreen(stateOf(data.copy(shifts = data.shifts + shift("forgot", "2026-03-10T09:00", null))))
}

@LTPreviews
@Composable
private fun WorkShiftSheetPreview() {
    val data = regularData(longTexts = false)
    val editor = WorkEditor.EditShift(null, "new", PREVIEW_TODAY, PREVIEW_ZONE, PREVIEW_NOW)
    PreviewScreen(stateOf(data), editor = editor)
}

@LTPreviews
@Composable
private fun WorkProjectSheetPreview() {
    PreviewScreen(
        stateOf(regularData(longTexts = false)),
        tab = WorkTab.PROJECTS,
        editor = WorkEditor.EditProject(null),
    )
}

@LTPreviews
@Composable
private fun WorkSettingsSheetPreview() {
    PreviewScreen(stateOf(regularData(longTexts = false)), tab = WorkTab.WEEK, editor = WorkEditor.EditSettings)
}

package com.example.lifet.feature.work

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.example.lifet.core.model.work.FocusKind
import com.example.lifet.core.model.work.ProjectColor
import com.example.lifet.core.model.work.ShiftIssue
import com.example.lifet.core.model.work.WorkClockIssue

@get:StringRes
internal val ShiftIssue.messageRes: Int
    get() =
        when (this) {
            ShiftIssue.START_IN_FUTURE -> R.string.work_issue_start_future
            ShiftIssue.END_IN_FUTURE -> R.string.work_issue_end_future
            ShiftIssue.END_BEFORE_START -> R.string.work_issue_end_before_start
            ShiftIssue.TOO_LONG -> R.string.work_issue_too_long
            ShiftIssue.NOTE_TOO_LONG -> R.string.work_issue_note_too_long
            ShiftIssue.TOO_MANY_BREAKS -> R.string.work_issue_too_many_breaks
            ShiftIssue.BREAK_END_BEFORE_START -> R.string.work_issue_break_end_before_start
            ShiftIssue.BREAK_OUTSIDE_SHIFT -> R.string.work_issue_break_outside
            ShiftIssue.BREAKS_OVERLAP -> R.string.work_issue_breaks_overlap
            ShiftIssue.OPEN_BREAK_IN_CLOSED_SHIFT -> R.string.work_issue_open_break_closed
            ShiftIssue.MORE_THAN_ONE_OPEN_BREAK -> R.string.work_issue_more_open_breaks
            ShiftIssue.OVERLAPS_OTHER_SHIFT -> R.string.work_issue_overlaps_shift
        }

@get:StringRes
internal val WorkClockIssue.messageRes: Int
    get() =
        when (this) {
            WorkClockIssue.ALREADY_CLOCKED_IN -> R.string.work_issue_already_clocked_in
            WorkClockIssue.NOT_CLOCKED_IN -> R.string.work_issue_not_clocked_in
            WorkClockIssue.ALREADY_ON_BREAK -> R.string.work_issue_already_on_break
            WorkClockIssue.NOT_ON_BREAK -> R.string.work_issue_not_on_break
            WorkClockIssue.SHIFT_NOT_FOUND -> R.string.work_issue_shift_not_found
            WorkClockIssue.TIMER_ALREADY_RUNNING -> R.string.work_issue_timer_running
            WorkClockIssue.NO_TIMER_RUNNING -> R.string.work_issue_no_timer
            WorkClockIssue.INVALID_DURATION -> R.string.work_issue_invalid_duration
            WorkClockIssue.TITLE_TOO_LONG -> R.string.work_issue_title_too_long
            WorkClockIssue.NAME_MISSING -> R.string.work_issue_name_missing
            WorkClockIssue.NAME_TOO_LONG -> R.string.work_issue_name_too_long
            WorkClockIssue.TOO_MANY_PROJECTS -> R.string.work_issue_too_many_projects
            WorkClockIssue.NOTE_TOO_LONG -> R.string.work_issue_note_too_long
            WorkClockIssue.TARGET_INVALID -> R.string.work_issue_target_invalid
            WorkClockIssue.NO_WORKING_DAYS -> R.string.work_issue_no_working_days
        }

/**
 * Mensaje de un rechazo del dominio. El `reason` lleva los nombres de los problemas separados por comas; se muestra el
 * primero que se conozca, que basta para que el usuario corrija una cosa cada vez.
 */
@StringRes
internal fun validationMessageRes(reason: String): Int {
    val names = reason.split(',').map { it.trim() }
    return names.firstNotNullOfOrNull { name ->
        ShiftIssue.entries.firstOrNull { it.name == name }?.messageRes
            ?: WorkClockIssue.entries.firstOrNull { it.name == name }?.messageRes
    } ?: R.string.work_error_invalid
}

@get:StringRes
internal val FocusKind.labelRes: Int
    get() =
        when (this) {
            FocusKind.FOCUS -> R.string.work_focus_kind_focus
            FocusKind.SHORT_BREAK -> R.string.work_focus_kind_short_break
            FocusKind.LONG_BREAK -> R.string.work_focus_kind_long_break
        }

@get:StringRes
internal val ProjectColor.labelRes: Int
    get() =
        when (this) {
            ProjectColor.BLUE -> R.string.work_color_blue
            ProjectColor.TEAL -> R.string.work_color_teal
            ProjectColor.GREEN -> R.string.work_color_green
            ProjectColor.AMBER -> R.string.work_color_amber
            ProjectColor.ORANGE -> R.string.work_color_orange
            ProjectColor.RED -> R.string.work_color_red
            ProjectColor.PINK -> R.string.work_color_pink
            ProjectColor.PURPLE -> R.string.work_color_purple
            ProjectColor.INDIGO -> R.string.work_color_indigo
            ProjectColor.GREY -> R.string.work_color_grey
        }

internal val ProjectColor.color: Color get() = Color(argb)

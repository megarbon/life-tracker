package com.example.lifet.feature.work

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.domain.work.ManageFocusTimer
import com.example.lifet.core.model.work.FocusKind
import com.example.lifet.core.model.work.FocusSession
import com.example.lifet.core.model.work.WorkOverview
import com.example.lifet.core.model.work.WorkProject
import com.example.lifet.core.model.work.WorkShift
import com.example.lifet.core.model.work.WorkWeekReport
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/**
 * Estado de la pantalla de Trabajo. [Empty] conserva los datos para poder usar el temporizador de foco y crear
 * proyectos aunque todavía no se haya fichado nunca.
 */
internal sealed interface WorkUiState {
    data object Loading : WorkUiState

    @Immutable
    data class Empty(
        val overview: WorkOverview,
        val week: WorkWeekReport,
    ) : WorkUiState

    @Immutable
    data class Success(
        val overview: WorkOverview,
        val week: WorkWeekReport,
    ) : WorkUiState

    data class Error(
        @param:StringRes val messageRes: Int,
    ) : WorkUiState
}

/**
 * Hoja abierta. Vive en el ViewModel para sobrevivir a una rotación. Los datos que dependen del reloj ([now], [zone],
 * [newId]) los estampa el ViewModel al abrir la hoja: la UI no conoce el reloj y el id se mantiene si la hoja se
 * recrea.
 */
internal sealed interface WorkEditor {
    data class EditShift(
        val existing: WorkShift?,
        val newId: String,
        val today: LocalDate,
        val zone: ZoneId,
        val now: Instant,
        /** Día con el que se rellena una jornada nueva: el que se tocó en Semana, o hoy. */
        val startDate: LocalDate = today,
    ) : WorkEditor

    /** Indicar a posteriori la hora de salida de una jornada que se quedó abierta. */
    data class CloseShift(
        val shift: WorkShift,
        val now: Instant,
    ) : WorkEditor

    data class EditProject(
        val existing: WorkProject?,
    ) : WorkEditor

    data class EditNote(
        val date: LocalDate,
        val text: String,
    ) : WorkEditor

    data object EditSettings : WorkEditor
}

/** Un elemento borrable; sirve tanto para borrar como para restaurar con "Deshacer". */
internal sealed interface WorkItem {
    data class Shift(
        val value: WorkShift,
    ) : WorkItem

    data class Project(
        val value: WorkProject,
    ) : WorkItem
}

/** Formulario enviado desde una hoja. `existing` es lo que se está editando, si hay algo. */
internal sealed interface WorkForm {
    data class Shift(
        val draft: ShiftDraft,
        val existing: WorkShift?,
        val zone: ZoneId,
    ) : WorkForm

    data class Close(
        val shift: WorkShift,
        val exitDate: LocalDate,
        val exitMinute: Int,
        val zone: ZoneId,
    ) : WorkForm

    data class Project(
        val draft: ProjectDraft,
    ) : WorkForm

    data class Note(
        val date: LocalDate,
        val text: String,
    ) : WorkForm

    data class Settings(
        val draft: SettingsDraft,
    ) : WorkForm
}

/** Eventos de un solo uso hacia la UI (snackbars, háptica). */
internal sealed interface WorkEffect {
    data class Message(
        @param:StringRes val messageRes: Int,
    ) : WorkEffect

    data class Deleted(
        @param:StringRes val messageRes: Int,
        val item: WorkItem,
    ) : WorkEffect

    data object Saved : WorkEffect

    /** Un intervalo de foco acabó: háptica y aviso de que toca el siguiente. */
    data class FocusFinished(
        val kind: FocusKind,
    ) : WorkEffect
}

internal sealed interface WorkAction {
    data object Retry : WorkAction

    data class ClockIn(
        val projectId: String?,
    ) : WorkAction

    data object StartBreak : WorkAction

    data object EndBreak : WorkAction

    data object ClockOut : WorkAction

    /**
     * Acciones que abren una hoja. Cada una sabe construir la suya con el reloj inyectado (la UI no conoce el reloj),
     * así el ViewModel las trata en una sola rama y `when` sigue siendo exhaustivo sin `else`.
     */
    sealed interface OpenEditor : WorkAction {
        fun editor(clock: Clock): WorkEditor
    }

    /** Abre la hoja de jornada: nueva (a mano) o editando [existing]. */
    data class OpenShift(
        val existing: WorkShift? = null,
        /** Día de una jornada nueva; `null` es hoy. */
        val date: LocalDate? = null,
    ) : OpenEditor {
        override fun editor(clock: Clock): WorkEditor = newShiftEditor(existing, clock, date)
    }

    data class OpenClose(
        val shift: WorkShift,
    ) : OpenEditor {
        override fun editor(clock: Clock): WorkEditor = WorkEditor.CloseShift(shift, clock.instant())
    }

    data class OpenProject(
        val existing: WorkProject? = null,
    ) : OpenEditor {
        override fun editor(clock: Clock): WorkEditor = WorkEditor.EditProject(existing)
    }

    data class OpenNote(
        val date: LocalDate,
        val text: String,
    ) : OpenEditor {
        override fun editor(clock: Clock): WorkEditor = WorkEditor.EditNote(date, text)
    }

    data object OpenSettings : OpenEditor {
        override fun editor(clock: Clock): WorkEditor = WorkEditor.EditSettings
    }

    data object Dismiss : WorkAction

    data class Save(
        val form: WorkForm,
    ) : WorkAction

    data class Delete(
        val item: WorkItem,
    ) : WorkAction

    data class Undo(
        val item: WorkItem,
    ) : WorkAction

    data class StartFocus(
        val kind: FocusKind,
        val minutes: Int,
        val projectId: String?,
        val title: String,
    ) : WorkAction

    data object StopFocus : WorkAction

    /** La pantalla ve que el intervalo agotó su tiempo: lo da por terminado. */
    data object FinishFocus : WorkAction

    /** Mueve la semana mostrada: `-1` la anterior, `1` la siguiente, `0` vuelve a la actual. */
    data class MoveWeek(
        val delta: Int,
    ) : WorkAction
}

/** Estampa "hoy", la zona y "ahora" con el reloj inyectado: la UI no conoce el reloj. */
internal fun newShiftEditor(
    existing: WorkShift?,
    clock: Clock,
    date: LocalDate? = null,
): WorkEditor.EditShift {
    val today = LocalDate.now(clock)
    return WorkEditor.EditShift(
        existing = existing,
        newId = UUID.randomUUID().toString(),
        today = today,
        zone = clock.zone,
        now = clock.instant(),
        // Nunca en el futuro: una jornada no puede empezar después de ahora.
        startDate = minOf(date ?: today, today),
    )
}

internal suspend fun WorkAction.StartFocus.startOn(focus: ManageFocusTimer): AppResult<FocusSession> =
    focus.start(kind, minutes, projectId, title)

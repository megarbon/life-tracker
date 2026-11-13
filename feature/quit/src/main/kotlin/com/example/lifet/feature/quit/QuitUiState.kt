package com.example.lifet.feature.quit

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.example.lifet.core.model.quit.CopingStrategy
import com.example.lifet.core.model.quit.QuitConsumption
import com.example.lifet.core.model.quit.QuitCraving
import com.example.lifet.core.model.quit.QuitEvent
import com.example.lifet.core.model.quit.QuitHabit
import com.example.lifet.core.model.quit.QuitOverview
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import com.example.lifet.core.model.quit.HabitKind as Kind

/** Estado de la pantalla. [Empty] es "aún no hay ningún hábito": invita a crear el primero. */
internal sealed interface QuitUiState {
    data object Loading : QuitUiState

    data object Empty : QuitUiState

    @Immutable
    data class Success(
        val overview: QuitOverview,
    ) : QuitUiState

    data class Error(
        @param:StringRes val messageRes: Int,
    ) : QuitUiState
}

/**
 * Hoja abierta. Vive en el ViewModel para sobrevivir a una rotación. [newId], [now], [today], [zone] y [nowMinute] los
 * estampa el ViewModel al abrir la hoja (la UI no conoce el reloj): colocan el registro "ahora", validan contra el
 * instante de apertura y mantienen el id estable si la hoja se recrea.
 */
internal sealed interface QuitEditor {
    /** Alta o edición de un antojo o de un consumo del hábito [habitId]. */
    data class Event(
        val habitId: String,
        val existing: QuitEvent?,
        val asConsumption: Boolean,
        val seedStrategy: CopingStrategy?,
        val newId: String,
        val now: Instant,
        val today: LocalDate,
        val zone: ZoneId,
        val nowMinute: Int,
    ) : QuitEditor

    /** Alta o edición de un hábito. [currency] es la divisa sugerida para uno nuevo. */
    data class Habit(
        val existing: QuitHabit?,
        val kind: Kind,
        val currency: String,
        val newId: String,
        val now: Instant,
        val today: LocalDate,
        val zone: ZoneId,
        val nowMinute: Int,
    ) : QuitEditor

    /** Respiración guiada para atravesar un antojo del hábito [habitId]. */
    data class Sos(
        val habitId: String,
    ) : QuitEditor
}

/** Un elemento borrable; sirve tanto para borrar como para restaurar con "Deshacer". */
internal sealed interface QuitItem {
    /** Un hábito se borra con todo su historial y se restaura con él. */
    data class Habit(
        val habit: QuitHabit,
        val cravings: List<QuitCraving>,
        val consumptions: List<QuitConsumption>,
    ) : QuitItem

    data class Craving(
        val value: QuitCraving,
    ) : QuitItem

    data class Consumption(
        val value: QuitConsumption,
    ) : QuitItem
}

/** Formulario enviado desde una hoja. `existing` es lo que se está editando, si hay algo. */
internal sealed interface QuitForm {
    data class Habit(
        val draft: HabitDraft,
        val existing: QuitHabit?,
    ) : QuitForm

    data class Craving(
        val draft: CravingDraft,
        val existing: QuitCraving?,
    ) : QuitForm

    data class Consumption(
        val draft: ConsumptionDraft,
        val existing: QuitConsumption?,
    ) : QuitForm
}

/** Eventos de un solo uso hacia la UI (snackbars, háptica). */
internal sealed interface QuitEffect {
    data class Message(
        @param:StringRes val messageRes: Int,
    ) : QuitEffect

    data class Deleted(
        @param:StringRes val messageRes: Int,
        val item: QuitItem,
    ) : QuitEffect

    /** Guardado correcto; [messageRes] es un mensaje amable de confirmación. */
    data class Saved(
        @param:StringRes val messageRes: Int,
    ) : QuitEffect
}

internal sealed interface QuitAction {
    data object Retry : QuitAction

    /** Vuelve a calcular con la hora actual: al volver a la app o al llegar a un hito. */
    data object Refresh : QuitAction

    /** Abre la hoja de antojo o consumo: nuevo, o editando [existing]. */
    data class OpenEvent(
        val habitId: String,
        val existing: QuitEvent? = null,
        val asConsumption: Boolean = false,
        val seedStrategy: CopingStrategy? = null,
    ) : QuitAction

    /** Abre la hoja de hábito: nuevo (de [kind]) o editando [existing]. */
    data class OpenHabit(
        val existing: QuitHabit? = null,
        val kind: Kind = Kind.TOBACCO,
        val currency: String = DEFAULT_CURRENCY,
    ) : QuitAction

    data class OpenSos(
        val habitId: String,
    ) : QuitAction

    /** Cierra la hoja abierta. */
    data object Dismiss : QuitAction

    data class Save(
        val form: QuitForm,
    ) : QuitAction

    data class Delete(
        val item: QuitItem,
    ) : QuitAction

    data class Undo(
        val item: QuitItem,
    ) : QuitAction

    companion object {
        const val DEFAULT_CURRENCY = "EUR"
    }
}

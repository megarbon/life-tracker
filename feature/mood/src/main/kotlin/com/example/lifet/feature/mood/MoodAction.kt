package com.example.lifet.feature.mood

import com.example.lifet.core.model.DateRange
import com.example.lifet.core.model.mood.MoodEntry
import com.example.lifet.core.model.mood.MoodLevel

/** Eventos de la UI de Ánimo hacia [MoodViewModel]. */
internal sealed interface MoodAction {
    /** Check-in de un toque desde la tarjeta rápida. */
    data class QuickCheckIn(
        val level: MoodLevel,
    ) : MoodAction

    data object OpenNewForm : MoodAction

    data class OpenEdit(
        val entry: MoodEntry,
    ) : MoodAction

    data class OpenDetail(
        val entry: MoodEntry,
    ) : MoodAction

    data object DismissOverlay : MoodAction

    data class UpdateForm(
        val transform: (MoodFormState) -> MoodFormState,
    ) : MoodAction

    data object SaveForm : MoodAction

    data class Delete(
        val entry: MoodEntry,
    ) : MoodAction

    data object Undo : MoodAction

    data class ChangeRange(
        val range: DateRange,
    ) : MoodAction

    data class ChangeYear(
        val year: Int,
    ) : MoodAction

    data object DismissSupport : MoodAction

    data object Retry : MoodAction
}

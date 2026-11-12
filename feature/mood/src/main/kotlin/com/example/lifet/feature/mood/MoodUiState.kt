package com.example.lifet.feature.mood

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.example.lifet.core.model.DateRange
import com.example.lifet.core.model.mood.MoodEntry
import com.example.lifet.core.model.mood.MoodSeries
import com.example.lifet.core.model.mood.MoodTodaySummary
import kotlinx.collections.immutable.ImmutableList
import java.time.LocalDate

/** Estado de los datos de la pantalla de Ánimo. El formulario y el detalle viven aparte en [MoodOverlay]. */
internal sealed interface MoodUiState {
    data object Loading : MoodUiState

    /** Todavía no hay ningún check-in. */
    data object Empty : MoodUiState

    data class Error(
        @param:StringRes val messageRes: Int,
    ) : MoodUiState

    @Immutable
    data class Success(
        val today: LocalDate,
        val summary: MoodTodaySummary,
        val series: MoodSeries,
        val journal: ImmutableList<MoodDayGroup>,
        val calendar: MoodCalendar,
        val supportVisible: Boolean,
    ) : MoodUiState {
        val range: DateRange get() = series.range
    }
}

/** Un día del diario con sus check-ins (más recientes primero) y su media. */
@Immutable
internal data class MoodDayGroup(
    val date: LocalDate,
    val average: Double,
    val entries: ImmutableList<MoodEntry>,
)

/**
 * Datos del calendario anual. [intensity] va de 0,1 (ánimo 1) a 1,0 (ánimo 5); un día sin dato no aparece.
 */
@Immutable
internal data class MoodCalendar(
    val range: DateRange,
    val intensity: Map<LocalDate, Float>,
    val loggedDays: Int,
    val average: Double?,
    val hasPreviousYear: Boolean,
    val hasNextYear: Boolean,
)

/** Superposición modal activa: formulario de check-in o detalle de una entrada. */
internal sealed interface MoodOverlay {
    data object None : MoodOverlay

    data class Form(
        val state: MoodFormState,
    ) : MoodOverlay

    data class Detail(
        val entry: MoodEntry,
    ) : MoodOverlay
}

/** Mensajes de un solo uso (snackbar). [Undoable] ofrece "Deshacer" para la última acción. */
internal sealed interface MoodEffect {
    data class Message(
        @param:StringRes val messageRes: Int,
        val success: Boolean = false,
    ) : MoodEffect

    data class Undoable(
        @param:StringRes val messageRes: Int,
    ) : MoodEffect
}

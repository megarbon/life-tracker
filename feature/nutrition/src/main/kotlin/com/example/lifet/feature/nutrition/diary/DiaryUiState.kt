package com.example.lifet.feature.nutrition.diary

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.example.lifet.core.model.nutrition.MealEntry
import com.example.lifet.core.model.nutrition.MealType
import com.example.lifet.core.model.nutrition.Nutrients
import com.example.lifet.core.model.nutrition.NutritionDiary
import com.example.lifet.core.model.nutrition.NutritionGoals
import com.example.lifet.core.model.nutrition.WaterEntry
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.time.LocalDate

/** Estado de la pantalla Diario. [Empty] es un día sin comidas ni agua; el resto de días muestran [Success]. */
internal sealed interface DiaryUiState {
    data object Loading : DiaryUiState

    @Immutable
    data class Empty(
        val date: LocalDate,
        val isToday: Boolean,
        val goals: NutritionGoals,
    ) : DiaryUiState

    @Immutable
    data class Success(
        val date: LocalDate,
        val isToday: Boolean,
        val consumed: Nutrients,
        val goals: NutritionGoals,
        val sections: ImmutableList<MealSection>,
        val water: ImmutableList<WaterEntry>,
        val week: ImmutableList<DayKcal>,
    ) : DiaryUiState {
        val waterMl: Int get() = water.sumOf { it.ml }
    }

    data class Error(
        @param:StringRes val messageRes: Int,
    ) : DiaryUiState
}

/** Alimentos de una comida del día con su energía total. */
@Immutable
internal data class MealSection(
    val type: MealType,
    val entries: ImmutableList<MealEntry>,
) {
    val kcal: Double get() = entries.sumOf { it.nutrients.kcal }
}

/** kcal de un día de la semana para la gráfica; [logged] distingue "0 kcal" de "no registrado". */
@Immutable
internal data class DayKcal(
    val date: LocalDate,
    val kcal: Double,
    val logged: Boolean,
)

/** Efectos de un solo uso (snackbars); nunca van en el estado. */
internal sealed interface DiaryEffect {
    data class ShowUndo(
        @param:StringRes val messageRes: Int,
        val undo: UndoAction,
    ) : DiaryEffect

    data class ShowError(
        @param:StringRes val messageRes: Int,
    ) : DiaryEffect
}

/** Todo lo que la pantalla Diario puede pedirle al ViewModel. */
internal sealed interface DiaryAction {
    data object PreviousDay : DiaryAction

    data object NextDay : DiaryAction

    data object GoToToday : DiaryAction

    data object Retry : DiaryAction

    data object AddWater : DiaryAction

    data object RemoveLastWater : DiaryAction

    data object RepeatYesterday : DiaryAction

    data class Delete(
        val entry: MealEntry,
    ) : DiaryAction

    data class Update(
        val entry: MealEntry,
        val grams: Int,
        val mealType: MealType,
    ) : DiaryAction

    /** Se acaba de añadir algo desde el detalle o la hoja rápida: se ofrece deshacer. */
    data class EntriesAdded(
        val ids: List<String>,
    ) : DiaryAction

    data class Undo(
        val action: UndoAction,
    ) : DiaryAction
}

/** Qué hay que hacer para deshacer lo último. */
internal sealed interface UndoAction {
    data class DeleteMeals(
        val ids: List<String>,
    ) : UndoAction

    data class RestoreMeals(
        val entries: List<MealEntry>,
    ) : UndoAction

    data class DeleteWater(
        val entry: WaterEntry,
    ) : UndoAction

    data class RestoreWater(
        val entry: WaterEntry,
    ) : UndoAction
}

internal fun NutritionDiary.toUiState(today: LocalDate): DiaryUiState {
    val date = day.date
    val water = day.water.toImmutableList()
    return if (day.meals.isEmpty() && water.isEmpty()) {
        DiaryUiState.Empty(date = date, isToday = date == today, goals = day.goals)
    } else {
        DiaryUiState.Success(
            date = date,
            isToday = date == today,
            consumed = day.totals.nutrients,
            goals = day.goals,
            sections =
                MealType.entries
                    .map { type -> MealSection(type, day.meals.filter { it.mealType == type }.toImmutableList()) }
                    .toImmutableList(),
            water = water,
            week = week.days.map { DayKcal(it.date, it.nutrients.kcal, it.hasMeals) }.toImmutableList(),
        )
    }
}

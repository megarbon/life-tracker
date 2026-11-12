package com.example.lifet.feature.fitness

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.example.lifet.core.model.fitness.BodyWeight
import com.example.lifet.core.model.fitness.DailySteps
import com.example.lifet.core.model.fitness.FitnessOverview
import com.example.lifet.core.model.fitness.Workout
import com.example.lifet.core.model.fitness.WorkoutTemplate
import java.time.LocalDate
import java.time.ZoneId

/** Estado de la pantalla de Deporte. [Empty] conserva el resumen para poder registrar peso o pasos igualmente. */
internal sealed interface FitnessUiState {
    data object Loading : FitnessUiState

    @Immutable
    data class Empty(
        val overview: FitnessOverview,
    ) : FitnessUiState

    @Immutable
    data class Success(
        val overview: FitnessOverview,
    ) : FitnessUiState

    data class Error(
        @param:StringRes val messageRes: Int,
    ) : FitnessUiState
}

/** De dónde sale el borrador de un entreno nuevo: repetir uno anterior o aplicar una plantilla. */
internal sealed interface WorkoutSeed {
    data class FromWorkout(
        val workout: Workout,
    ) : WorkoutSeed

    data class FromTemplate(
        val template: WorkoutTemplate,
    ) : WorkoutSeed
}

/** Hoja abierta. Vive en el ViewModel para sobrevivir a una rotación. */
internal sealed interface FitnessEditor {
    /**
     * [newId], [today], [zone] y [nowMinute] los estampa el ViewModel al abrir la hoja (la UI no conoce el reloj):
     * sirven para colocar un entreno nuevo "acabando ahora" y mantienen el id estable si la hoja se recrea.
     */
    data class EditWorkout(
        val existing: Workout?,
        val seed: WorkoutSeed?,
        val newId: String,
        val today: LocalDate,
        val zone: ZoneId,
        val nowMinute: Int,
    ) : FitnessEditor

    data class EditWeight(
        val existing: BodyWeight? = null,
    ) : FitnessEditor

    data class EditSteps(
        val existing: DailySteps? = null,
    ) : FitnessEditor

    /** Detalle de un ejercicio por su clave normalizada (`ExerciseAnalytics.key`). */
    data class ExerciseDetail(
        val key: String,
    ) : FitnessEditor
}

/** Un elemento borrable; sirve tanto para borrar como para restaurar con "Deshacer". */
internal sealed interface FitnessItem {
    data class Workout(
        val value: com.example.lifet.core.model.fitness.Workout,
    ) : FitnessItem

    data class Weight(
        val value: BodyWeight,
    ) : FitnessItem

    data class Steps(
        val value: DailySteps,
    ) : FitnessItem

    data class Template(
        val value: WorkoutTemplate,
    ) : FitnessItem
}

/** Formulario enviado desde una hoja. `existing` es lo que se está editando, si hay algo. */
internal sealed interface FitnessForm {
    data class Workout(
        val draft: WorkoutDraft,
        val existing: com.example.lifet.core.model.fitness.Workout?,
    ) : FitnessForm

    data class Weight(
        val draft: WeightDraft,
        val existing: BodyWeight?,
    ) : FitnessForm

    data class Steps(
        val draft: StepsDraft,
        val existing: DailySteps?,
    ) : FitnessForm
}

/** Eventos de un solo uso hacia la UI (snackbars, háptica). */
internal sealed interface FitnessEffect {
    data class Message(
        @param:StringRes val messageRes: Int,
    ) : FitnessEffect

    data class Deleted(
        @param:StringRes val messageRes: Int,
        val item: FitnessItem,
    ) : FitnessEffect

    data object Saved : FitnessEffect
}

internal sealed interface FitnessAction {
    data object Retry : FitnessAction

    /** Abre la hoja de entreno: nuevo, editando [existing] o partiendo de [seed] (repetir o plantilla). */
    data class OpenWorkout(
        val existing: Workout? = null,
        val seed: WorkoutSeed? = null,
    ) : FitnessAction

    /** `null` cierra la hoja abierta. Para abrir la de entreno usa [OpenWorkout]. */
    data class Open(
        val editor: FitnessEditor?,
    ) : FitnessAction

    data class Save(
        val form: FitnessForm,
    ) : FitnessAction

    data class Delete(
        val item: FitnessItem,
    ) : FitnessAction

    data class Undo(
        val item: FitnessItem,
    ) : FitnessAction
}

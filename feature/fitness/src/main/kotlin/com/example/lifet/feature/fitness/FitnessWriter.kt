package com.example.lifet.feature.fitness

import com.example.lifet.core.common.AppError
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.data.fitness.FitnessRepository
import java.time.Clock
import java.util.UUID
import javax.inject.Inject

/**
 * Valida los formularios de la UI y los guarda. Separado del ViewModel para que éste sólo enrute estado y eventos.
 * Un formulario inválido devuelve [AppError.Validation] sin tocar la base de datos.
 */
internal class FitnessWriter
    @Inject
    constructor(
        private val repository: FitnessRepository,
        private val clock: Clock,
    ) {
        suspend fun save(form: FitnessForm): AppResult<Unit> =
            when (form) {
                is FitnessForm.Workout -> saveWorkout(form)
                is FitnessForm.Weight -> saveWeight(form)
                is FitnessForm.Steps -> saveSteps(form)
            }

        suspend fun delete(item: FitnessItem): AppResult<Unit> =
            when (item) {
                is FitnessItem.Workout -> repository.deleteWorkout(item.value.id)
                is FitnessItem.Weight -> repository.deleteWeight(item.value.date)
                is FitnessItem.Steps -> repository.deleteSteps(item.value.date)
                is FitnessItem.Template -> repository.deleteTemplate(item.value.id)
            }

        suspend fun restore(item: FitnessItem): AppResult<Unit> =
            when (item) {
                is FitnessItem.Workout -> repository.saveWorkout(item.value)
                is FitnessItem.Weight -> repository.saveWeight(item.value)
                is FitnessItem.Steps -> repository.saveSteps(item.value)
                is FitnessItem.Template -> repository.saveTemplate(item.value)
            }

        private suspend fun saveWorkout(form: FitnessForm.Workout): AppResult<Unit> {
            val zone = form.existing?.zone ?: clock.zone
            return when (val result = form.draft.validate(zone)) {
                is DraftResult.Invalid -> {
                    invalid(result)
                }

                is DraftResult.Valid -> {
                    // Editar conserva el origen y el id externo del registro original.
                    val workout =
                        form.existing?.let { result.value.copy(source = it.source, externalId = it.externalId) }
                            ?: result.value
                    repository.saveWorkout(workout).andThen {
                        if (form.draft.saveAsTemplate && workout.title.isNotBlank()) {
                            repository.saveTemplate(workout.toTemplate(UUID.randomUUID().toString()))
                        } else {
                            AppResult.Success(Unit)
                        }
                    }
                }
            }
        }

        private suspend fun saveWeight(form: FitnessForm.Weight): AppResult<Unit> =
            when (val result = form.draft.validate()) {
                is DraftResult.Invalid -> {
                    invalid(result)
                }

                is DraftResult.Valid -> {
                    // Cambiar la fecha de un peso existente lo mueve: se borra el de la fecha antigua.
                    val moved = form.existing?.takeIf { it.date != result.value.date }
                    moved?.let { repository.deleteWeight(it.date) }
                    repository.saveWeight(result.value)
                }
            }

        private suspend fun saveSteps(form: FitnessForm.Steps): AppResult<Unit> =
            when (val result = form.draft.validate()) {
                is DraftResult.Invalid -> {
                    invalid(result)
                }

                is DraftResult.Valid -> {
                    val moved = form.existing?.takeIf { it.date != result.value.date }
                    moved?.let { repository.deleteSteps(it.date) }
                    repository.saveSteps(result.value)
                }
            }

        private fun invalid(result: DraftResult.Invalid): AppResult<Unit> =
            AppResult.Failure(AppError.Validation(result.issues.joinToString { it.name }))

        private suspend fun AppResult<Unit>.andThen(next: suspend () -> AppResult<Unit>): AppResult<Unit> =
            when (this) {
                is AppResult.Success -> next()
                is AppResult.Failure -> this
            }
    }

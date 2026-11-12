package com.example.lifet.core.testing.fitness

import com.example.lifet.core.common.AppError
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.data.fitness.FitnessRepository
import com.example.lifet.core.model.fitness.BodyWeight
import com.example.lifet.core.model.fitness.DailySteps
import com.example.lifet.core.model.fitness.FitnessDataSet
import com.example.lifet.core.model.fitness.Workout
import com.example.lifet.core.model.fitness.WorkoutTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import java.time.LocalDate

/**
 * Repositorio de Deporte en memoria con las mismas reglas que el de Room (un peso y unos pasos por día; borrar un
 * entreno borra sus series). [failObserve] hace que la lectura falle, para probar el estado de error, y
 * [failWrites] que las escrituras devuelvan un error de almacenamiento.
 */
@Suppress("TooManyFunctions") // Implementa FitnessRepository.
public class InMemoryFitnessRepository(
    initial: FitnessDataSet = FitnessDataSet(),
) : FitnessRepository {
    private val state = MutableStateFlow(initial)

    public var failObserve: Boolean = false
    public var failWrites: Boolean = false

    public val current: FitnessDataSet get() = state.value

    // Se evalúa al recolectar, así un test puede activar el fallo después de crear el ViewModel.
    override fun observeData(): Flow<FitnessDataSet> =
        flow {
            check(!failObserve) { "Fallo simulado" }
            emitAll(state)
        }

    override suspend fun saveWorkout(workout: Workout): AppResult<Unit> =
        edit { data -> data.copy(workouts = data.workouts.replacing(workout) { it.id == workout.id }) }

    override suspend fun deleteWorkout(id: String): AppResult<Unit> =
        edit { data -> data.copy(workouts = data.workouts.filterNot { it.id == id }) }

    override suspend fun saveWeight(weight: BodyWeight): AppResult<Unit> =
        edit { data -> data.copy(weights = data.weights.replacing(weight) { it.date == weight.date }) }

    override suspend fun deleteWeight(date: LocalDate): AppResult<Unit> =
        edit { data -> data.copy(weights = data.weights.filterNot { it.date == date }) }

    override suspend fun saveSteps(steps: DailySteps): AppResult<Unit> =
        edit { data -> data.copy(steps = data.steps.replacing(steps) { it.date == steps.date }) }

    override suspend fun deleteSteps(date: LocalDate): AppResult<Unit> =
        edit { data -> data.copy(steps = data.steps.filterNot { it.date == date }) }

    override suspend fun saveTemplate(template: WorkoutTemplate): AppResult<Unit> =
        edit { data -> data.copy(templates = data.templates.replacing(template) { it.id == template.id }) }

    override suspend fun deleteTemplate(id: String): AppResult<Unit> =
        edit { data -> data.copy(templates = data.templates.filterNot { it.id == id }) }

    override suspend fun replaceAll(data: FitnessDataSet): AppResult<Unit> = edit { data }

    private fun edit(change: (FitnessDataSet) -> FitnessDataSet): AppResult<Unit> =
        if (failWrites) {
            AppResult.Failure(AppError.Storage)
        } else {
            state.update(change)
            AppResult.Success(Unit)
        }

    private inline fun <T> List<T>.replacing(
        item: T,
        matches: (T) -> Boolean,
    ): List<T> = if (any(matches)) map { if (matches(it)) item else it } else this + item
}

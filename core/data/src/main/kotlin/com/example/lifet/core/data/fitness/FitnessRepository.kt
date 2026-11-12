package com.example.lifet.core.data.fitness

import com.example.lifet.core.common.AppResult
import com.example.lifet.core.model.fitness.BodyWeight
import com.example.lifet.core.model.fitness.DailySteps
import com.example.lifet.core.model.fitness.FitnessDataSet
import com.example.lifet.core.model.fitness.Workout
import com.example.lifet.core.model.fitness.WorkoutTemplate
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Fuente única de verdad de Deporte (Room). Las lecturas son reactivas; las escrituras devuelven [AppResult] y
 * nunca lanzan, salvo cancelación.
 */
@Suppress("TooManyFunctions") // Un repositorio por área: cuatro entidades con alta, edición y borrado.
public interface FitnessRepository {
    /** Todo el contenido de Deporte; vuelve a emitir con cada cambio. */
    public fun observeData(): Flow<FitnessDataSet>

    /** Inserta o actualiza el entreno con todas sus series. */
    public suspend fun saveWorkout(workout: Workout): AppResult<Unit>

    public suspend fun deleteWorkout(id: String): AppResult<Unit>

    /** Un peso por día: guardar otro el mismo día lo sustituye. */
    public suspend fun saveWeight(weight: BodyWeight): AppResult<Unit>

    public suspend fun deleteWeight(date: LocalDate): AppResult<Unit>

    /** Un registro de pasos por día: guardar otro el mismo día lo sustituye. */
    public suspend fun saveSteps(steps: DailySteps): AppResult<Unit>

    public suspend fun deleteSteps(date: LocalDate): AppResult<Unit>

    public suspend fun saveTemplate(template: WorkoutTemplate): AppResult<Unit>

    public suspend fun deleteTemplate(id: String): AppResult<Unit>

    /** Sustituye todos los datos de Deporte (demo, restaurar copia). */
    public suspend fun replaceAll(data: FitnessDataSet): AppResult<Unit>
}

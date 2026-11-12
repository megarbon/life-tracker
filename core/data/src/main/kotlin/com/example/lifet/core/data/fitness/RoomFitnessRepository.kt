package com.example.lifet.core.data.fitness

import android.database.SQLException
import com.example.lifet.core.common.AppError
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.common.IoDispatcher
import com.example.lifet.core.common.runCatchingApp
import com.example.lifet.core.database.fitness.FitnessDao
import com.example.lifet.core.database.fitness.FitnessSetEntity
import com.example.lifet.core.database.fitness.FitnessTemplateEntity
import com.example.lifet.core.database.fitness.FitnessTemplateExerciseEntity
import com.example.lifet.core.database.fitness.FitnessWorkoutEntity
import com.example.lifet.core.model.fitness.BodyWeight
import com.example.lifet.core.model.fitness.DailySteps
import com.example.lifet.core.model.fitness.FitnessDataSet
import com.example.lifet.core.model.fitness.Workout
import com.example.lifet.core.model.fitness.WorkoutTemplate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

@Suppress("TooManyFunctions") // Implementa FitnessRepository.
internal class RoomFitnessRepository
    @Inject
    constructor(
        private val dao: FitnessDao,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : FitnessRepository {
        override fun observeData(): Flow<FitnessDataSet> =
            combine(
                combine(dao.observeWorkouts(), dao.observeSets(), ::Pair),
                dao.observeWeights(),
                dao.observeSteps(),
                combine(dao.observeTemplates(), dao.observeTemplateExercises(), ::Pair),
            ) { (workouts, sets), weights, steps, (templates, templateExercises) ->
                FitnessDataSet(
                    workouts = workouts.withSets(sets),
                    weights = weights.map { it.toDomain() },
                    steps = steps.map { it.toDomain() },
                    templates = templates.withExercises(templateExercises),
                )
            }.flowOn(ioDispatcher)

        override suspend fun saveWorkout(workout: Workout): AppResult<Unit> =
            write { dao.saveWorkout(workout.toEntity(), workout.toSetEntities()) }

        override suspend fun deleteWorkout(id: String): AppResult<Unit> = write { dao.deleteWorkout(id) }

        override suspend fun saveWeight(weight: BodyWeight): AppResult<Unit> =
            write { dao.upsertWeight(weight.toEntity()) }

        override suspend fun deleteWeight(date: LocalDate): AppResult<Unit> = write { dao.deleteWeight(date) }

        override suspend fun saveSteps(steps: DailySteps): AppResult<Unit> = write { dao.upsertSteps(steps.toEntity()) }

        override suspend fun deleteSteps(date: LocalDate): AppResult<Unit> = write { dao.deleteSteps(date) }

        override suspend fun saveTemplate(template: WorkoutTemplate): AppResult<Unit> =
            write { dao.saveTemplate(template.toEntity(), template.toExerciseEntities()) }

        override suspend fun deleteTemplate(id: String): AppResult<Unit> = write { dao.deleteTemplate(id) }

        override suspend fun replaceAll(data: FitnessDataSet): AppResult<Unit> =
            write {
                dao.replaceAll(
                    workouts = data.workouts.map { it.toEntity() },
                    sets = data.workouts.flatMap { it.toSetEntities() },
                    weights = data.weights.map { it.toEntity() },
                    steps = data.steps.map { it.toEntity() },
                    templates = data.templates.map { it.toEntity() },
                    templateExercises = data.templates.flatMap { it.toExerciseEntities() },
                )
            }

        private fun List<FitnessWorkoutEntity>.withSets(sets: List<FitnessSetEntity>): List<Workout> {
            val byWorkout = sets.groupBy { it.workoutId }
            return map { it.toDomain(byWorkout[it.id].orEmpty().toExerciseLogs()) }
        }

        private fun List<FitnessTemplateEntity>.withExercises(
            exercises: List<FitnessTemplateExerciseEntity>,
        ): List<WorkoutTemplate> {
            val byTemplate = exercises.groupBy { it.templateId }
            return map { it.toDomain(byTemplate[it.id].orEmpty()) }
        }

        private suspend fun write(block: suspend () -> Unit): AppResult<Unit> =
            withContext(ioDispatcher) {
                runCatchingApp(onError = { if (it is SQLException) AppError.Storage else AppError.Unknown }) { block() }
            }
    }

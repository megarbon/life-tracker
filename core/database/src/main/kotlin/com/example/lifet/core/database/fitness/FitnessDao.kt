package com.example.lifet.core.database.fitness

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Acceso a las tablas de Deporte. Es `abstract class` porque guardar un entreno o una plantilla escribe en dos
 * tablas y va en `@Transaction`. Las series y los ejercicios de plantilla dependen de su padre con borrado en
 * cascada: borrar el entreno o la plantilla los elimina.
 */
@Dao
@Suppress("TooManyFunctions") // Un DAO por área: seis tablas que se escriben juntas.
public abstract class FitnessDao {
    @Query("SELECT * FROM fitness_workouts ORDER BY start DESC")
    public abstract fun observeWorkouts(): Flow<List<FitnessWorkoutEntity>>

    @Query("SELECT * FROM fitness_sets ORDER BY workoutId, exerciseIndex, setIndex")
    public abstract fun observeSets(): Flow<List<FitnessSetEntity>>

    @Query("SELECT * FROM fitness_weights ORDER BY date")
    public abstract fun observeWeights(): Flow<List<FitnessWeightEntity>>

    @Query("SELECT * FROM fitness_steps ORDER BY date")
    public abstract fun observeSteps(): Flow<List<FitnessStepsEntity>>

    @Query("SELECT * FROM fitness_templates ORDER BY name")
    public abstract fun observeTemplates(): Flow<List<FitnessTemplateEntity>>

    @Query("SELECT * FROM fitness_template_exercises ORDER BY templateId, position")
    public abstract fun observeTemplateExercises(): Flow<List<FitnessTemplateExerciseEntity>>

    @Upsert
    public abstract suspend fun upsertWorkout(entity: FitnessWorkoutEntity)

    @Insert
    public abstract suspend fun insertSets(entities: List<FitnessSetEntity>)

    @Query("DELETE FROM fitness_sets WHERE workoutId = :workoutId")
    public abstract suspend fun deleteSetsOf(workoutId: String)

    /** Guarda el entreno y sustituye todas sus series, de modo que editar nunca deja series sueltas. */
    @Transaction
    public open suspend fun saveWorkout(
        workout: FitnessWorkoutEntity,
        sets: List<FitnessSetEntity>,
    ) {
        upsertWorkout(workout)
        deleteSetsOf(workout.id)
        insertSets(sets)
    }

    @Query("DELETE FROM fitness_workouts WHERE id = :id")
    public abstract suspend fun deleteWorkout(id: String)

    @Upsert
    public abstract suspend fun upsertWeight(entity: FitnessWeightEntity)

    @Query("DELETE FROM fitness_weights WHERE date = :date")
    public abstract suspend fun deleteWeight(date: LocalDate)

    @Upsert
    public abstract suspend fun upsertSteps(entity: FitnessStepsEntity)

    @Query("DELETE FROM fitness_steps WHERE date = :date")
    public abstract suspend fun deleteSteps(date: LocalDate)

    @Upsert
    public abstract suspend fun upsertTemplate(entity: FitnessTemplateEntity)

    @Insert
    public abstract suspend fun insertTemplateExercises(entities: List<FitnessTemplateExerciseEntity>)

    @Query("DELETE FROM fitness_template_exercises WHERE templateId = :templateId")
    public abstract suspend fun deleteTemplateExercisesOf(templateId: String)

    @Transaction
    public open suspend fun saveTemplate(
        template: FitnessTemplateEntity,
        exercises: List<FitnessTemplateExerciseEntity>,
    ) {
        upsertTemplate(template)
        deleteTemplateExercisesOf(template.id)
        insertTemplateExercises(exercises)
    }

    @Query("DELETE FROM fitness_templates WHERE id = :id")
    public abstract suspend fun deleteTemplate(id: String)

    @Query("DELETE FROM fitness_workouts")
    public abstract suspend fun deleteAllWorkouts()

    @Query("DELETE FROM fitness_weights")
    public abstract suspend fun deleteAllWeights()

    @Query("DELETE FROM fitness_steps")
    public abstract suspend fun deleteAllSteps()

    @Query("DELETE FROM fitness_templates")
    public abstract suspend fun deleteAllTemplates()

    @Insert
    public abstract suspend fun insertWorkouts(entities: List<FitnessWorkoutEntity>)

    @Insert
    public abstract suspend fun insertWeights(entities: List<FitnessWeightEntity>)

    @Insert
    public abstract suspend fun insertSteps(entities: List<FitnessStepsEntity>)

    @Insert
    public abstract suspend fun insertTemplates(entities: List<FitnessTemplateEntity>)

    /** Sustituye todo el contenido de Deporte (datos de demo, restaurar copia). Las series caen en cascada. */
    @Transaction
    public open suspend fun replaceAll(
        workouts: List<FitnessWorkoutEntity>,
        sets: List<FitnessSetEntity>,
        weights: List<FitnessWeightEntity>,
        steps: List<FitnessStepsEntity>,
        templates: List<FitnessTemplateEntity>,
        templateExercises: List<FitnessTemplateExerciseEntity>,
    ) {
        deleteAllWorkouts()
        deleteAllWeights()
        deleteAllSteps()
        deleteAllTemplates()
        insertWorkouts(workouts)
        insertSets(sets)
        insertWeights(weights)
        insertSteps(steps)
        insertTemplates(templates)
        insertTemplateExercises(templateExercises)
    }
}

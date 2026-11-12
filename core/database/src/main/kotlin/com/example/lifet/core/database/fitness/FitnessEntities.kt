package com.example.lifet.core.database.fitness

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.lifet.core.model.fitness.RecordSource
import com.example.lifet.core.model.fitness.WorkoutType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// Todo en SI y en enteros: segundos, metros y gramos. Nunca Double.

@Entity(tableName = "fitness_workouts", indices = [Index("start"), Index("externalId")])
public data class FitnessWorkoutEntity(
    @PrimaryKey public val id: String,
    public val type: WorkoutType,
    public val title: String,
    public val start: Instant,
    public val zoneId: ZoneId,
    public val durationSec: Long,
    public val distanceM: Long?,
    public val energyKcal: Int?,
    public val note: String,
    public val source: RecordSource,
    public val externalId: String?,
)

/** Una serie de fuerza. [exerciseIndex] y [setIndex] conservan el orden en que se registraron. */
@Entity(
    tableName = "fitness_sets",
    foreignKeys =
        [
            ForeignKey(
                entity = FitnessWorkoutEntity::class,
                parentColumns = ["id"],
                childColumns = ["workoutId"],
                onDelete = ForeignKey.CASCADE,
            ),
        ],
    indices = [Index("workoutId")],
)
public data class FitnessSetEntity(
    @PrimaryKey(autoGenerate = true) public val id: Long = 0,
    public val workoutId: String,
    public val exerciseIndex: Int,
    public val exerciseName: String,
    public val setIndex: Int,
    public val reps: Int,
    public val weightGrams: Long,
)

/** Un peso por día: registrar otro el mismo día lo sustituye. */
@Entity(tableName = "fitness_weights")
public data class FitnessWeightEntity(
    @PrimaryKey public val date: LocalDate,
    public val weightGrams: Long,
    public val note: String,
)

@Entity(tableName = "fitness_steps")
public data class FitnessStepsEntity(
    @PrimaryKey public val date: LocalDate,
    public val steps: Int,
    public val source: RecordSource,
)

@Entity(tableName = "fitness_templates")
public data class FitnessTemplateEntity(
    @PrimaryKey public val id: String,
    public val name: String,
    public val type: WorkoutType,
    public val durationSec: Long,
)

@Entity(
    tableName = "fitness_template_exercises",
    foreignKeys =
        [
            ForeignKey(
                entity = FitnessTemplateEntity::class,
                parentColumns = ["id"],
                childColumns = ["templateId"],
                onDelete = ForeignKey.CASCADE,
            ),
        ],
    indices = [Index("templateId")],
)
public data class FitnessTemplateExerciseEntity(
    @PrimaryKey(autoGenerate = true) public val id: Long = 0,
    public val templateId: String,
    public val position: Int,
    public val name: String,
    public val sets: Int,
    public val reps: Int,
    public val weightGrams: Long,
)

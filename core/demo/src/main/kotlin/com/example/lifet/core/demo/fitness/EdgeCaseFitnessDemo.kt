@file:Suppress("MagicNumber") // Fechas y horas literales: cada caso límite se describe mejor con su valor exacto.

package com.example.lifet.core.demo.fitness

import com.example.lifet.core.model.fitness.BodyWeight
import com.example.lifet.core.model.fitness.DailySteps
import com.example.lifet.core.model.fitness.ExerciseLog
import com.example.lifet.core.model.fitness.FitnessDataSet
import com.example.lifet.core.model.fitness.FitnessRules
import com.example.lifet.core.model.fitness.StrengthSet
import com.example.lifet.core.model.fitness.TemplateExercise
import com.example.lifet.core.model.fitness.Workout
import com.example.lifet.core.model.fitness.WorkoutTemplate
import com.example.lifet.core.model.fitness.WorkoutType
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

private const val LONG_EXERCISE_SEED = "Press de banca inclinado con agarre cerrado y pausa de dos segundos 🏋️ "

/**
 * Escenario EDGE_CASES: entrenos que cruzan la medianoche y el cambio de hora, otra zona horaria, textos de longitud
 * máxima con emojis, cargas y repeticiones en el límite, un día con ocho entrenos, uno de 24 horas, ejercicios
 * escritos de dos maneras, pesos y pasos extremos y un hueco de doce días sin datos (de hace 14 a hace 25).
 */
internal class EdgeCaseFitnessDemo(
    private val clock: Clock,
) {
    private val zone: ZoneId = clock.zone
    private val today: LocalDate = LocalDate.now(clock)

    fun build(): FitnessDataSet =
        FitnessDataSet(
            workouts =
                midnight() + daylightSaving() + longTexts() + hugeLoads() + busyDay() + ultra() + otherZone() +
                    oddities() + aliases(),
            weights = weights(),
            steps = steps(),
            templates = templates(),
        )

    private fun day(back: Long): LocalDate = today.minusDays(back)

    private fun midnight(): List<Workout> =
        listOf(
            demoWorkout(
                id = "edge-midnight-run",
                type = WorkoutType.RUN,
                title = "Carrera nocturna",
                date = day(2),
                time = LocalTime.of(23, 30),
                zone = zone,
                durationSec = 5_400,
                distanceM = 14_000,
                energyKcal = 900,
                note = "Empieza antes de medianoche y acaba pasada la una.",
            ),
            demoWorkout(
                id = "edge-exact-midnight",
                type = WorkoutType.WALK,
                title = "Paseo a las 00:00",
                date = day(4),
                time = LocalTime.MIDNIGHT,
                zone = zone,
                durationSec = 3_600,
                distanceM = 4_500,
            ),
            demoWorkout(
                id = "edge-last-second",
                type = WorkoutType.CYCLING,
                title = "Bici a las 23:59:59",
                date = day(5),
                time = LocalTime.of(23, 59, 59),
                zone = zone,
                durationSec = 1_800,
                distanceM = 9_000,
            ),
        )

    /** Un entreno de tres horas reales que empieza una hora antes del último cambio de hora de la zona. */
    private fun daylightSaving(): List<Workout> {
        val transition = zone.rules.previousTransition(clock.instant()) ?: return emptyList()
        val workout =
            Workout(
                id = "edge-dst",
                type = WorkoutType.OTHER,
                title = "Entreno en el cambio de hora ⏰",
                start = transition.instant.minusSeconds(3_600),
                zone = zone,
                durationSec = 10_800,
                note = "Ese día tiene 23 o 25 horas; el reparto entre días usa tiempo real transcurrido.",
            )
        return listOf(workout)
    }

    /** Título y nota en su longitud máxima, 30 ejercicios y uno con 50 series. */
    private fun longTexts(): List<Workout> {
        val exercises =
            (1..FitnessRules.MAX_EXERCISES).map { index ->
                when (index) {
                    1 -> ExerciseLog(fitText(LONG_EXERCISE_SEED, FitnessRules.EXERCISE_NAME_MAX_LENGTH), sets(8))
                    2 -> ExerciseLog("Serie interminable", repeatedSets(FitnessRules.MAX_SETS_PER_EXERCISE, 15, 40_000))
                    else -> ExerciseLog("Ejercicio $index 💪", sets(10))
                }
            }
        return listOf(
            demoWorkout(
                id = "edge-long-texts",
                type = WorkoutType.STRENGTH,
                title = fitText("Pierna y glúteo 🦵🍑 sesión larguísima · ", FitnessRules.TITLE_MAX_LENGTH),
                date = day(8),
                time = LocalTime.of(17, 0),
                zone = zone,
                durationSec = 4 * 3_600L,
                exercises = exercises,
                note = fitText("Nota larguísima con emojis 🔥🏃‍♂️ y\nsaltos de línea. ", FitnessRules.NOTE_MAX_LENGTH),
            ),
        )
    }

    private fun sets(reps: Int): List<StrengthSet> = repeatedSets(3, reps, 20_000)

    /** Cargas y repeticiones en el máximo que acepta el formulario, y un ejercicio sólo con peso corporal. */
    private fun hugeLoads(): List<Workout> =
        listOf(
            demoWorkout(
                id = "edge-huge-loads",
                type = WorkoutType.STRENGTH,
                title = "Cargas enormes 🚛",
                date = day(7),
                time = LocalTime.of(18, 0),
                zone = zone,
                durationSec = 3_600,
                exercises =
                    listOf(
                        ExerciseLog(
                            "Levantamiento de camión 🚛",
                            listOf(
                                StrengthSet(1, FitnessRules.MAX_SET_WEIGHT_GRAMS),
                                StrengthSet(FitnessRules.MAX_REPS, FitnessRules.MAX_SET_WEIGHT_GRAMS),
                            ),
                        ),
                        ExerciseLog("Flexiones sin parar", listOf(StrengthSet(FitnessRules.MAX_REPS, 0))),
                    ),
            ),
        )

    /** Ocho entrenos cortos el mismo día. */
    private fun busyDay(): List<Workout> {
        val types = listOf(WorkoutType.RUN, WorkoutType.WALK, WorkoutType.CYCLING, WorkoutType.SWIM)
        return (0 until 8).map { index ->
            demoWorkout(
                id = "edge-busy-$index",
                type = types[index % types.size],
                title = "Sesión corta ${index + 1}",
                date = day(3),
                time = LocalTime.of(6 + index * 2, 0),
                zone = zone,
                durationSec = 600L + 60L * index,
                distanceM = 1_500L + 100L * index,
                energyKcal = 80 + index * 5,
            )
        }
    }

    /** Veinticuatro horas seguidas y, además, el máximo de distancia y de kcal que acepta el formulario. */
    private fun ultra(): List<Workout> =
        listOf(
            demoWorkout(
                id = "edge-ultra-24h",
                type = WorkoutType.RUN,
                title = "Ultra de 24 horas 🏃",
                date = day(6),
                time = LocalTime.of(20, 0),
                zone = zone,
                durationSec = FitnessRules.MAX_DURATION_SECONDS,
                distanceM = 180_000,
                energyKcal = 18_000,
            ),
            demoWorkout(
                id = "edge-max-distance",
                type = WorkoutType.CYCLING,
                title = "Vuelta al mundo",
                date = day(11),
                time = LocalTime.of(6, 0),
                zone = zone,
                durationSec = 20 * 3_600L,
                distanceM = FitnessRules.MAX_DISTANCE_METERS,
                energyKcal = FitnessRules.MAX_ENERGY_KCAL,
            ),
        )

    /** Registrado en Tokio: su día lógico es el de la zona guardada, no el del móvil actual. */
    private fun otherZone(): List<Workout> =
        listOf(
            demoWorkout(
                id = "edge-tokyo",
                type = WorkoutType.RUN,
                title = "Carrera en Tokio 🗼",
                date = day(9),
                time = LocalTime.of(6, 30),
                zone = ZoneId.of("Asia/Tokyo"),
                durationSec = 2_700,
                distanceM = 8_000,
                energyKcal = 520,
            ),
        )

    /** Un minuto de duración, y un cardio sin título, sin distancia y sin kcal. */
    private fun oddities(): List<Workout> =
        listOf(
            demoWorkout(
                id = "edge-one-minute",
                type = WorkoutType.OTHER,
                title = "Un minuto",
                date = day(10),
                time = LocalTime.of(12, 0),
                zone = zone,
                durationSec = 60,
            ),
            demoWorkout(
                id = "edge-empty-cardio",
                type = WorkoutType.SWIM,
                title = "",
                date = day(12),
                time = LocalTime.of(13, 0),
                zone = zone,
                durationSec = 1_800,
            ),
        )

    /** El mismo ejercicio escrito de dos maneras: la progresión debe unirlos. */
    private fun aliases(): List<Workout> =
        listOf(
            demoWorkout(
                id = "edge-alias-1",
                type = WorkoutType.STRENGTH,
                title = "Fuerza",
                date = day(40),
                time = LocalTime.of(19, 0),
                zone = zone,
                durationSec = 3_000,
                exercises = listOf(ExerciseLog("press banca", repeatedSets(4, 5, 80_000))),
            ),
            demoWorkout(
                id = "edge-alias-2",
                type = WorkoutType.STRENGTH,
                title = "Fuerza",
                date = day(26),
                time = LocalTime.of(19, 0),
                zone = zone,
                durationSec = 3_000,
                exercises = listOf(ExerciseLog("Press  Banca ", repeatedSets(4, 5, 82_500))),
            ),
        )

    /** Dos pesos normales, uno extremo con nota larguísima y un hueco de más de un mes. */
    private fun weights(): List<BodyWeight> =
        listOf(
            BodyWeight(day(1), 79_100),
            BodyWeight(day(2), 78_900),
            BodyWeight(day(13), 499_900, fitText("Pesaje extremo 🧪 ", FitnessRules.NOTE_MAX_LENGTH)),
            BodyWeight(day(45), 78_000),
            BodyWeight(day(46), 78_300, "😀"),
        )

    private fun steps(): List<DailySteps> =
        listOf(
            DailySteps(day(2), FitnessRules.MAX_STEPS - 1),
            DailySteps(day(3), 0),
            DailySteps(day(4), 12_345),
        )

    private fun templates(): List<WorkoutTemplate> =
        listOf(
            WorkoutTemplate(
                id = "edge-template-long",
                name = fitText("Plantilla con un nombre larguísimo 📋 ", FitnessRules.TITLE_MAX_LENGTH),
                type = WorkoutType.STRENGTH,
                durationSec = FitnessRules.MAX_DURATION_SECONDS,
                exercises =
                    (1..FitnessRules.MAX_EXERCISES).map {
                        TemplateExercise(
                            "Ejercicio $it ⚡",
                            FitnessRules.MAX_SETS_PER_EXERCISE,
                            FitnessRules.MAX_REPS,
                            FitnessRules.MAX_SET_WEIGHT_GRAMS,
                        )
                    },
            ),
            WorkoutTemplate(id = "edge-template-blank", name = "", type = WorkoutType.OTHER, durationSec = 60),
        )
}

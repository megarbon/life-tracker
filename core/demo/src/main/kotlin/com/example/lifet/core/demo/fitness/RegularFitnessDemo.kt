@file:Suppress("MagicNumber") // Probabilidades, horas y rangos literales: son los parámetros de los datos de ejemplo.

package com.example.lifet.core.demo.fitness

import com.example.lifet.core.model.fitness.BodyWeight
import com.example.lifet.core.model.fitness.DailySteps
import com.example.lifet.core.model.fitness.ExerciseLog
import com.example.lifet.core.model.fitness.FitnessDataSet
import com.example.lifet.core.model.fitness.StrengthSet
import com.example.lifet.core.model.fitness.TemplateExercise
import com.example.lifet.core.model.fitness.Workout
import com.example.lifet.core.model.fitness.WorkoutTemplate
import com.example.lifet.core.model.fitness.WorkoutType
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Random

private const val WEEKS_BACK = 25L
private const val VACATION_WEEKS_AGO = 9L
private const val DELOAD_EVERY = 8
private const val CARDIO_CHANCE = 60
private const val WEIGHT_CHANCE = 75
private const val WEIGHT_CHANCE_ON_VACATION = 25
private const val STEPS_CHANCE = 45
private const val START_WEIGHT_G = 82_400L
private const val TOTAL_LOSS_G = 2_500L

/**
 * Escenario REGULAR: unos 6 meses (26 semanas) con tres sesiones de fuerza (empuje, tirón y pierna) y un cardio de fin
 * de semana la mayoría de las semanas, cargas que suben con una semana de descarga cada ocho, una semana de
 * vacaciones sin actividad (con la fuerza algo resentida al volver) y un peso con tendencia suave a la baja.
 * Todo el azar sale de [random]; las fechas, de [clock].
 */
internal class RegularFitnessDemo(
    clock: Clock,
    private val random: Random,
) {
    private val zone: ZoneId = clock.zone
    private val today: LocalDate = LocalDate.now(clock)
    private val currentMonday: LocalDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    private val firstMonday: LocalDate = currentMonday.minusWeeks(WEEKS_BACK)
    private val vacationMonday: LocalDate = currentMonday.minusWeeks(VACATION_WEEKS_AGO)
    private var counter = 0

    fun build(): FitnessDataSet {
        val workouts = mutableListOf<Workout>()
        var trainingWeeks = 0
        for (week in 0L..WEEKS_BACK) {
            val monday = firstMonday.plusWeeks(week)
            if (monday != vacationMonday) {
                workouts += weekOf(monday, week.toInt(), trainingWeeks)
                trainingWeeks++
            }
        }
        return FitnessDataSet(workouts, weights(), steps(), templates(trainingWeeks - 1))
    }

    private fun weekOf(
        monday: LocalDate,
        weekIndex: Int,
        trainingWeek: Int,
    ): List<Workout> {
        val percent = loadPercent(monday, trainingWeek)
        // Lunes/martes, miércoles/jueves y viernes/sábado: nunca coincide con el cardio del domingo.
        val strength =
            DemoSession.entries.mapIndexed { index, session ->
                strength(session, monday.plusDays((index * 2 + random.nextInt(2)).toLong()), trainingWeek, percent)
            }
        val cardio =
            if (random.nextInt(100) < CARDIO_CHANCE) listOf(cardio(monday.plusDays(6), weekIndex)) else emptyList()
        return strength + cardio
    }

    /** 100 en una semana normal; 95 al volver de vacaciones y 90 en la semana de descarga. */
    private fun loadPercent(
        monday: LocalDate,
        trainingWeek: Int,
    ): Long =
        when {
            monday == vacationMonday.plusWeeks(1) -> 95L
            trainingWeek % DELOAD_EVERY == DELOAD_EVERY - 1 -> 90L
            else -> 100L
        }

    private fun strength(
        session: DemoSession,
        date: LocalDate,
        trainingWeek: Int,
        percent: Long,
    ): Workout {
        val start = LocalTime.of(17, 30).plusMinutes(5L * random.nextInt(25))
        val exercises = session.exercises.map { ExerciseLog(it.name, setsOf(it, trainingWeek, percent)) }
        return demoWorkout(
            id = nextId(date),
            type = WorkoutType.STRENGTH,
            title = session.title,
            date = date,
            time = start,
            zone = zone,
            durationSec = (36 + random.nextInt(21)) * 60L,
            energyKcal = 260 + random.nextInt(140),
            exercises = exercises,
        )
    }

    /** Con peso corporal sube una repetición cada cinco semanas; con carga, alguna serie pierde una repetición. */
    private fun setsOf(
        spec: ExerciseSpec,
        trainingWeek: Int,
        percent: Long,
    ): List<StrengthSet> {
        val grams = spec.loadAt(trainingWeek, percent)
        val progression = if (spec.isBodyweight) trainingWeek / 5 else 0
        return List(spec.sets) { index ->
            val fatigue = if (index > 0 && random.nextInt(3) == 0) 1 else 0
            StrengthSet(reps = (spec.reps + progression - fatigue).coerceAtLeast(1), weightGrams = grams)
        }
    }

    private fun cardio(
        date: LocalDate,
        weekIndex: Int,
    ): Workout {
        val start = LocalTime.of(9, 0).plusMinutes(5L * random.nextInt(25))
        val id = nextId(date)
        return when (weekIndex % 4) {
            2 -> ride(id, date, start)
            3 -> hike(id, date, start)
            else -> run(id, date, start, weekIndex)
        }
    }

    /** La distancia crece y el ritmo mejora poco a poco. */
    private fun run(
        id: String,
        date: LocalDate,
        start: LocalTime,
        weekIndex: Int,
    ): Workout {
        val distance = 6_000L + 100L * weekIndex + 50L * random.nextInt(17)
        val paceSecPerKm = 345L - weekIndex + random.nextInt(25)
        return demoWorkout(
            id = id,
            type = WorkoutType.RUN,
            title = "Rodaje largo",
            date = date,
            time = start,
            zone = zone,
            durationSec = distance * paceSecPerKm / 1_000,
            distanceM = distance,
            energyKcal = (distance * 65 / 1_000).toInt() + random.nextInt(40),
        )
    }

    private fun ride(
        id: String,
        date: LocalDate,
        start: LocalTime,
    ): Workout {
        val distance = 24_000L + 500L * random.nextInt(17)
        val durationSec = distance / (6L + random.nextInt(2))
        return demoWorkout(
            id = id,
            type = WorkoutType.CYCLING,
            title = "Ruta en bici",
            date = date,
            time = start,
            zone = zone,
            durationSec = durationSec,
            distanceM = distance,
            energyKcal = (durationSec / 60 * 9).toInt(),
        )
    }

    private fun hike(
        id: String,
        date: LocalDate,
        start: LocalTime,
    ): Workout {
        val distance = 9_000L + 500L * random.nextInt(9)
        return demoWorkout(
            id = id,
            type = WorkoutType.HIKE,
            title = "Senderismo",
            date = date,
            time = start,
            zone = zone,
            durationSec = distance,
            distanceM = distance,
            energyKcal = (distance / 60 * 6).toInt(),
        )
    }

    /** Un registro la mayoría de los días: bajada suave de unos 2,5 kg, algo más alto en fin de semana, con ruido. */
    private fun weights(): List<BodyWeight> {
        val span = ChronoUnit.DAYS.between(firstMonday, today).coerceAtLeast(1L)
        return days().mapNotNull { date -> weightOn(date, span) }
    }

    private fun weightOn(
        date: LocalDate,
        span: Long,
    ): BodyWeight? {
        val chance = if (onVacation(date)) WEIGHT_CHANCE_ON_VACATION else WEIGHT_CHANCE
        if (random.nextInt(100) >= chance) return null
        val elapsed = ChronoUnit.DAYS.between(firstMonday, date)
        val trend = START_WEIGHT_G - TOTAL_LOSS_G * elapsed / span
        val weekend = if (date.dayOfWeek.value >= DayOfWeek.SATURDAY.value) 300L else 0L
        val noise = random.nextInt(501) - 250L
        return BodyWeight(date, (trend + weekend + noise) / 100 * 100)
    }

    /** Pasos anotados a mano algunos días, más altos el domingo; ninguno en vacaciones. */
    private fun steps(): List<DailySteps> = days().mapNotNull { date -> stepsOn(date) }

    private fun stepsOn(date: LocalDate): DailySteps? {
        if (onVacation(date) || random.nextInt(100) >= STEPS_CHANCE) return null
        val sunday = if (date.dayOfWeek == DayOfWeek.SUNDAY) 3_000 else 0
        return DailySteps(date, (4_000 + random.nextInt(8_000) + sunday) / 10 * 10)
    }

    private fun templates(lastTrainingWeek: Int): List<WorkoutTemplate> =
        DemoSession.entries.map { session ->
            WorkoutTemplate(
                id = "demo-template-${session.name.lowercase()}",
                name = session.title,
                type = WorkoutType.STRENGTH,
                durationSec = 3_600,
                exercises =
                    session.exercises.map {
                        TemplateExercise(it.name, it.sets, it.reps, it.loadAt(lastTrainingWeek, 100L))
                    },
            )
        } + WorkoutTemplate("demo-template-run", "Rodaje largo", WorkoutType.RUN, 3_000)

    private fun days(): List<LocalDate> =
        generateSequence(firstMonday) { it.plusDays(1) }.takeWhile { it <= today }.toList()

    private fun onVacation(date: LocalDate): Boolean = date >= vacationMonday && date < vacationMonday.plusWeeks(1)

    private fun nextId(date: LocalDate): String = "demo-fit-$date-${counter++}"
}

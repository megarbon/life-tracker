@file:Suppress("MagicNumber") // Horas y medidas literales: son datos de ejemplo, no reglas.

package com.example.lifet.core.demo.fitness

import com.example.lifet.core.demo.DemoScenario
import com.example.lifet.core.model.fitness.BodyWeight
import com.example.lifet.core.model.fitness.DailySteps
import com.example.lifet.core.model.fitness.ExerciseLog
import com.example.lifet.core.model.fitness.FitnessDataSet
import com.example.lifet.core.model.fitness.WorkoutType
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.util.Random

/**
 * Datos de demo de Deporte, deterministas: todo el azar sale de [random] y las fechas de [clock]. REGULAR vive en
 * [RegularFitnessDemo] y EDGE_CASES en [EdgeCaseFitnessDemo]. Nada de lo generado queda en el futuro.
 */
internal class FitnessDemoData(
    private val clock: Clock,
    private val random: Random,
) {
    fun build(scenario: DemoScenario): FitnessDataSet =
        when (scenario) {
            DemoScenario.EMPTY -> FitnessDataSet()
            DemoScenario.NEW_USER -> newUser()
            DemoScenario.REGULAR -> RegularFitnessDemo(clock, random).build()
            DemoScenario.EDGE_CASES -> EdgeCaseFitnessDemo(clock).build()
        }.withoutFuture(clock)

    /** Tres días de uso: un paseo, el primer día de fuerza, una primera carrera hoy, dos pesos y unos pasos. */
    private fun newUser(): FitnessDataSet {
        val zone = clock.zone
        val today = LocalDate.now(clock)
        val firstStrength =
            demoWorkout(
                id = "demo-new-strength",
                type = WorkoutType.STRENGTH,
                title = "Primer día de fuerza",
                date = today.minusDays(1),
                time = LocalTime.of(19, 0),
                zone = zone,
                durationSec = 45 * 60L,
                energyKcal = 210,
                exercises =
                    listOf(
                        ExerciseLog("Sentadilla", repeatedSets(3, 10, 20_000)),
                        ExerciseLog("Press banca", repeatedSets(3, 8, 25_000)),
                        ExerciseLog("Remo con mancuerna", repeatedSets(3, 10, 12_500)),
                    ),
            )
        return FitnessDataSet(
            workouts =
                listOf(
                    demoWorkout(
                        id = "demo-new-walk",
                        type = WorkoutType.WALK,
                        title = "Paseo",
                        date = today.minusDays(2),
                        time = LocalTime.of(18, 30),
                        zone = zone,
                        durationSec = 35 * 60L,
                        distanceM = 2_800,
                        energyKcal = 110,
                    ),
                    firstStrength,
                    demoWorkout(
                        id = "demo-new-run",
                        type = WorkoutType.RUN,
                        title = "Primer rodaje",
                        date = today,
                        time = LocalTime.of(7, 30),
                        zone = zone,
                        durationSec = 25 * 60L,
                        distanceM = 3_500,
                        energyKcal = 240,
                    ),
                ),
            weights = listOf(BodyWeight(today.minusDays(2), 81_600), BodyWeight(today, 81_300)),
            steps = listOf(DailySteps(today.minusDays(1), 5_400), DailySteps(today, 3_200)),
        )
    }
}

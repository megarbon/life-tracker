@file:Suppress("MagicNumber") // Horas y cantidades literales: son datos de ejemplo, no reglas.

package com.example.lifet.core.demo.quit

import com.example.lifet.core.demo.DemoScenario
import com.example.lifet.core.model.quit.BaselinePeriod
import com.example.lifet.core.model.quit.CopingStrategy
import com.example.lifet.core.model.quit.CravingTrigger
import com.example.lifet.core.model.quit.HabitKind
import com.example.lifet.core.model.quit.QuitDataSet
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.util.Random

/**
 * Datos de demo de Dejar de fumar y beber, deterministas: todo el azar sale de [random] y las fechas de [clock].
 * REGULAR vive en [RegularQuitDemo] y EDGE_CASES en [EdgeCaseQuitDemo]. Nada de lo generado queda en el futuro.
 */
internal class QuitDemoData(
    private val clock: Clock,
    private val random: Random,
) {
    fun build(scenario: DemoScenario): QuitDataSet =
        when (scenario) {
            DemoScenario.EMPTY -> QuitDataSet()
            DemoScenario.NEW_USER -> newUser()
            DemoScenario.REGULAR -> RegularQuitDemo(clock, random).build()
            DemoScenario.EDGE_CASES -> EdgeCaseQuitDemo(clock).build()
        }.withoutFuture(clock)

    /** Tres días de uso: el tabaco se dejó hace poco más de tres días y ya hay unos cuantos antojos superados. */
    private fun newUser(): QuitDataSet {
        val zone = clock.zone
        val today = LocalDate.now(clock)
        val habit =
            demoHabit(
                id = "demo-new-tobacco",
                kind = HabitKind.TOBACCO,
                name = "Tabaco",
                startedAt = instantAt(today.minusDays(3), LocalTime.of(8, 0), zone),
                zone = zone,
                baselineUnits = 12,
                baselinePeriod = BaselinePeriod.DAY,
                unitsPerPack = 20,
                priceMinor = 540,
            )
        val cravings =
            listOf(
                Triple(3L, LocalTime.of(10, 10), CravingTrigger.COFFEE),
                Triple(2L, LocalTime.of(14, 5), CravingTrigger.AFTER_MEAL),
                Triple(2L, LocalTime.of(21, 30), CravingTrigger.BOREDOM),
                Triple(1L, LocalTime.of(9, 45), CravingTrigger.COFFEE),
                Triple(1L, LocalTime.of(18, 20), CravingTrigger.STRESS),
                Triple(0L, LocalTime.of(8, 40), CravingTrigger.ROUTINE),
            ).mapIndexed { index, (back, time, trigger) ->
                demoCraving(
                    id = "demo-new-craving-$index",
                    habitId = habit.id,
                    at = instantAt(today.minusDays(back), time, zone),
                    zone = zone,
                    intensity = 4 - index / 3,
                    trigger = trigger,
                    strategy = if (index % 2 == 0) CopingStrategy.BREATHING else CopingStrategy.WALK,
                )
            }
        return QuitDataSet(habits = listOf(habit), cravings = cravings)
    }
}

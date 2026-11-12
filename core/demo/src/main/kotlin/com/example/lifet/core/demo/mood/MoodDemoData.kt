@file:Suppress("MagicNumber") // Horas y escalas literales: son datos de ejemplo, no reglas.

package com.example.lifet.core.demo.mood

import com.example.lifet.core.demo.DemoScenario
import com.example.lifet.core.model.mood.MoodEntry
import com.example.lifet.core.model.mood.MoodLevel
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Random

/** Check-in en la zona [zone] a la hora local [time] de [date]. */
internal fun moodAt(
    date: LocalDate,
    time: LocalTime,
    level: MoodLevel,
    zone: ZoneId,
): MoodEntry = MoodEntry(instant = date.atTime(time).atZone(zone).toInstant(), zone = zone, level = level)

/**
 * Datos de demo de ánimo, deterministas: todo el azar sale de [random] y las fechas de [clock].
 * REGULAR vive en [RegularMoodDemo] y EDGE_CASES en [EdgeCaseMoodDemo].
 */
internal class MoodDemoData(
    private val clock: Clock,
    private val random: Random,
) {
    fun build(scenario: DemoScenario): List<MoodEntry> =
        when (scenario) {
            DemoScenario.EMPTY -> emptyList()
            DemoScenario.NEW_USER -> newUser()
            DemoScenario.REGULAR -> RegularMoodDemo(clock, random).build()
            DemoScenario.EDGE_CASES -> EdgeCaseMoodDemo(clock).build()
        }

    private fun newUser(): List<MoodEntry> {
        val zone = clock.zone
        val today = LocalDate.now(clock)
        // El check-in de hoy nunca queda en el futuro: si aún no son las 8:30, se pone "ahora".
        val morning = moodAt(today, LocalTime.of(8, 30), MoodLevel.NEUTRAL, zone)
        val now = clock.instant()
        val todayEntry = if (morning.instant.isAfter(now)) morning.copy(instant = now) else morning
        return listOf(
            moodAt(today.minusDays(2), LocalTime.of(21, 10), MoodLevel.NEUTRAL, zone),
            moodAt(today.minusDays(1), LocalTime.of(9, 5), MoodLevel.GOOD, zone).copy(
                emotions = listOf("calm", "motivation"),
                energy = 4,
                stress = 2,
                factors = listOf("sleep", "sport"),
                note = "Primer día probando la app. He salido a correr temprano.",
            ),
            moodAt(today.minusDays(1), LocalTime.of(22, 0), MoodLevel.GOOD, zone),
            todayEntry.copy(emotions = listOf("tired"), energy = 2, factors = listOf("sleep", "work")),
        )
    }
}

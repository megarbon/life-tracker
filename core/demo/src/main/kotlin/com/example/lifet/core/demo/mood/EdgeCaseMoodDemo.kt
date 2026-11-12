@file:Suppress("MagicNumber") // Fechas y horas literales: cada caso límite se describe mejor con su valor exacto.

package com.example.lifet.core.demo.mood

import com.example.lifet.core.model.mood.MoodEntry
import com.example.lifet.core.model.mood.MoodLevel
import com.example.lifet.core.model.mood.MoodRules
import com.example.lifet.core.model.mood.MoodTags
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Escenario EDGE_CASES: cruce de medianoche, cambio de hora, otra zona horaria, textos larguísimos, emojis,
 * un día con muchos check-ins y un hueco de 9 días sin datos.
 */
internal class EdgeCaseMoodDemo(
    private val clock: Clock,
) {
    private val zone: ZoneId = clock.zone
    private val today: LocalDate = LocalDate.now(clock)

    fun build(): List<MoodEntry> =
        buildList {
            addAll(midnight())
            addAll(daylightSaving())
            add(longTexts())
            add(emojis())
            add(traveller())
            addAll(busyDay())
            // Hueco sin datos entre hace 25 y hace 15 días.
            add(at(25, LocalTime.NOON, MoodLevel.VERY_LOW).copy(energy = 1, stress = 5))
            add(at(15, LocalTime.NOON, MoodLevel.VERY_GOOD).copy(energy = 5, stress = 1))
        }.filter { !it.instant.isAfter(clock.instant()) }

    private fun at(
        daysAgo: Long,
        time: LocalTime,
        level: MoodLevel,
    ): MoodEntry = moodAt(today.minusDays(daysAgo), time, level, zone)

    private fun midnight(): List<MoodEntry> =
        listOf(
            at(2, LocalTime.of(23, 59, 59), MoodLevel.LOW).copy(note = "Justo antes de medianoche"),
            at(1, LocalTime.of(0, 0, 1), MoodLevel.NEUTRAL).copy(note = "Justo después de medianoche"),
        )

    /** Dos check-ins una hora antes y después del último cambio de hora de la zona (si la zona lo tiene). */
    private fun daylightSaving(): List<MoodEntry> {
        val transition = zone.rules.previousTransition(clock.instant()) ?: return emptyList()
        val instant = transition.instant
        return listOf(
            MoodEntry(instant = instant.minusSeconds(SECONDS_IN_HOUR), zone = zone, level = MoodLevel.NEUTRAL),
            MoodEntry(
                instant = instant.plusSeconds(SECONDS_IN_HOUR),
                zone = zone,
                level = MoodLevel.GOOD,
                note = "Cambio de hora: hoy el día tiene 23 o 25 horas",
            ),
        )
    }

    private fun longTexts(): MoodEntry =
        at(3, LocalTime.of(18, 0), MoodLevel.NEUTRAL).copy(
            note = LONG_TEXT.repeat(40).take(MoodRules.TEXT_MAX_LENGTH),
            gratitude = LONG_TEXT.repeat(20),
            emotions = MoodTags.DEFAULT_EMOTIONS + "x".repeat(MoodTags.MAX_LENGTH),
            factors = MoodTags.DEFAULT_FACTORS,
            energy = 3,
            stress = 3,
        )

    private fun emojis(): MoodEntry =
        at(4, LocalTime.of(10, 0), MoodLevel.VERY_GOOD).copy(
            emotions = listOf("joy", "☀️ solecito", "🥳"),
            factors = listOf("🏃‍♀️ correr", "☕ café", "nature"),
            note = "¡Día redondo! 🎉🎉 Paseo por el Retiro 🌳 y vermú con amigas 🍷",
            gratitude = "Por las personas que me cuidan ❤️",
        )

    /** Check-in en otra zona horaria: su día lógico es el de Nueva York, no el del móvil. */
    private fun traveller(): MoodEntry {
        val newYork = ZoneId.of("America/New_York")
        return moodAt(today.minusDays(6), LocalTime.of(23, 30), MoodLevel.GOOD, newYork)
            .copy(note = "De viaje en Nueva York")
    }

    private fun busyDay(): List<MoodEntry> =
        (0 until 12).map { index ->
            at(5, LocalTime.of(8 + index, 0), MoodLevel.entries[index % MoodLevel.entries.size])
        }

    private companion object {
        const val SECONDS_IN_HOUR = 3_600L
        const val LONG_TEXT = "Texto larguísimo para comprobar que nada se corta ni rompe el diseño. "
    }
}

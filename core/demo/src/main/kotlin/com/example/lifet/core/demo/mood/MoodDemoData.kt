package com.example.lifet.core.demo.mood

import com.example.lifet.core.demo.DemoScenario
import com.example.lifet.core.model.mood.MoodEntry
import com.example.lifet.core.model.mood.MoodLevel
import com.example.lifet.core.model.mood.MoodRules
import com.example.lifet.core.model.mood.MoodTags
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Random
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Datos de demo de ánimo, deterministas: todo el azar sale de [random] y las fechas de [clock].
 *
 * REGULAR (~6 meses) siembra patrones que Insights debe encontrar: lunes más bajos, mejores días cuando hay
 * deporte, una ligera ola estacional y una racha baja de 5 días que termina anteayer (para la tarjeta de apoyo).
 */
internal class MoodDemoData(
    private val clock: Clock,
    private val random: Random,
) {
    private val zone: ZoneId = clock.zone
    private val today: LocalDate = LocalDate.now(clock)

    fun build(scenario: DemoScenario): List<MoodEntry> =
        when (scenario) {
            DemoScenario.EMPTY -> emptyList()
            DemoScenario.NEW_USER -> newUser()
            DemoScenario.REGULAR -> regular()
            DemoScenario.EDGE_CASES -> edgeCases()
        }

    private fun newUser(): List<MoodEntry> =
        listOf(
            at(today.minusDays(2), LocalTime.of(21, 10), MoodLevel.NEUTRAL),
            at(today.minusDays(1), LocalTime.of(9, 5), MoodLevel.GOOD).copy(
                emotions = listOf("calm", "motivation"),
                energy = 4,
                stress = 2,
                factors = listOf("sleep", "sport"),
                note = "Primer día probando la app. He salido a correr temprano.",
            ),
            at(today.minusDays(1), LocalTime.of(22, 0), MoodLevel.GOOD),
            notAfterNow(at(today, LocalTime.of(8, 30), MoodLevel.NEUTRAL)).copy(
                emotions = listOf("tired"),
                energy = 2,
                factors = listOf("sleep", "work"),
            ),
        )

    // ---- REGULAR ----

    private fun regular(): List<MoodEntry> =
        (REGULAR_DAYS downTo 0L).flatMap { daysAgo ->
            val date = today.minusDays(daysAgo)
            val lowStreak = daysAgo in LOW_STREAK_DAYS_AGO
            val skip = !lowStreak && daysAgo > 0 && random.nextDouble() < SKIP_DAY_PROBABILITY
            if (skip) emptyList() else regularDay(date, lowStreak)
        }

    private fun regularDay(
        date: LocalDate,
        lowStreak: Boolean,
    ): List<MoodEntry> {
        val sport = isSportDay(date)
        val base = if (lowStreak) LOW_STREAK_BASE else baseMood(date, sport)
        val slots = CHECK_IN_SLOTS.take(1 + random.nextInt(CHECK_IN_SLOTS.size))
        return slots
            .mapIndexed { index, time ->
                val sampled = levelFrom(base + random.nextGaussian() * INTRA_DAY_NOISE)
                // En la racha baja todos los check-ins son bajos: así la tarjeta de apoyo aparece siempre.
                val level = if (lowStreak) minOf(sampled, MoodLevel.LOW) else sampled
                val detailed = index == 0 || random.nextDouble() < DETAIL_PROBABILITY
                val entry = at(date, time.plusMinutes(random.nextInt(MAX_MINUTE_JITTER).toLong()), level)
                if (detailed) entry.withDetail(date, sport, lowStreak) else entry
            }.filter { !it.instant.isAfter(clock.instant()) }
    }

    private fun isSportDay(date: LocalDate): Boolean =
        when (date.dayOfWeek) {
            DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY -> random.nextDouble() < SPORT_LIKELY
            else -> random.nextDouble() < SPORT_UNLIKELY
        }

    private fun baseMood(
        date: LocalDate,
        sport: Boolean,
    ): Double {
        val weekday = WEEKDAY_OFFSET.getValue(date.dayOfWeek)
        val seasonal = SEASONAL_AMPLITUDE * sin(2 * PI * date.dayOfYear / DAYS_IN_YEAR)
        val sportBoost = if (sport) SPORT_BOOST else 0.0
        return BASE_MOOD + weekday + seasonal + sportBoost + random.nextGaussian() * DAY_NOISE
    }

    private fun MoodEntry.withDetail(
        date: LocalDate,
        sport: Boolean,
        lowStreak: Boolean,
    ): MoodEntry {
        val weekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
        val factors =
            buildList {
                if (sport) add("sport")
                if (!weekend && random.nextDouble() < WORK_FACTOR_PROBABILITY) add("work")
                if (weekend && random.nextDouble() < SOCIAL_FACTOR_PROBABILITY) add("social")
                if (lowStreak || random.nextDouble() < SLEEP_FACTOR_PROBABILITY) add("sleep")
                if (random.nextDouble() < OTHER_FACTOR_PROBABILITY) add(pick(OTHER_FACTORS))
            }.distinct()
        val mondayStress = if (date.dayOfWeek == DayOfWeek.MONDAY) 1 else 0
        return copy(
            emotions = emotionsFor(level),
            energy = scaleAround(level.value + if (sport) 1 else 0),
            stress = scaleAround(MoodRules.SCALE_MAX + 1 - level.value + mondayStress),
            factors = factors,
            note = if (random.nextDouble() < NOTE_PROBABILITY) pick(if (lowStreak) LOW_NOTES else NOTES) else "",
            gratitude = if (random.nextDouble() < GRATITUDE_PROBABILITY) pick(GRATITUDES) else "",
        )
    }

    private fun emotionsFor(level: MoodLevel): List<String> {
        val pool =
            when {
                level.value >= MoodLevel.GOOD.value -> POSITIVE
                level.value <= MoodLevel.LOW.value -> NEGATIVE
                else -> POSITIVE.take(2) + NEGATIVE.take(2)
            }
        return List(1 + random.nextInt(2)) { pick(pool) }.distinct()
    }

    private fun scaleAround(center: Int): Int =
        (center + random.nextInt(3) - 1).coerceIn(MoodRules.SCALE_MIN, MoodRules.SCALE_MAX)

    private fun levelFrom(value: Double): MoodLevel =
        MoodLevel.fromValue(value.roundToInt().coerceIn(MoodLevel.MIN, MoodLevel.MAX)) ?: MoodLevel.NEUTRAL

    // ---- EDGE_CASES ----

    private fun edgeCases(): List<MoodEntry> =
        buildList {
            addAll(midnight())
            addAll(daylightSaving())
            add(longTexts())
            add(emojis())
            add(traveller())
            addAll(busyDay())
            // Hueco de 10 días sin datos: entre hace 25 y hace 15 días no hay nada.
            add(at(today.minusDays(EDGE_GAP_START), LocalTime.NOON, MoodLevel.VERY_LOW).copy(energy = 1, stress = 5))
            add(at(today.minusDays(EDGE_GAP_END), LocalTime.NOON, MoodLevel.VERY_GOOD).copy(energy = 5, stress = 1))
        }

    private fun midnight(): List<MoodEntry> {
        val day = today.minusDays(2)
        return listOf(
            at(day, LocalTime.of(23, 59, 59), MoodLevel.LOW).copy(note = "Justo antes de medianoche"),
            at(day.plusDays(1), LocalTime.of(0, 0, 1), MoodLevel.NEUTRAL).copy(note = "Justo después de medianoche"),
        )
    }

    /** Dos check-ins una hora antes y después del último cambio de hora de la zona (si la zona lo tiene). */
    private fun daylightSaving(): List<MoodEntry> {
        val transition = zone.rules.previousTransition(clock.instant()) ?: return emptyList()
        val instant = transition.instant
        return listOf(
            MoodEntry(instant = instant.minusSeconds(SECONDS_IN_HOUR), zone = zone, level = MoodLevel.NEUTRAL),
            MoodEntry(instant = instant.plusSeconds(SECONDS_IN_HOUR), zone = zone, level = MoodLevel.GOOD)
                .copy(note = "Cambio de hora: hoy el día tiene 23 o 25 horas"),
        )
    }

    private fun longTexts(): MoodEntry =
        at(today.minusDays(3), LocalTime.of(18, 0), MoodLevel.NEUTRAL).copy(
            note = LONG_TEXT.repeat(LONG_TEXT_REPEAT).take(MoodRules.TEXT_MAX_LENGTH),
            gratitude = LONG_TEXT.repeat(LONG_TEXT_REPEAT / 2),
            emotions = MoodTags.DEFAULT_EMOTIONS + "x".repeat(MoodTags.MAX_LENGTH),
            factors = MoodTags.DEFAULT_FACTORS,
            energy = 3,
            stress = 3,
        )

    private fun emojis(): MoodEntry =
        at(today.minusDays(4), LocalTime.of(10, 0), MoodLevel.VERY_GOOD).copy(
            emotions = listOf("joy", "☀️ solecito", "🥳"),
            factors = listOf("🏃‍♀️ correr", "☕ café", "nature"),
            note = "¡Día redondo! 🎉🎉 Paseo por el Retiro 🌳 y vermú con amigas 🍷",
            gratitude = "Por las personas que me cuidan ❤️",
        )

    /** Check-in en otra zona horaria: su día lógico es el de Nueva York, no el del móvil. */
    private fun traveller(): MoodEntry {
        val newYork = ZoneId.of(TRAVEL_ZONE)
        val instant = today.minusDays(6).atTime(LocalTime.of(23, 30)).atZone(newYork).toInstant()
        return MoodEntry(instant = instant, zone = newYork, level = MoodLevel.GOOD, note = "De viaje en Nueva York")
    }

    private fun busyDay(): List<MoodEntry> {
        val day = today.minusDays(5)
        return (0 until BUSY_DAY_CHECK_INS).map { hour ->
            at(day, LocalTime.of(BUSY_DAY_FIRST_HOUR + hour, 0), MoodLevel.entries[hour % MoodLevel.entries.size])
        }
    }

    // ---- utilidades ----

    private fun at(
        date: LocalDate,
        time: LocalTime,
        level: MoodLevel,
    ): MoodEntry = MoodEntry(instant = date.atTime(time).atZone(zone).toInstant(), zone = zone, level = level)

    /** Nunca se generan check-ins en el futuro: si la hora preferida aún no ha llegado, se usa "ahora". */
    private fun notAfterNow(entry: MoodEntry): MoodEntry =
        if (entry.instant.isAfter(clock.instant())) entry.copy(instant = clock.instant()) else entry

    private fun <T> pick(options: List<T>): T = options[random.nextInt(options.size)]

    private companion object {
        const val REGULAR_DAYS = 182L
        val LOW_STREAK_DAYS_AGO = 2L..6L
        const val SKIP_DAY_PROBABILITY = 0.08
        const val BASE_MOOD = 3.4
        const val LOW_STREAK_BASE = 1.8
        const val SPORT_BOOST = 0.6
        const val SEASONAL_AMPLITUDE = 0.25
        const val DAYS_IN_YEAR = 365.0
        const val DAY_NOISE = 0.45
        const val INTRA_DAY_NOISE = 0.4
        const val SPORT_LIKELY = 0.8
        const val SPORT_UNLIKELY = 0.15
        const val DETAIL_PROBABILITY = 0.3
        const val WORK_FACTOR_PROBABILITY = 0.7
        const val SOCIAL_FACTOR_PROBABILITY = 0.6
        const val SLEEP_FACTOR_PROBABILITY = 0.25
        const val OTHER_FACTOR_PROBABILITY = 0.3
        const val NOTE_PROBABILITY = 0.25
        const val GRATITUDE_PROBABILITY = 0.2
        const val MAX_MINUTE_JITTER = 50
        const val SECONDS_IN_HOUR = 3_600L
        const val LONG_TEXT_REPEAT = 40
        const val EDGE_GAP_START = 25L
        const val EDGE_GAP_END = 15L
        const val BUSY_DAY_CHECK_INS = 12
        const val BUSY_DAY_FIRST_HOUR = 8
        const val TRAVEL_ZONE = "America/New_York"

        val CHECK_IN_SLOTS = listOf(LocalTime.of(8, 0), LocalTime.of(14, 0), LocalTime.of(21, 0))

        val WEEKDAY_OFFSET =
            mapOf(
                DayOfWeek.MONDAY to -0.8,
                DayOfWeek.TUESDAY to -0.2,
                DayOfWeek.WEDNESDAY to -0.1,
                DayOfWeek.THURSDAY to 0.0,
                DayOfWeek.FRIDAY to 0.3,
                DayOfWeek.SATURDAY to 0.5,
                DayOfWeek.SUNDAY to 0.2,
            )

        val POSITIVE = listOf("joy", "calm", "gratitude", "motivation", "love", "pride")
        val NEGATIVE = listOf("tired", "anxiety", "sadness", "anger", "loneliness", "boredom")
        val OTHER_FACTORS = listOf("family", "health", "food", "money", "nature", "hobbies")

        val NOTES =
            listOf(
                "Buen día en el trabajo, la reunión salió mejor de lo esperado.",
                "Comida con la familia.",
                "Me ha costado arrancar, pero al final bien.",
                "Tarde de lectura en el sofá.",
                "Paseo largo al atardecer.",
                "Mucho lío hoy, necesito desconectar.",
            )
        val LOW_NOTES =
            listOf(
                "Semana cuesta arriba, duermo mal.",
                "Sin ganas de mucho hoy.",
                "Día gris. Mañana será otro día.",
            )
        val GRATITUDES =
            listOf(
                "Un café tranquilo por la mañana",
                "Una llamada con mi hermana",
                "El sol de la tarde",
                "Haber entrenado aunque no tenía ganas",
                "Una cena rica",
            )
        const val LONG_TEXT = "Texto larguísimo para comprobar que nada se corta ni rompe el diseño. "
    }
}

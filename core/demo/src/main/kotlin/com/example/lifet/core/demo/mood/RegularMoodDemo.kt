package com.example.lifet.core.demo.mood

import com.example.lifet.core.model.mood.MoodEntry
import com.example.lifet.core.model.mood.MoodLevel
import com.example.lifet.core.model.mood.MoodRules
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.Random
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Escenario REGULAR: ~6 meses con patrones que Insights debe encontrar. Lunes más bajos, mejores días con
 * deporte, una ligera ola estacional, días sueltos sin registro y una racha baja de 5 días que termina
 * anteayer (para la tarjeta de apoyo).
 */
internal class RegularMoodDemo(
    private val clock: Clock,
    private val random: Random,
) {
    private val today: LocalDate = LocalDate.now(clock)

    fun build(): List<MoodEntry> =
        (REGULAR_DAYS downTo 0L).flatMap { daysAgo ->
            val date = today.minusDays(daysAgo)
            val lowStreak = daysAgo in LOW_STREAK_DAYS_AGO
            val skip = !lowStreak && daysAgo > 0 && random.nextDouble() < SKIP_DAY_PROBABILITY
            if (skip) emptyList() else day(date, lowStreak)
        }

    private fun day(
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
                val minute = time.plusMinutes(random.nextInt(MAX_MINUTE_JITTER).toLong())
                val entry = moodAt(date, minute, level, clock.zone)
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
        val mondayStress = if (date.dayOfWeek == DayOfWeek.MONDAY) 1 else 0
        val sportEnergy = if (sport) 1 else 0
        val notes = if (lowStreak) LOW_NOTES else NOTES
        return copy(
            emotions = emotionsFor(level),
            energy = scaleAround(level.value + sportEnergy),
            stress = scaleAround(MoodRules.SCALE_MAX + 1 - level.value + mondayStress),
            factors = factorsFor(date, sport, lowStreak),
            note = if (random.nextDouble() < NOTE_PROBABILITY) pick(notes) else "",
            gratitude = if (random.nextDouble() < GRATITUDE_PROBABILITY) pick(GRATITUDES) else "",
        )
    }

    private fun factorsFor(
        date: LocalDate,
        sport: Boolean,
        lowStreak: Boolean,
    ): List<String> {
        val weekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
        return buildList {
            if (sport) add("sport")
            if (!weekend && random.nextDouble() < WORK_FACTOR_PROBABILITY) add("work")
            if (weekend && random.nextDouble() < SOCIAL_FACTOR_PROBABILITY) add("social")
            if (lowStreak || random.nextDouble() < SLEEP_FACTOR_PROBABILITY) add("sleep")
            if (random.nextDouble() < OTHER_FACTOR_PROBABILITY) add(pick(OTHER_FACTORS))
        }.distinct()
    }

    private fun emotionsFor(level: MoodLevel): List<String> {
        val pool =
            when {
                level >= MoodLevel.GOOD -> POSITIVE
                level <= MoodLevel.LOW -> NEGATIVE
                else -> POSITIVE.take(MIXED_TAKE) + NEGATIVE.take(MIXED_TAKE)
            }
        return List(1 + random.nextInt(MAX_EXTRA_EMOTIONS)) { pick(pool) }.distinct()
    }

    /** Valor de 1 a 5 alrededor de [center] con una variación de ±1. */
    private fun scaleAround(center: Int): Int =
        (center + random.nextInt(SCALE_JITTER) - 1).coerceIn(MoodRules.SCALE_MIN, MoodRules.SCALE_MAX)

    private fun levelFrom(value: Double): MoodLevel =
        MoodLevel.fromValue(value.roundToInt().coerceIn(MoodLevel.MIN, MoodLevel.MAX)) ?: MoodLevel.NEUTRAL

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
        const val MIXED_TAKE = 2
        const val MAX_EXTRA_EMOTIONS = 2
        const val SCALE_JITTER = 3

        val CHECK_IN_SLOTS: List<LocalTime> =
            listOf(LocalTime.of(8, 0), LocalTime.of(14, 0), LocalTime.of(21, 0))

        val WEEKDAY_OFFSET: Map<DayOfWeek, Double> =
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
    }
}

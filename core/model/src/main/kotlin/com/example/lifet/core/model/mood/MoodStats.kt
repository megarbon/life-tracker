package com.example.lifet.core.model.mood

import com.example.lifet.core.model.DateRange
import java.time.LocalDate
import kotlin.math.abs

/**
 * Cálculos puros sobre check-ins de ánimo. Se agrupa siempre por día lógico ([MoodEntry.localDate]) y cada día
 * pesa lo mismo, aunque tenga varios check-ins, para que un día con muchos registros no sesgue las medias.
 */
public object MoodStats {
    private const val DEFAULT_TOP_EMOTIONS = 5

    /** Media de nivel por día. */
    public fun dailyAverages(entries: List<MoodEntry>): Map<LocalDate, Double> =
        entries
            .groupBy { it.localDate }
            .mapValues { (_, dayEntries) -> dayEntries.map { it.level.value }.average() }

    /** Media de las medias diarias, o `null` si no hay datos. */
    public fun averageOfDays(entries: List<MoodEntry>): Double? =
        dailyAverages(entries).values.takeIf { it.isNotEmpty() }?.average()

    /**
     * Serie diaria de [range]. Para que la media móvil de los primeros días sea correcta, [entries] puede incluir
     * los 6 días anteriores al rango; las métricas agregadas sólo cuentan los días dentro del rango.
     */
    public fun series(
        range: DateRange,
        entries: List<MoodEntry>,
        topEmotionsLimit: Int = DEFAULT_TOP_EMOTIONS,
    ): MoodSeries {
        val daily = dailyAverages(entries)
        val counts = entries.groupingBy { it.localDate }.eachCount()
        val inRange = entries.filter { it.localDate in range }
        val points =
            range.days.map { date ->
                MoodDayPoint(
                    date = date,
                    average = daily[date],
                    movingAverage = movingAverage(daily, date),
                    entryCount = counts[date] ?: 0,
                )
            }
        return MoodSeries(
            range = range,
            days = points,
            average = averageOfDays(inRange),
            entryCount = inRange.size,
            recentDelta = recentDelta(daily, range.endInclusive),
            topEmotions = topEmotions(inRange, topEmotionsLimit),
            factorImpacts = factorImpacts(inRange),
        )
    }

    /** Media de los días con datos en la ventana de 7 días que termina en [date] (incluido). */
    public fun movingAverage(
        daily: Map<LocalDate, Double>,
        date: LocalDate,
        windowDays: Int = MoodRules.MOVING_AVERAGE_DAYS,
    ): Double? =
        (0 until windowDays)
            .mapNotNull { daily[date.minusDays(it.toLong())] }
            .takeIf { it.isNotEmpty() }
            ?.average()

    /** Últimos 7 días que terminan en [end] frente a los 7 anteriores. */
    public fun recentDelta(
        daily: Map<LocalDate, Double>,
        end: LocalDate,
    ): Double? {
        val window = MoodRules.MOVING_AVERAGE_DAYS.toLong()
        val recent = movingAverage(daily, end)
        val previous = movingAverage(daily, end.minusDays(window))
        return if (recent != null && previous != null) recent - previous else null
    }

    /** Emociones más frecuentes, de mayor a menor (empate: orden alfabético para que sea estable). */
    public fun topEmotions(
        entries: List<MoodEntry>,
        limit: Int = DEFAULT_TOP_EMOTIONS,
    ): List<EmotionCount> =
        entries
            .flatMap { it.emotions }
            .groupingBy { it }
            .eachCount()
            .map { (emotion, count) -> EmotionCount(emotion, count) }
            .sortedWith(compareByDescending<EmotionCount> { it.count }.thenBy { it.emotion })
            .take(limit)

    /**
     * Ánimo medio de los días con y sin cada factor. Sólo se incluyen factores con al menos
     * [MoodRules.MIN_FACTOR_DAYS] días a cada lado, ordenados por la magnitud del efecto.
     * Es una comparación descriptiva, no causal: la UI debe expresarlo con prudencia.
     */
    public fun factorImpacts(entries: List<MoodEntry>): List<FactorImpact> {
        val byDay = entries.groupBy { it.localDate }
        val dayAverage = byDay.mapValues { (_, day) -> day.map { it.level.value }.average() }
        val dayFactors = byDay.mapValues { (_, day) -> day.flatMapTo(mutableSetOf()) { it.factors } }
        val allFactors = dayFactors.values.flatten().toSortedSet()
        return allFactors
            .mapNotNull { factor ->
                val (withFactor, withoutFactor) =
                    dayAverage.entries.partition { factor in dayFactors.getValue(it.key) }
                val enoughData =
                    withFactor.size >= MoodRules.MIN_FACTOR_DAYS && withoutFactor.size >= MoodRules.MIN_FACTOR_DAYS
                if (enoughData) {
                    FactorImpact(
                        factor = factor,
                        averageWith = withFactor.map { it.value }.average(),
                        averageWithout = withoutFactor.map { it.value }.average(),
                        daysWith = withFactor.size,
                        daysWithout = withoutFactor.size,
                    )
                } else {
                    null
                }
            }.sortedByDescending { abs(it.delta) }
    }

    /**
     * Días seguidos con media por debajo de [MoodRules.LOW_DAY_THRESHOLD] que terminan en [today] o, si hoy
     * aún no hay registro, en ayer. Un día sin datos corta la racha: no se presupone cómo estuvo.
     */
    public fun lowStreak(
        daily: Map<LocalDate, Double>,
        today: LocalDate,
    ): Int {
        val start = if (daily.containsKey(today)) today else today.minusDays(1)
        return generateSequence(start) { it.minusDays(1) }
            .takeWhile { day -> daily[day]?.let { it < MoodRules.LOW_DAY_THRESHOLD } == true }
            .count()
    }

    /** Resumen para la pantalla "Hoy" a partir de los check-ins recientes (bastan los últimos 14 días). */
    public fun todaySummary(
        entries: List<MoodEntry>,
        today: LocalDate,
    ): MoodTodaySummary {
        val todayEntries = entries.filter { it.localDate == today }.sortedBy { it.instant }
        val daily = dailyAverages(entries)
        return MoodTodaySummary(
            averageToday = todayEntries.takeIf { it.isNotEmpty() }?.map { it.level.value }?.average(),
            checkInsToday = todayEntries.size,
            weekAverage = movingAverage(daily, today),
            lastLevel = todayEntries.lastOrNull()?.level,
            lowStreakDays = lowStreak(daily, today),
        )
    }
}

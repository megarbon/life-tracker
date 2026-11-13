package com.example.lifet.core.model.quit

import com.example.lifet.core.model.CurrencyCode
import com.example.lifet.core.model.DateRange
import com.example.lifet.core.model.Money
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * Cálculos de Dejar de fumar y beber sobre un [QuitDataSet]. Pura y determinista: "ahora" y la zona llegan de fuera.
 *
 * Un consumo reinicia sólo la racha actual: el récord, los días limpios totales y el ahorro acumulado se conservan.
 * Los consumos anteriores al inicio del hábito o posteriores a "ahora" no cuentan.
 */
public class QuitCalculator(
    private val data: QuitDataSet,
    private val now: Instant,
    private val zone: ZoneId,
) {
    private val today: LocalDate = now.atZone(zone).toLocalDate()

    private val cravingsByHabit: Map<String, List<QuitCraving>> by lazy {
        data.cravings.filter { it.at <= now }.groupBy { it.habitId }
    }

    private val consumptionsByHabit: Map<String, List<QuitConsumption>> by lazy {
        data.consumptions.filter { it.at <= now }.groupBy { it.habitId }
    }

    private val habitsInOrder: List<QuitHabit> by lazy {
        data.habits.sortedWith(compareBy<QuitHabit> { it.startedAt }.thenBy { it.id })
    }

    /** Hábito principal: el que se deja del todo y empezó antes; si no hay ninguno, el más antiguo. */
    public fun primaryHabit(): QuitHabit? =
        habitsInOrder.firstOrNull { it.mode == HabitMode.QUIT } ?: habitsInOrder.firstOrNull()

    public fun overview(): QuitOverview {
        val progress = habitsInOrder.map(::progress)
        val events: List<QuitEvent> =
            (data.cravings + data.consumptions)
                .filter { it.at <= now }
                .sortedWith(compareByDescending<QuitEvent> { it.at }.thenBy { it.id })
        return QuitOverview(
            now = now,
            today = today,
            habits = progress,
            primaryId = primaryHabit()?.id,
            events = events,
            summary = todaySummary(progress),
        )
    }

    public fun todaySummary(): QuitTodaySummary = todaySummary(habitsInOrder.map(::progress))

    private fun todaySummary(progress: List<HabitProgress>): QuitTodaySummary {
        val primary = progress.firstOrNull { it.habit.id == primaryHabit()?.id }
        val currency = primary?.habit?.packPrice?.currency ?: CurrencyCode.EUR
        val sameCurrency = progress.filter { it.habit.packPrice.currency == currency }
        val reducing = primary?.thisWeek
        return QuitTodaySummary(
            hasData = primary != null,
            primaryHabitName = primary?.habit?.name,
            primaryKind = primary?.habit?.kind,
            primaryMode = primary?.habit?.mode,
            cleanSince = primary?.cleanSince,
            cleanSeconds = primary?.cleanSeconds ?: 0L,
            bestStreakSeconds = primary?.bestStreakSeconds ?: 0L,
            totalSaved = sumSaturating(currency, sameCurrency.map { it.saved }),
            hasOtherCurrencies = sameCurrency.size != progress.size,
            nextMilestone = primary?.nextMilestone,
            cravingToday = progress.any { it.cravingsToday > 0 },
            cravingsToday = progress.sumOf { it.cravingsToday },
            consumedToday = progress.any { it.consumedToday },
            weekUnits = reducing?.units,
            weeklyLimit = reducing?.limit,
        )
    }

    /**
     * Serie diaria de [range] hasta hoy. Un día es limpio si había un hábito en marcha y ningún consumo de los
     * hábitos que se dejan del todo (si todos están en modo reducción, de cualquiera): beber dentro del límite
     * semanal no debe convertir cada día en "no limpio". Los antojos y las unidades cuentan todos los hábitos.
     */
    public fun series(range: DateRange): QuitSeries {
        val relevant = habitsInOrder.filter { it.mode == HabitMode.QUIT }.ifEmpty { habitsInOrder }
        val firstStart = relevant.firstOrNull()?.startDate
        val relevantIds = relevant.mapTo(mutableSetOf()) { it.id }
        val consumedByDate = consumptionsByHabit.values.flatten().groupBy { it.date }
        val cravingsByDate = cravingsByHabit.values.flatten().groupBy { it.date }
        val days =
            range.days.filter { it <= today }.map { date ->
                val tracked = firstStart != null && date >= firstStart
                val consumed = consumedByDate[date].orEmpty()
                val cravings = cravingsByDate[date].orEmpty()
                QuitDayPoint(
                    date = date,
                    tracked = tracked,
                    clean = tracked && consumed.none { it.habitId in relevantIds },
                    cravings = cravings.size,
                    maxCravingIntensity = cravings.maxOfOrNull { it.intensity },
                    consumedUnits = consumed.sumOf { it.units },
                )
            }
        return QuitSeries(range, days)
    }

    /** Progreso de un hábito en el instante "ahora". */
    public fun progress(habit: QuitHabit): HabitProgress {
        val cravings = cravingsByHabit[habit.id].orEmpty()
        val consumptions =
            consumptionsByHabit[habit.id]
                .orEmpty()
                .filter { it.at >= habit.startedAt }
                .sortedBy { it.at }
        val streaks = streaks(habit.startedAt, consumptions.map { it.at })
        val consumedUnits = consumptions.sumOf { it.units.toLong() }
        val milestones =
            if (habit.mode == HabitMode.QUIT) {
                milestonesFor(streaks.cleanSince, streaks.currentSeconds, streaks.bestSeconds)
            } else {
                emptyList()
            }
        val weeks = weeks(habit, consumptions)
        return HabitProgress(
            habit = habit,
            now = now,
            started = now >= habit.startedAt,
            cleanSince = streaks.cleanSince,
            cleanSeconds = streaks.currentSeconds,
            bestStreakSeconds = streaks.bestSeconds,
            totalCleanDays = totalCleanDays(habit, consumptions),
            saved = habit.savedAt(now, consumedUnits),
            avoidedUnits = avoidedUnits(habit, consumedUnits),
            slips = consumptions.size,
            consumedUnits = consumedUnits,
            lastConsumptionAt = consumptions.lastOrNull()?.at,
            consumedToday = consumptions.any { it.date == today },
            milestones = milestones,
            nextMilestone = milestones.firstOrNull { !it.reachedNow },
            weeks = weeks,
            compliance = compliance(habit, weeks),
            cravingsTotal = cravings.size,
            cravingsToday = cravings.count { it.date == today },
            cravingsByHour = cravingsByHour(cravings),
            topTriggers = topTriggers(cravings),
            savings = savings(habit, consumptions),
            intensity = intensity(habit, consumptions),
        )
    }

    private class Streaks(
        val cleanSince: Instant,
        val currentSeconds: Long,
        val bestSeconds: Long,
    )

    /** Racha actual y récord: la racha termina en cada consumo y la actual sigue hasta "ahora". */
    private fun streaks(
        start: Instant,
        slips: List<Instant>,
    ): Streaks {
        var cursor = start
        var best = 0L
        slips.forEach { slip ->
            best = maxOf(best, secondsBetween(cursor, slip))
            cursor = slip
        }
        val current = secondsBetween(cursor, now)
        return Streaks(cleanSince = cursor, currentSeconds = current, bestSeconds = maxOf(best, current))
    }

    private fun secondsBetween(
        from: Instant,
        to: Instant,
    ): Long = (to.epochSecond - from.epochSecond).coerceAtLeast(0L)

    /** Días ya completos desde el inicio sin ningún consumo; el día en curso aún no cuenta. */
    private fun totalCleanDays(
        habit: QuitHabit,
        consumptions: List<QuitConsumption>,
    ): Int {
        val completed = ChronoUnit.DAYS.between(habit.startDate, today).coerceAtLeast(0L)
        val slipDays =
            consumptions
                .map { it.date }
                .filter { it >= habit.startDate && it < today }
                .toSet()
                .size
        return (completed - slipDays).coerceAtLeast(0L).toInt()
    }

    /** Unidades que habría consumido con el hábito previo menos las consumidas de verdad, con suelo en cero. */
    private fun avoidedUnits(
        habit: QuitHabit,
        consumedUnits: Long,
    ): Long {
        val elapsed = secondsBetween(habit.startedAt, now)
        val expected = habit.baselineUnits.toLong() * elapsed / habit.baselinePeriod.seconds
        return (expected - consumedUnits).coerceAtLeast(0L)
    }

    /** Las últimas [QuitRules.WEEKS_SHOWN] semanas (lunes a domingo) con su consumo; la última es la actual. */
    private fun weeks(
        habit: QuitHabit,
        consumptions: List<QuitConsumption>,
    ): List<WeekUsage> {
        val unitsByDate = consumptions.groupBy { it.date }.mapValues { (_, items) -> items.sumOf { it.units } }
        val currentMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val limit = if (habit.mode == HabitMode.REDUCE) habit.weeklyLimit else null
        return (QuitRules.WEEKS_SHOWN - 1 downTo 0).map { back ->
            val monday = currentMonday.minusWeeks(back.toLong())
            val units = (0 until QuitRules.DAYS_PER_WEEK).sumOf { unitsByDate[monday.plusDays(it.toLong())] ?: 0 }
            WeekUsage(weekStart = monday, units = units, limit = limit)
        }
    }

    /** Las últimas semanas completas (sin la actual) que ya existían cuando empezó el hábito. */
    private fun compliance(
        habit: QuitHabit,
        weeks: List<WeekUsage>,
    ): List<WeekUsage> =
        weeks
            .dropLast(1)
            .filter { it.weekStart.plusDays((QuitRules.DAYS_PER_WEEK - 1).toLong()) >= habit.startDate }
            .takeLast(QuitRules.COMPLIANCE_WEEKS)

    private fun cravingsByHour(cravings: List<QuitCraving>): List<Int> {
        val counts = IntArray(QuitRules.HOURS_PER_DAY)
        cravings.forEach { counts[it.at.atZone(it.zone).hour]++ }
        return counts.toList()
    }

    private fun topTriggers(cravings: List<QuitCraving>): List<TriggerCount> =
        cravings
            .groupingBy { it.trigger }
            .eachCount()
            .map { (trigger, count) -> TriggerCount(trigger, count) }
            .sortedWith(compareByDescending<TriggerCount> { it.count }.thenBy { it.trigger.ordinal })

    /** Ahorro al final de varios días: uno por día si el hábito es reciente y [QuitRules.SAVINGS_POINTS] si no. */
    private fun savings(
        habit: QuitHabit,
        consumptions: List<QuitConsumption>,
    ): List<SavingsPoint> {
        if (habit.startDate > today) return emptyList()
        return savingsDates(habit.startDate).map { date ->
            val endOfDay = date.plusDays(1).atStartOfDay(zone).toInstant()
            val at = if (endOfDay < now) endOfDay else now
            val consumed = consumptions.filter { it.at <= at }.sumOf { it.units.toLong() }
            SavingsPoint(date, habit.savedAt(at, consumed))
        }
    }

    private fun savingsDates(start: LocalDate): List<LocalDate> {
        val age = ChronoUnit.DAYS.between(start, today)
        val lastIndex = (QuitRules.SAVINGS_POINTS - 1).toLong()
        return if (age <= lastIndex) {
            (0..age).map { start.plusDays(it) }
        } else {
            (0..lastIndex).map { start.plusDays(age * it / lastIndex) }.distinct()
        }
    }

    /** Calendario de los últimos meses: 1 si el día fue limpio, tenue si hubo consumo; sin dato antes de empezar. */
    private fun intensity(
        habit: QuitHabit,
        consumptions: List<QuitConsumption>,
    ): Map<LocalDate, Float> {
        val firstDay = YearMonth.from(today).minusMonths((QuitRules.HEATMAP_MONTHS - 1).toLong()).atDay(1)
        val slipDays = consumptions.map { it.date }.toSet()
        val from = maxOf(firstDay, habit.startDate)
        if (from > today) return emptyMap()
        return generateSequence(from) { it.plusDays(1) }
            .takeWhile { it <= today }
            .associateWith { if (it in slipDays) QuitRules.CONSUMPTION_DAY_LEVEL else 1f }
    }
}

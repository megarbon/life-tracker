package com.example.lifet.core.model.quit

import com.example.lifet.core.model.DateRange
import com.example.lifet.core.model.Money
import java.time.Instant
import java.time.LocalDate

/** Veces que un [trigger] aparece en los antojos de un hábito. */
public data class TriggerCount(
    public val trigger: CravingTrigger,
    public val count: Int,
)

/** Ahorro acumulado de un hábito al final del día [date] (o hasta ahora, si es hoy). */
public data class SavingsPoint(
    public val date: LocalDate,
    public val saved: Money,
)

/**
 * Consumo de una semana (de lunes a domingo) que empieza en [weekStart]. Con [limit] `null` (modo dejar del todo)
 * la semana no se evalúa y [compliant] es siempre verdadero.
 */
public data class WeekUsage(
    public val weekStart: LocalDate,
    public val units: Int,
    public val limit: Int?,
) {
    public val compliant: Boolean get() = limit == null || units <= limit

    /** Unidades que quedan hasta el límite; cero si ya se llegó o se pasó. */
    public val remaining: Int get() = limit?.let { (it - units).coerceAtLeast(0) } ?: 0
}

/**
 * Todo lo que la pantalla necesita de un hábito, calculado en el instante [now].
 *
 * Un consumo no borra nada: [cleanSeconds] cuenta desde el último y [bestStreakSeconds] conserva el récord histórico
 * (nunca es menor que la racha actual). [totalCleanDays] y [saved] son totales de toda la vida del hábito.
 *
 * @property cleanSince instante en que empezó la racha actual (el inicio del hábito o el último consumo).
 * @property totalCleanDays días completos desde el inicio sin ningún consumo ese día.
 * @property avoidedUnits unidades que no se han consumido respecto al consumo previo (nunca negativo).
 * @property slips cuántos consumos se han registrado; [consumedUnits] son las unidades que suman.
 * @property milestones hitos de tiempo limpio; vacío en modo reducción, donde manda el límite semanal.
 * @property nextMilestone el primer hito que la racha actual aún no ha alcanzado.
 * @property weeks las últimas semanas con su consumo, de la más antigua a la actual (la última).
 * @property compliance las semanas completas más recientes (sin la actual) para "cumplido X de 4".
 * @property cravingsByHour antojos por hora del día (0 a 23) según la hora local de cada uno, 24 valores.
 * @property intensity días del calendario de los últimos meses: 1 si limpio, tenue si hubo consumo.
 */
public data class HabitProgress(
    public val habit: QuitHabit,
    public val now: Instant,
    public val started: Boolean,
    public val cleanSince: Instant,
    public val cleanSeconds: Long,
    public val bestStreakSeconds: Long,
    public val totalCleanDays: Int,
    public val saved: Money,
    public val avoidedUnits: Long,
    public val slips: Int,
    public val consumedUnits: Long,
    public val lastConsumptionAt: Instant?,
    public val consumedToday: Boolean,
    public val milestones: List<Milestone>,
    public val nextMilestone: Milestone?,
    public val weeks: List<WeekUsage>,
    public val compliance: List<WeekUsage>,
    public val cravingsTotal: Int,
    public val cravingsToday: Int,
    public val cravingsByHour: List<Int>,
    public val topTriggers: List<TriggerCount>,
    public val savings: List<SavingsPoint>,
    public val intensity: Map<LocalDate, Float>,
) {
    public val cleanDays: Int get() = (cleanSeconds / QuitRules.SECONDS_PER_DAY).toInt()

    public val bestStreakDays: Int get() = (bestStreakSeconds / QuitRules.SECONDS_PER_DAY).toInt()

    /** `true` si ha habido algún consumo: la pantalla enseña entonces la racha actual junto al récord. */
    public val hasSlipped: Boolean get() = slips > 0

    /** Semana en curso, sólo en modo reducción. */
    public val thisWeek: WeekUsage? get() = if (habit.mode == HabitMode.REDUCE) weeks.lastOrNull() else null

    public val compliantWeeks: Int get() = compliance.count { it.compliant }

    /** Antojos agrupados en franjas de [blockHours] horas seguidas (por defecto tres: 0-3, 3-6... 21-24). */
    public fun cravingsByBlock(blockHours: Int = DEFAULT_BLOCK_HOURS): List<Int> =
        cravingsByHour.chunked(blockHours) { it.sum() }

    private companion object {
        const val DEFAULT_BLOCK_HOURS = 3
    }
}

/**
 * Lo que la pantalla Hoy necesita de Dejar de fumar y beber: el hábito principal, lo ahorrado en total, el próximo
 * hito y si hoy hubo antojos. `hasData = false` indica que aún no hay ningún hábito.
 *
 * @property primaryHabitName hábito principal: el que se deja del todo y lleva más tiempo (o el más antiguo).
 * @property hasOtherCurrencies `true` si hay hábitos en otra divisa que [totalSaved] no suma.
 * @property weekUnits consumo de esta semana si el hábito principal está en modo reducción.
 */
public data class QuitTodaySummary(
    public val hasData: Boolean,
    public val primaryHabitName: String?,
    public val primaryKind: HabitKind?,
    public val primaryMode: HabitMode?,
    public val cleanSince: Instant?,
    public val cleanSeconds: Long,
    public val bestStreakSeconds: Long,
    public val totalSaved: Money,
    public val hasOtherCurrencies: Boolean,
    public val nextMilestone: Milestone?,
    public val cravingToday: Boolean,
    public val cravingsToday: Int,
    public val consumedToday: Boolean,
    public val weekUnits: Int?,
    public val weeklyLimit: Int?,
) {
    public val cleanDays: Int get() = (cleanSeconds / QuitRules.SECONDS_PER_DAY).toInt()
}

/**
 * Un día de la serie. [tracked] es falso antes de que empezara el primer hábito; [clean] es verdadero si ese día
 * hubo hábitos en marcha y ningún consumo.
 */
public data class QuitDayPoint(
    public val date: LocalDate,
    public val tracked: Boolean,
    public val clean: Boolean,
    public val cravings: Int,
    public val maxCravingIntensity: Int?,
    public val consumedUnits: Int,
)

/** Serie diaria de un [range] hasta hoy (los días sin datos van con cero), para gráficas e Insights. */
public data class QuitSeries(
    public val range: DateRange,
    public val days: List<QuitDayPoint>,
) {
    public val cravingCount: Int get() = days.sumOf { it.cravings }

    public val cleanDays: Int get() = days.count { it.clean }
}

/**
 * Todo lo que necesita la pantalla, recalculado con cada cambio en la base de datos.
 *
 * @property habits un [HabitProgress] por hábito, del que empezó antes al que empezó después.
 * @property primaryId id del hábito principal, o `null` si no hay ninguno.
 * @property events cravings y consumos de todos los hábitos, del más reciente al más antiguo.
 */
public data class QuitOverview(
    public val now: Instant,
    public val today: LocalDate,
    public val habits: List<HabitProgress>,
    public val primaryId: String?,
    public val events: List<QuitEvent>,
    public val summary: QuitTodaySummary,
) {
    public val hasAnyRecord: Boolean get() = habits.isNotEmpty()
}

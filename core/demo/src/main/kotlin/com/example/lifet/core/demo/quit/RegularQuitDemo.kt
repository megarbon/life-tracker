@file:Suppress("MagicNumber") // Probabilidades, horas y rangos literales: son los parámetros de los datos de ejemplo.

package com.example.lifet.core.demo.quit

import com.example.lifet.core.model.quit.BaselinePeriod
import com.example.lifet.core.model.quit.CopingStrategy
import com.example.lifet.core.model.quit.CravingTrigger
import com.example.lifet.core.model.quit.HabitKind
import com.example.lifet.core.model.quit.HabitMode
import com.example.lifet.core.model.quit.QuitConsumption
import com.example.lifet.core.model.quit.QuitCraving
import com.example.lifet.core.model.quit.QuitDataSet
import com.example.lifet.core.model.quit.QuitHabit
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.Random
import kotlin.math.exp

private const val TOBACCO_DAYS = 47L
private const val TOBACCO_SLIP_DAY = 12L
private const val ALCOHOL_WEEKS = 12L
private const val ALCOHOL_LIMIT = 4

/** Consumo semanal de las últimas cuatro semanas completas (de la más antigua a la más reciente): cumple 3 de 4. */
private val RECENT_WEEK_UNITS = listOf(4, 7, 3, 4)

/** Peso de cada hora del día para un antojo de tabaco: tras el desayuno y tras comer son las horas fuertes. */
private val HOUR_WEIGHTS =
    intArrayOf(1, 0, 0, 0, 0, 0, 1, 3, 8, 10, 6, 4, 5, 9, 8, 3, 4, 5, 6, 8, 7, 4, 3, 2)

private val MORNING_TRIGGERS =
    listOf(CravingTrigger.COFFEE to 60, CravingTrigger.ROUTINE to 20, CravingTrigger.STRESS to 20)
private val WORK_TRIGGERS =
    listOf(CravingTrigger.WORK_BREAK to 50, CravingTrigger.STRESS to 30, CravingTrigger.BOREDOM to 20)
private val MIDDAY_TRIGGERS =
    listOf(CravingTrigger.AFTER_MEAL to 65, CravingTrigger.COFFEE to 20, CravingTrigger.STRESS to 15)
private val EVENING_TRIGGERS =
    listOf(
        CravingTrigger.SOCIAL to 30,
        CravingTrigger.TIREDNESS to 25,
        CravingTrigger.EMOTIONS to 25,
        CravingTrigger.ROUTINE to 20,
    )
private val NIGHT_TRIGGERS =
    listOf(CravingTrigger.BOREDOM to 50, CravingTrigger.TIREDNESS to 30, CravingTrigger.EMOTIONS to 20)

private val ALCOHOL_TRIGGERS = listOf(CravingTrigger.SOCIAL, CravingTrigger.ROUTINE, CravingTrigger.STRESS)

private val DRINKING_DAYS =
    listOf(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SUNDAY, DayOfWeek.THURSDAY)

private val COPING =
    listOf(
        CopingStrategy.BREATHING,
        CopingStrategy.WALK,
        CopingStrategy.WATER,
        CopingStrategy.DISTRACTION,
        CopingStrategy.CALL_SOMEONE,
        CopingStrategy.WAIT,
        CopingStrategy.EXERCISE,
        CopingStrategy.SNACK,
    )

/**
 * Escenario REGULAR: el tabaco se dejó hace 47 días con una recaída el día 12 (los antojos son más frecuentes e
 * intensos al principio y se calman con el tiempo, y se concentran tras el café y tras comer) y el alcohol está en
 * modo reducción desde hace doce semanas con un límite semanal de cuatro copas: de las últimas cuatro semanas
 * completas se cumplen tres. Todo el azar sale de [random]; las fechas, de [clock].
 */
@Suppress("TooManyFunctions") // Un método por patrón de la demo (antojos, consumos, horas, disparadores...).
internal class RegularQuitDemo(
    clock: Clock,
    private val random: Random,
) {
    private val zone: ZoneId = clock.zone
    private val today: LocalDate = LocalDate.now(clock)
    private var counter = 0

    fun build(): QuitDataSet {
        val tobacco = tobacco()
        val alcohol = alcohol()
        val slip =
            demoConsumption(
                id = nextId("slip"),
                habitId = tobacco.id,
                at = instantAt(tobacco.startDate.plusDays(TOBACCO_SLIP_DAY), LocalTime.of(21, 40), zone),
                zone = zone,
                units = 3,
                trigger = CravingTrigger.SOCIAL,
                note = "Cena con amigos y todos fumaban. Mañana seguimos.",
            )
        return QuitDataSet(
            habits = listOf(tobacco, alcohol),
            cravings = tobaccoCravings(tobacco) + alcoholCravings(alcohol),
            consumptions = listOf(slip) + alcoholDrinks(alcohol),
        )
    }

    private fun nextId(prefix: String): String = "demo-quit-$prefix-${counter++}"

    private fun tobacco(): QuitHabit =
        demoHabit(
            id = "demo-quit-tobacco",
            kind = HabitKind.TOBACCO,
            name = "Tabaco",
            startedAt = instantAt(today.minusDays(TOBACCO_DAYS), LocalTime.of(8, 30), zone),
            zone = zone,
            baselineUnits = 20,
            baselinePeriod = BaselinePeriod.DAY,
            unitsPerPack = 20,
            priceMinor = 540,
        )

    private fun alcohol(): QuitHabit {
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(ALCOHOL_WEEKS)
        return demoHabit(
            id = "demo-quit-alcohol",
            kind = HabitKind.ALCOHOL,
            name = "Alcohol",
            startedAt = instantAt(monday, LocalTime.of(10, 0), zone),
            zone = zone,
            baselineUnits = 14,
            baselinePeriod = BaselinePeriod.WEEK,
            unitsPerPack = 1,
            priceMinor = 450,
            mode = HabitMode.REDUCE,
            weeklyLimit = ALCOHOL_LIMIT,
        )
    }

    // --- Antojos de tabaco ---

    private fun tobaccoCravings(habit: QuitHabit): List<QuitCraving> =
        (0..TOBACCO_DAYS).flatMap { dayIndex ->
            val date = habit.startDate.plusDays(dayIndex)
            List(cravingsOn(dayIndex)) { cravingAt(habit, date, dayIndex) }
        }

    /** Antojos esperados ese día: unos tres al día al principio, y cerca de uno suelto pasado un mes y medio. */
    private fun cravingsOn(dayIndex: Long): Int {
        val boost = if (dayIndex == TOBACCO_SLIP_DAY) 2.0 else 0.0
        val expected = 0.35 + 3.0 * exp(-dayIndex / 18.0) + boost
        val whole = expected.toInt()
        return whole + if (random.nextDouble() < expected - whole) 1 else 0
    }

    private fun cravingAt(
        habit: QuitHabit,
        date: LocalDate,
        dayIndex: Long,
    ): QuitCraving {
        val hour = pickHour()
        val time = LocalTime.of(hour, random.nextInt(60))
        val intensity = (4 - dayIndex / 16 + random.nextInt(3) - 1).toInt().coerceIn(1, 5)
        return demoCraving(
            id = nextId("craving"),
            habitId = habit.id,
            at = instantAt(date, time, zone),
            zone = zone,
            intensity = intensity,
            trigger = triggerFor(hour),
            strategy = if (random.nextInt(100) < 75) COPING[random.nextInt(COPING.size)] else null,
        )
    }

    private fun pickHour(): Int {
        var pick = random.nextInt(HOUR_WEIGHTS.sum())
        for (hour in HOUR_WEIGHTS.indices) {
            pick -= HOUR_WEIGHTS[hour]
            if (pick < 0) return hour
        }
        return HOUR_WEIGHTS.lastIndex
    }

    /** El café manda por la mañana, "después de comer" a mediodía y el cansancio o el ocio por la noche. */
    private fun triggerFor(hour: Int): CravingTrigger =
        pickWeighted(
            when (hour) {
                in 6..10 -> MORNING_TRIGGERS
                in 11..12, in 16..17 -> WORK_TRIGGERS
                in 13..15 -> MIDDAY_TRIGGERS
                in 18..21 -> EVENING_TRIGGERS
                else -> NIGHT_TRIGGERS
            },
        )

    private fun pickWeighted(options: List<Pair<CravingTrigger, Int>>): CravingTrigger {
        var pick = random.nextInt(options.sumOf { it.second })
        for ((trigger, weight) in options) {
            pick -= weight
            if (pick < 0) return trigger
        }
        return options.last().first
    }

    // --- Alcohol ---

    private fun alcoholDrinks(habit: QuitHabit): List<QuitConsumption> {
        val currentMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return (ALCOHOL_WEEKS downTo 0L).flatMap { back ->
            val monday = currentMonday.minusWeeks(back)
            weekSessions(unitsFor(back)).mapIndexed { index, units ->
                val date = monday.with(DRINKING_DAYS[index % DRINKING_DAYS.size])
                val time = LocalTime.of(20 + random.nextInt(3), random.nextInt(60))
                demoConsumption(
                    id = nextId("drink"),
                    habitId = habit.id,
                    at = instantAt(date, time, zone),
                    zone = zone,
                    units = units,
                    trigger = if (units >= 3) CravingTrigger.SOCIAL else null,
                )
            }
        }
    }

    /** Unidades de una semana: baja poco a poco al principio, y las cuatro últimas completas son fijas. */
    private fun unitsFor(weeksBack: Long): Int =
        when {
            weeksBack in 1L..4L -> RECENT_WEEK_UNITS[(4 - weeksBack).toInt()]
            weeksBack == 0L -> 2
            else -> (9 - (ALCOHOL_WEEKS - weeksBack) / 2).toInt() + random.nextInt(2)
        }

    /** Reparte las unidades de una semana en salidas de una a tres copas. */
    private fun weekSessions(units: Int): List<Int> {
        val sessions = mutableListOf<Int>()
        var left = units
        while (left > 0) {
            val next = minOf(left, 1 + random.nextInt(3))
            sessions += next
            left -= next
        }
        return sessions
    }

    private fun alcoholCravings(habit: QuitHabit): List<QuitCraving> =
        (0 until ALCOHOL_WEEKS * 7).filter { random.nextInt(100) < 38 }.map { dayIndex ->
            val date = habit.startDate.plusDays(dayIndex)
            demoCraving(
                id = nextId("alcohol-craving"),
                habitId = habit.id,
                at = instantAt(date, LocalTime.of(18 + random.nextInt(5), random.nextInt(60)), zone),
                zone = zone,
                intensity = 1 + random.nextInt(4),
                trigger = ALCOHOL_TRIGGERS[random.nextInt(ALCOHOL_TRIGGERS.size)],
                strategy = if (random.nextBoolean()) COPING[random.nextInt(COPING.size)] else null,
            )
        }
}

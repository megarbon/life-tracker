package com.example.lifet.core.demo.quit

import com.example.lifet.core.model.CurrencyCode
import com.example.lifet.core.model.Money
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
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Instante a la hora local [time] de [date] en [zone]. */
internal fun instantAt(
    date: LocalDate,
    time: LocalTime,
    zone: ZoneId,
): Instant = date.atTime(time).atZone(zone).toInstant()

internal fun demoHabit(
    id: String,
    kind: HabitKind,
    name: String,
    startedAt: Instant,
    zone: ZoneId,
    baselineUnits: Int,
    baselinePeriod: BaselinePeriod,
    unitsPerPack: Int,
    priceMinor: Long,
    mode: HabitMode = HabitMode.QUIT,
    weeklyLimit: Int? = null,
    currency: CurrencyCode = CurrencyCode.EUR,
): QuitHabit =
    QuitHabit(
        id = id,
        kind = kind,
        name = name,
        mode = mode,
        startedAt = startedAt,
        zone = zone,
        baselineUnits = baselineUnits,
        baselinePeriod = baselinePeriod,
        unitsPerPack = unitsPerPack,
        packPrice = Money(priceMinor, currency),
        weeklyLimit = weeklyLimit,
    )

internal fun demoCraving(
    id: String,
    habitId: String,
    at: Instant,
    zone: ZoneId,
    intensity: Int,
    trigger: CravingTrigger,
    strategy: CopingStrategy? = null,
    note: String = "",
): QuitCraving = QuitCraving(id, habitId, at, zone, intensity, trigger, strategy, note)

internal fun demoConsumption(
    id: String,
    habitId: String,
    at: Instant,
    zone: ZoneId,
    units: Int,
    trigger: CravingTrigger? = null,
    note: String = "",
): QuitConsumption = QuitConsumption(id, habitId, at, zone, units, trigger, note)

/** Repite [seed] hasta acercarse a [max] caracteres sin pasarse, y sin partir nunca un emoji por la mitad. */
internal fun fitText(
    seed: String,
    max: Int,
): String =
    buildString {
        while (length + seed.length <= max) append(seed)
    }

/**
 * Quita lo que aún no ha ocurrido respecto al reloj: hábitos que empiezan después de "ahora" y antojos o consumos
 * posteriores a "ahora" o anteriores al inicio de su hábito. Los datos de demo nunca deben adelantarse a "ahora".
 */
internal fun QuitDataSet.withoutFuture(clock: Clock): QuitDataSet {
    val now = clock.instant()
    val started = habits.filter { it.startedAt <= now }
    val starts = started.associate { it.id to it.startedAt }

    fun isValid(
        at: Instant,
        habitId: String,
    ): Boolean = at <= now && starts[habitId]?.let { at >= it } == true
    return QuitDataSet(
        habits = started,
        cravings = cravings.filter { isValid(it.at, it.habitId) },
        consumptions = consumptions.filter { isValid(it.at, it.habitId) },
    )
}

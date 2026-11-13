package com.example.lifet.core.model.quit

import com.example.lifet.core.model.CurrencyCode
import com.example.lifet.core.model.Money
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

internal val MADRID: ZoneId = ZoneId.of("Europe/Madrid")

/** Instante a la hora local de Madrid. */
internal fun at(
    date: String,
    time: String = "12:00",
): Instant = LocalDateTime.parse("${date}T$time").atZone(MADRID).toInstant()

internal fun date(value: String): LocalDate = LocalDate.parse(value)

/** Tabaco: 20 cigarrillos al día en cajetillas de 20 a 5 €. */
internal fun tobacco(
    startedAt: Instant,
    id: String = "tobacco",
    price: Long = 500,
) = QuitHabit(
    id = id,
    kind = HabitKind.TOBACCO,
    name = "Tabaco",
    mode = HabitMode.QUIT,
    startedAt = startedAt,
    zone = MADRID,
    baselineUnits = 20,
    baselinePeriod = BaselinePeriod.DAY,
    unitsPerPack = 20,
    packPrice = Money(price, CurrencyCode.EUR),
)

/** Alcohol en reducción: antes 14 copas a la semana, a 4 € cada una, ahora con un límite semanal. */
internal fun alcohol(
    startedAt: Instant,
    limit: Int = 4,
    id: String = "alcohol",
) = QuitHabit(
    id = id,
    kind = HabitKind.ALCOHOL,
    name = "Alcohol",
    mode = HabitMode.REDUCE,
    startedAt = startedAt,
    zone = MADRID,
    baselineUnits = 14,
    baselinePeriod = BaselinePeriod.WEEK,
    unitsPerPack = 1,
    packPrice = Money(400, CurrencyCode.EUR),
    weeklyLimit = limit,
)

internal fun slip(
    habitId: String,
    at: Instant,
    units: Int = 1,
    id: String = "slip-$habitId-${at.epochSecond}",
) = QuitConsumption(id = id, habitId = habitId, at = at, zone = MADRID, units = units)

internal fun craving(
    habitId: String,
    at: Instant,
    intensity: Int = 3,
    trigger: CravingTrigger = CravingTrigger.STRESS,
    id: String = "craving-$habitId-${at.epochSecond}",
) = QuitCraving(
    id = id,
    habitId = habitId,
    at = at,
    zone = MADRID,
    intensity = intensity,
    trigger = trigger,
)

@file:Suppress("TooManyFunctions") // Un par de mapeos entity <-> dominio por cada tabla.

package com.example.lifet.core.data.quit

import com.example.lifet.core.database.quit.QuitConsumptionEntity
import com.example.lifet.core.database.quit.QuitCravingEntity
import com.example.lifet.core.database.quit.QuitHabitEntity
import com.example.lifet.core.model.CurrencyCode
import com.example.lifet.core.model.Money
import com.example.lifet.core.model.quit.QuitConsumption
import com.example.lifet.core.model.quit.QuitCraving
import com.example.lifet.core.model.quit.QuitHabit

internal fun QuitHabitEntity.toDomain(): QuitHabit =
    QuitHabit(
        id = id,
        kind = kind,
        name = name,
        mode = mode,
        startedAt = startedAt,
        zone = zoneId,
        baselineUnits = baselineUnits,
        baselinePeriod = baselinePeriod,
        unitsPerPack = unitsPerPack,
        packPrice = Money(packPriceMinor, CurrencyCode(currency)),
        weeklyLimit = weeklyLimit,
    )

internal fun QuitHabit.toEntity(): QuitHabitEntity =
    QuitHabitEntity(
        id = id,
        kind = kind,
        name = name,
        mode = mode,
        startedAt = startedAt,
        zoneId = zone,
        baselineUnits = baselineUnits,
        baselinePeriod = baselinePeriod,
        unitsPerPack = unitsPerPack,
        packPriceMinor = packPrice.amountMinor,
        currency = packPrice.currency.value,
        weeklyLimit = weeklyLimit,
    )

internal fun QuitCravingEntity.toDomain(): QuitCraving =
    QuitCraving(
        id = id,
        habitId = habitId,
        at = occurredAt,
        zone = zoneId,
        intensity = intensity,
        trigger = cravingTrigger,
        strategy = strategy,
        note = note,
    )

internal fun QuitCraving.toEntity(): QuitCravingEntity =
    QuitCravingEntity(
        id = id,
        habitId = habitId,
        occurredAt = at,
        zoneId = zone,
        intensity = intensity,
        cravingTrigger = trigger,
        strategy = strategy,
        note = note,
    )

internal fun QuitConsumptionEntity.toDomain(): QuitConsumption =
    QuitConsumption(
        id = id,
        habitId = habitId,
        at = occurredAt,
        zone = zoneId,
        units = units,
        trigger = cravingTrigger,
        note = note,
    )

internal fun QuitConsumption.toEntity(): QuitConsumptionEntity =
    QuitConsumptionEntity(
        id = id,
        habitId = habitId,
        occurredAt = at,
        zoneId = zone,
        units = units,
        cravingTrigger = trigger,
        note = note,
    )

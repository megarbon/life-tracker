@file:Suppress("MatchingDeclarationName") // Helpers de test compartidos por varios archivos del paquete.

package com.example.lifet.core.model.work

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

internal val MADRID: ZoneId = ZoneId.of("Europe/Madrid")

/** Instante a la hora local [time] (`2026-03-10T09:00`) de Madrid. */
internal fun at(
    time: String,
    zone: ZoneId = MADRID,
): Instant = LocalDateTime.parse(time).atZone(zone).toInstant()

internal fun shift(
    id: String,
    start: String,
    end: String?,
    zone: ZoneId = MADRID,
    breaks: List<WorkBreak> = emptyList(),
    projectId: String? = null,
): WorkShift =
    WorkShift(
        id = id,
        start = at(start, zone),
        zone = zone,
        end = end?.let { at(it, zone) },
        breaks = breaks,
        projectId = projectId,
    )

internal fun pause(
    start: String,
    end: String?,
): WorkBreak = WorkBreak(at(start), end?.let { at(it) })

internal fun day(date: String): LocalDate = LocalDate.parse(date)

internal const val HOUR: Long = 3_600L

@file:Suppress("MagicNumber") // Fechas y horas literales: cada caso límite se describe mejor con su valor exacto.

package com.example.lifet.core.demo.work

import com.example.lifet.core.model.work.FocusKind
import com.example.lifet.core.model.work.FocusSession
import com.example.lifet.core.model.work.ProjectColor
import com.example.lifet.core.model.work.WorkBreak
import com.example.lifet.core.model.work.WorkDataSet
import com.example.lifet.core.model.work.WorkDayNote
import com.example.lifet.core.model.work.WorkProject
import com.example.lifet.core.model.work.WorkRules
import com.example.lifet.core.model.work.WorkSettings
import com.example.lifet.core.model.work.WorkShift
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

private const val LONG_NOTE_SEED = "Reunión maratoniana con el cliente, revisión de requisitos y cierre del sprint 📝 "
private const val LONG_NAME_SEED = "Plataforma de reservas internacional 🌍 "
private const val DST_LOOKBACK = 4

/**
 * Escenario EDGE_CASES: una jornada nocturna que atraviesa el cambio de hora de primavera (una hora menos en tiempo
 * real) y otra el de otoño (una hora más), una jornada de 24 horas exactas, una jornada con veinte pausas, otra
 * registrada en Tokio, una pausa en curso, textos de longitud máxima con emojis, un proyecto con nombre larguísimo,
 * un objetivo semanal poco habitual y un hueco de varios días sin jornadas. Todas respetan los límites del formulario.
 */
internal class EdgeCaseWorkDemo(
    private val clock: Clock,
) {
    private val zone: ZoneId = clock.zone
    private val today: LocalDate = LocalDate.now(clock)

    fun build(): WorkDataSet =
        WorkDataSet(
            shifts = daylightSaving() + longDay() + manyBreaks() + otherZone() + inProgress(),
            projects = projects(),
            focusSessions = focusSessions(),
            notes = listOf(WorkDayNote(today.minusDays(3), fitText(LONG_NOTE_SEED, WorkRules.DAY_NOTE_MAX_LENGTH))),
            settings = WorkSettings(weeklyTargetMinutes = 37 * 60 + 30),
        )

    private fun day(back: Long): LocalDate = today.minusDays(back)

    /**
     * Noches que empiezan la víspera de los dos últimos cambios de hora y cruzan la medianoche: duran 7 h (primavera)
     * y 9 h (otoño) de tiempo real, no 8. Las fechas salen de las reglas de la zona, relativas al reloj; en una zona
     * sin cambio de hora no hay ninguna.
     */
    private fun daylightSaving(): List<WorkShift> {
        val rules = zone.rules
        val transitions =
            generateSequence(rules.previousTransition(clock.instant())) { rules.previousTransition(it.instant) }
        val recent = transitions.take(DST_LOOKBACK).toList()
        val spring = recent.firstOrNull { it.isGap }
        val autumn = recent.firstOrNull { it.isOverlap }
        return listOfNotNull(
            spring?.let { dstNight("edge-dst-spring", it.dateTimeBefore.toLocalDate().minusDays(1), -1L, "primavera") },
            autumn?.let { dstNight("edge-dst-autumn", it.dateTimeBefore.toLocalDate().minusDays(1), 1L, "otoño") },
        )
    }

    /** Noche de 22:00 a 06:00 hora de reloj, con [hourShift] horas de más o de menos en tiempo real. */
    private fun dstNight(
        id: String,
        date: LocalDate,
        hourShift: Long,
        season: String,
    ): WorkShift =
        demoShift(
            id = id,
            date = date,
            start = LocalTime.of(22, 0),
            zone = zone,
            durationSec = (8 + hourShift) * SECONDS_PER_HOUR,
            note = "Noche del cambio de hora de $season.",
        )

    /** Una jornada de 24 horas exactas: el máximo que acepta la validación. */
    private fun longDay(): List<WorkShift> =
        listOf(
            demoShift(
                id = "edge-24h",
                date = day(40),
                start = LocalTime.of(9, 0),
                zone = zone,
                durationSec = WorkRules.MAX_SHIFT_SECONDS,
                projectId = "edge-long-project",
                note = fitText(LONG_NOTE_SEED, WorkRules.SHIFT_NOTE_MAX_LENGTH),
            ),
        )

    /** Veinte pausas de cinco minutos seguidas, el máximo permitido. */
    private fun manyBreaks(): List<WorkShift> {
        val date = day(12)
        val begin = instantAt(date, LocalTime.of(8, 0), zone)
        val breaks =
            (0 until WorkRules.MAX_BREAKS).map { index ->
                val start = begin.plusSeconds((30 + index * 20L) * SECONDS_PER_MINUTE)
                WorkBreak(start, start.plusSeconds(5 * SECONDS_PER_MINUTE))
            }
        return listOf(
            demoShift("edge-many-breaks", date, LocalTime.of(8, 0), zone, 10 * SECONDS_PER_HOUR, breaks = breaks),
        )
    }

    /** Una jornada registrada en otra zona horaria: su día lógico es el de allí. */
    private fun otherZone(): List<WorkShift> =
        listOf(
            demoShift(
                id = "edge-tokyo",
                date = day(20),
                start = LocalTime.of(9, 0),
                zone = ZoneId.of("Asia/Tokyo"),
                durationSec = 9 * SECONDS_PER_HOUR,
                note = "Viaje de trabajo ✈️",
            ),
        )

    /** Una jornada en curso hoy con una pausa abierta, para ver el cronómetro detenido. */
    private fun inProgress(): List<WorkShift> {
        val begin = clock.instant().minusSeconds(3 * SECONDS_PER_HOUR)
        val pause = WorkBreak(begin.plusSeconds(2 * SECONDS_PER_HOUR), null)
        val start = begin.atZone(zone).toLocalTime()
        // Si la jornada empezaría antes de medianoche se deja sin pausa: así nunca cae fuera de su día.
        val sameDay = begin.atZone(zone).toLocalDate() == today
        return listOf(
            WorkShift(
                id = "edge-open",
                start = begin,
                zone = zone,
                end = null,
                breaks = if (sameDay) listOf(pause) else emptyList(),
                note = "Jornada en curso con la pausa abierta (${start.hour}h).",
            ),
        )
    }

    private fun projects(): List<WorkProject> =
        listOf(
            WorkProject(
                "edge-long-project",
                fitText(LONG_NAME_SEED, WorkRules.PROJECT_NAME_MAX_LENGTH),
                ProjectColor.PURPLE,
            ),
            WorkProject("edge-emoji-project", "🚀", ProjectColor.PINK),
            WorkProject("edge-grey-project", "Sin color definido", ProjectColor.GREY),
        )

    private fun focusSessions(): List<FocusSession> {
        val start = instantAt(day(2), LocalTime.of(23, 50), zone)
        return listOf(
            FocusSession(
                id = "edge-focus-midnight",
                kind = FocusKind.FOCUS,
                start = start,
                zone = zone,
                plannedSeconds = 50 * SECONDS_PER_MINUTE,
                end = start.plusSeconds(50 * SECONDS_PER_MINUTE),
                completed = true,
                title = fitText("Terminar el informe 📊 ", WorkRules.FOCUS_TITLE_MAX_LENGTH),
            ),
        )
    }
}

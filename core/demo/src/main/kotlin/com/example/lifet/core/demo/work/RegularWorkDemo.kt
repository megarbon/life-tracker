@file:Suppress("MagicNumber") // Probabilidades, horas y rangos literales: son los parámetros de los datos de ejemplo.

package com.example.lifet.core.demo.work

import com.example.lifet.core.model.work.FocusKind
import com.example.lifet.core.model.work.FocusSession
import com.example.lifet.core.model.work.ProjectColor
import com.example.lifet.core.model.work.WorkDataSet
import com.example.lifet.core.model.work.WorkDayNote
import com.example.lifet.core.model.work.WorkProject
import com.example.lifet.core.model.work.WorkShift
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.Random

private const val WEEKS_BACK = 26L
private const val HOLIDAY_WEEKS_AGO = 11L
private const val FORGOTTEN_WEEKS_AGO_FIRST = 14L
private const val FORGOTTEN_WEEKS_AGO_SECOND = 5L
private const val NIGHT_WEEKS_AGO = 8L
private const val MINUTES_STEP = 15
private const val SATURDAY_WORK_CHANCE = 12
private const val FOCUS_CHANCE = 55
private const val NOTE_CHANCE = 18

internal val DEMO_PROJECTS =
    listOf(
        WorkProject("demo-work-web", "Rediseño web", ProjectColor.BLUE),
        WorkProject("demo-work-app", "App móvil", ProjectColor.TEAL),
        WorkProject("demo-work-admin", "Administración", ProjectColor.AMBER),
    )

private val NOTES =
    listOf(
        "Reunión con el cliente por la mañana.",
        "Día tranquilo: revisión de pendientes.",
        "Entrega del sprint, salgo un poco más tarde.",
        "Formación interna sobre accesibilidad.",
        "Atascado con un fallo de sincronización; lo retomo mañana.",
    )

private val FOCUS_TITLES = listOf("Informe semanal", "Revisión de código", "Diseño de pantallas", "Correo y gestiones")

/**
 * Escenario REGULAR: unas 26 semanas de lunes a viernes con entrada entre las 8:00 y las 9:30, de 7 h 30 min a 9 h 30
 * min, una pausa para comer y variación de proyecto por día. Incluye una semana de vacaciones sin jornadas, **dos
 * olvidos de fichar la salida** (jornadas abiertas de hace semanas, para probar el cierre a posteriori), **una jornada
 * nocturna que cruza la medianoche** (un sábado), tres proyectos, sesiones de foco y notas del día. Todo el azar sale
 * de [random]; las fechas, de [clock]; nada queda por delante de "ahora".
 */
internal class RegularWorkDemo(
    private val clock: Clock,
    private val random: Random,
) {
    private val zone: ZoneId = clock.zone
    private val today: LocalDate = LocalDate.now(clock)
    private val thisMonday: LocalDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun build(): WorkDataSet {
        val shifts = ArrayList<WorkShift>()
        val focus = ArrayList<FocusSession>()
        val notes = ArrayList<WorkDayNote>()
        for (weeksAgo in WEEKS_BACK downTo 0L) {
            val monday = thisMonday.minusWeeks(weeksAgo)
            if (weeksAgo == HOLIDAY_WEEKS_AGO) continue
            for (offset in 0L..4L) {
                val date = monday.plusDays(offset)
                if (date > today) continue
                val forgotten = isForgotten(weeksAgo, offset)
                shifts += regularDay(date, forgotten)
                if (!forgotten && random.nextInt(100) < FOCUS_CHANCE) focus += focusSessions(date)
                if (random.nextInt(100) < NOTE_CHANCE) notes += WorkDayNote(date, NOTES[random.nextInt(NOTES.size)])
            }
        }
        shifts += nightShift()
        shifts += occasionalSaturdays()
        return WorkDataSet(shifts = shifts, projects = DEMO_PROJECTS, focusSessions = focus, notes = notes)
    }

    /** Los dos olvidos: un martes de hace 14 semanas y un jueves de hace 5. */
    private fun isForgotten(
        weeksAgo: Long,
        dayOffset: Long,
    ): Boolean =
        (weeksAgo == FORGOTTEN_WEEKS_AGO_FIRST && dayOffset == 1L) ||
            (weeksAgo == FORGOTTEN_WEEKS_AGO_SECOND && dayOffset == 3L)

    private fun regularDay(
        date: LocalDate,
        forgotten: Boolean,
    ): WorkShift {
        val start = LocalTime.of(8, 0).plusMinutes(random.nextInt(7).toLong() * MINUTES_STEP)
        // De 7 h 30 min a 9 h 30 min, en pasos de un cuarto de hora.
        val lengthSec = (450 + random.nextInt(9) * MINUTES_STEP) * SECONDS_PER_MINUTE
        val begin = instantAt(date, start, zone)
        val lunch = demoBreak(begin, afterSec = 4 * SECONDS_PER_HOUR, minutes = 30L + 15L * random.nextInt(3))
        return demoShift(
            id = "demo-work-$date",
            date = date,
            start = start,
            zone = zone,
            durationSec = if (forgotten) null else lengthSec,
            projectId = DEMO_PROJECTS[random.nextInt(DEMO_PROJECTS.size)].id,
            breaks = if (forgotten) emptyList() else listOf(lunch),
        )
    }

    /** Una jornada nocturna de sábado a domingo: empieza a las 22:00 y acaba a las 03:30, ya pasada la medianoche. */
    private fun nightShift(): WorkShift {
        val saturday = thisMonday.minusWeeks(NIGHT_WEEKS_AGO).plusDays(5)
        return demoShift(
            id = "demo-work-night",
            date = saturday,
            start = LocalTime.of(22, 0),
            zone = zone,
            durationSec = 5 * SECONDS_PER_HOUR + 30 * SECONDS_PER_MINUTE,
            projectId = DEMO_PROJECTS[1].id,
            note = "Despliegue nocturno de la versión 2.0.",
        )
    }

    /** Algún sábado suelto de trabajo extra (sin objetivo ese día: todo cuenta como horas a favor). */
    private fun occasionalSaturdays(): List<WorkShift> =
        (1L..WEEKS_BACK)
            .filter { it != NIGHT_WEEKS_AGO && it != HOLIDAY_WEEKS_AGO && random.nextInt(100) < SATURDAY_WORK_CHANCE }
            .map { weeksAgo ->
                val date = thisMonday.minusWeeks(weeksAgo).plusDays(5)
                demoShift(
                    id = "demo-work-sat-$date",
                    date = date,
                    start = LocalTime.of(10, 0),
                    zone = zone,
                    durationSec = (2 + random.nextInt(3)) * SECONDS_PER_HOUR,
                    projectId = DEMO_PROJECTS[random.nextInt(DEMO_PROJECTS.size)].id,
                )
            }

    private fun focusSessions(date: LocalDate): List<FocusSession> =
        (0 until 1 + random.nextInt(3)).map { index ->
            val start = instantAt(date, LocalTime.of(10, 0).plusMinutes(index * 45L), zone)
            FocusSession(
                id = "demo-work-focus-$date-$index",
                kind = FocusKind.FOCUS,
                start = start,
                zone = zone,
                plannedSeconds = 25 * SECONDS_PER_MINUTE,
                end = start.plusSeconds(25 * SECONDS_PER_MINUTE),
                completed = true,
                projectId = DEMO_PROJECTS[random.nextInt(DEMO_PROJECTS.size)].id,
                title = FOCUS_TITLES[random.nextInt(FOCUS_TITLES.size)],
            )
        }
}

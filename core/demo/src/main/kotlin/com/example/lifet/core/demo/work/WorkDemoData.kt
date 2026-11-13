@file:Suppress("MagicNumber") // Horas y duraciones literales: son datos de ejemplo, no reglas.

package com.example.lifet.core.demo.work

import com.example.lifet.core.demo.DemoScenario
import com.example.lifet.core.model.work.FocusKind
import com.example.lifet.core.model.work.FocusSession
import com.example.lifet.core.model.work.WorkBreak
import com.example.lifet.core.model.work.WorkDataSet
import com.example.lifet.core.model.work.WorkDayNote
import com.example.lifet.core.model.work.WorkShift
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.util.Random

/**
 * Datos de demo de Trabajo, deterministas: todo el azar sale de [random] y las fechas de [clock]. REGULAR vive en
 * [RegularWorkDemo] y EDGE_CASES en [EdgeCaseWorkDemo]. Nada de lo generado queda por delante de "ahora".
 */
internal class WorkDemoData(
    private val clock: Clock,
    private val random: Random,
) {
    fun build(scenario: DemoScenario): WorkDataSet =
        when (scenario) {
            DemoScenario.EMPTY -> WorkDataSet()
            DemoScenario.NEW_USER -> newUser()
            DemoScenario.REGULAR -> RegularWorkDemo(clock, random).build()
            DemoScenario.EDGE_CASES -> EdgeCaseWorkDemo(clock).build()
        }.withoutFuture(clock)

    /** Tres días de uso: dos jornadas completas con un proyecto, una sesión de foco y la jornada de hoy en curso. */
    private fun newUser(): WorkDataSet {
        val zone = clock.zone
        val today = LocalDate.now(clock)
        val project = DEMO_PROJECTS.first()
        val twoDaysAgo =
            demoShift("demo-new-1", today.minusDays(2), LocalTime.of(9, 0), zone, 8 * SECONDS_PER_HOUR, project.id)
        val yesterdayStart = instantAt(today.minusDays(1), LocalTime.of(8, 45), zone)
        val yesterday =
            WorkShift(
                id = "demo-new-2",
                start = yesterdayStart,
                zone = zone,
                end = yesterdayStart.plusSeconds(8 * SECONDS_PER_HOUR + 30 * SECONDS_PER_MINUTE),
                breaks =
                    listOf(
                        WorkBreak(
                            yesterdayStart.plusSeconds(4 * SECONDS_PER_HOUR),
                            yesterdayStart.plusSeconds(4 * SECONDS_PER_HOUR + 45 * SECONDS_PER_MINUTE),
                        ),
                    ),
                projectId = project.id,
            )
        val now = clock.instant()
        val openStart = now.minusSeconds(SECONDS_PER_HOUR)
        val focusStart = instantAt(today.minusDays(1), LocalTime.of(10, 0), zone)
        return WorkDataSet(
            shifts = listOf(twoDaysAgo, yesterday, WorkShift("demo-new-3", openStart, zone, projectId = project.id)),
            projects = listOf(project),
            focusSessions =
                listOf(
                    FocusSession(
                        id = "demo-new-focus",
                        kind = FocusKind.FOCUS,
                        start = focusStart,
                        zone = zone,
                        plannedSeconds = 25 * SECONDS_PER_MINUTE,
                        end = focusStart.plusSeconds(25 * SECONDS_PER_MINUTE),
                        completed = true,
                        projectId = project.id,
                        title = "Primer informe",
                    ),
                ),
            notes = listOf(WorkDayNote(today.minusDays(1), "Primer día usando la app para fichar.")),
        )
    }
}

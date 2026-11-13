@file:Suppress("TooManyFunctions") // Un par de mapeos entity <-> dominio por cada tabla.

package com.example.lifet.core.data.work

import com.example.lifet.core.database.work.WorkBreakEntity
import com.example.lifet.core.database.work.WorkDayNoteEntity
import com.example.lifet.core.database.work.WorkFocusEntity
import com.example.lifet.core.database.work.WorkProjectEntity
import com.example.lifet.core.database.work.WorkSettingsEntity
import com.example.lifet.core.database.work.WorkShiftEntity
import com.example.lifet.core.model.work.FocusSession
import com.example.lifet.core.model.work.WorkBreak
import com.example.lifet.core.model.work.WorkDayNote
import com.example.lifet.core.model.work.WorkProject
import com.example.lifet.core.model.work.WorkSettings
import com.example.lifet.core.model.work.WorkShift
import java.time.DayOfWeek

/** Jornada sin pausas: las pausas se leen de otra tabla y se unen en [toDomain]. */
internal fun WorkShiftEntity.toDomain(breaks: List<WorkBreakEntity>): WorkShift =
    WorkShift(
        id = id,
        start = start,
        zone = zoneId,
        end = end,
        breaks = breaks.sortedBy { it.start }.map { WorkBreak(it.start, it.end) },
        projectId = projectId,
        note = note,
    )

internal fun WorkShift.toEntity(): WorkShiftEntity =
    WorkShiftEntity(id = id, start = start, zoneId = zone, end = end, projectId = projectId, note = note)

internal fun WorkShift.toBreakEntities(): List<WorkBreakEntity> =
    breaks.map { WorkBreakEntity(shiftId = id, start = it.start, end = it.end) }

internal fun WorkProjectEntity.toDomain(): WorkProject = WorkProject(id = id, name = name, color = color)

internal fun WorkProject.toEntity(): WorkProjectEntity = WorkProjectEntity(id = id, name = name, color = color)

internal fun WorkFocusEntity.toDomain(): FocusSession =
    FocusSession(
        id = id,
        kind = kind,
        start = start,
        zone = zoneId,
        plannedSeconds = plannedSeconds,
        end = end,
        completed = completed,
        projectId = projectId,
        title = title,
    )

internal fun FocusSession.toEntity(): WorkFocusEntity =
    WorkFocusEntity(
        id = id,
        kind = kind,
        start = start,
        zoneId = zone,
        plannedSeconds = plannedSeconds,
        end = end,
        completed = completed,
        projectId = projectId,
        title = title,
    )

internal fun WorkDayNoteEntity.toDomain(): WorkDayNote = WorkDayNote(date = date, text = text)

internal fun WorkDayNote.toEntity(): WorkDayNoteEntity = WorkDayNoteEntity(date = date, text = text)

internal fun WorkSettingsEntity.toDomain(): WorkSettings =
    WorkSettings(weeklyTargetMinutes = weeklyTargetMinutes, workingDays = workingDaysMask.toWorkingDays())

internal fun WorkSettings.toEntity(): WorkSettingsEntity =
    WorkSettingsEntity(weeklyTargetMinutes = weeklyTargetMinutes, workingDaysMask = workingDays.toMask())

/** Un bit por día: lunes = bit 0 … domingo = bit 6. */
internal fun Set<DayOfWeek>.toMask(): Int = fold(0) { mask, day -> mask or (1 shl (day.value - 1)) }

internal fun Int.toWorkingDays(): Set<DayOfWeek> =
    DayOfWeek.entries.filterTo(LinkedHashSet()) { day -> this and (1 shl (day.value - 1)) != 0 }

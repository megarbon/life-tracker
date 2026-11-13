package com.example.lifet.core.database.work

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.lifet.core.model.work.FocusKind
import com.example.lifet.core.model.work.ProjectColor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// Todo el tiempo va como Instant (UTC) + ZoneId del usuario en ese momento; las duraciones, en segundos enteros.

/**
 * Una jornada. [end] nulo = abierta. [projectId] no tiene clave foránea a propósito: borrar un proyecto no toca las
 * jornadas, de modo que "Deshacer" lo restaura con todas sus horas.
 */
@Entity(tableName = "work_shifts", indices = [Index("start")])
public data class WorkShiftEntity(
    @PrimaryKey public val id: String,
    public val start: Instant,
    public val zoneId: ZoneId,
    public val end: Instant?,
    public val projectId: String?,
    public val note: String,
)

/** Pausa de una jornada. Sin [end] está en curso. Cae en cascada con su jornada. */
@Entity(
    tableName = "work_breaks",
    foreignKeys =
        [
            ForeignKey(
                entity = WorkShiftEntity::class,
                parentColumns = ["id"],
                childColumns = ["shiftId"],
                onDelete = ForeignKey.CASCADE,
            ),
        ],
    indices = [Index("shiftId")],
)
public data class WorkBreakEntity(
    @PrimaryKey(autoGenerate = true) public val id: Long = 0,
    public val shiftId: String,
    public val start: Instant,
    public val end: Instant?,
)

@Entity(tableName = "work_projects")
public data class WorkProjectEntity(
    @PrimaryKey public val id: String,
    public val name: String,
    public val color: ProjectColor,
)

/** Intervalo del temporizador de foco. Se guarda su inicio, nunca "segundos que quedan". */
@Entity(tableName = "work_focus_sessions", indices = [Index("start")])
public data class WorkFocusEntity(
    @PrimaryKey public val id: String,
    public val kind: FocusKind,
    public val start: Instant,
    public val zoneId: ZoneId,
    public val plannedSeconds: Long,
    public val end: Instant?,
    public val completed: Boolean,
    public val projectId: String?,
    public val title: String,
)

/** Una nota por día: guardar otra el mismo día la sustituye. */
@Entity(tableName = "work_day_notes")
public data class WorkDayNoteEntity(
    @PrimaryKey public val date: LocalDate,
    public val text: String,
)

/** Ajustes: una única fila (`id = 1`). [workingDaysMask] tiene un bit por día (lunes = bit 0 … domingo = bit 6). */
@Entity(tableName = "work_settings")
public data class WorkSettingsEntity(
    @PrimaryKey public val id: Int = SINGLETON_ID,
    public val weeklyTargetMinutes: Int,
    public val workingDaysMask: Int,
) {
    public companion object {
        public const val SINGLETON_ID: Int = 1
    }
}

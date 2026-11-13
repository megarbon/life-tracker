package com.example.lifet.core.database.work

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Acceso a las tablas de Trabajo. Es `abstract class` porque guardar una jornada escribe en dos tablas y va en
 * `@Transaction`. Las pausas dependen de su jornada con borrado en cascada.
 */
@Dao
@Suppress("TooManyFunctions") // Un DAO por área: seis tablas con alta, edición y borrado.
public abstract class WorkDao {
    @Query("SELECT * FROM work_shifts ORDER BY start DESC")
    public abstract fun observeShifts(): Flow<List<WorkShiftEntity>>

    @Query("SELECT * FROM work_breaks ORDER BY shiftId, start")
    public abstract fun observeBreaks(): Flow<List<WorkBreakEntity>>

    @Query("SELECT * FROM work_projects ORDER BY name COLLATE NOCASE")
    public abstract fun observeProjects(): Flow<List<WorkProjectEntity>>

    @Query("SELECT * FROM work_focus_sessions ORDER BY start DESC")
    public abstract fun observeFocusSessions(): Flow<List<WorkFocusEntity>>

    @Query("SELECT * FROM work_day_notes ORDER BY date")
    public abstract fun observeNotes(): Flow<List<WorkDayNoteEntity>>

    @Query("SELECT * FROM work_settings WHERE id = 1")
    public abstract fun observeSettings(): Flow<WorkSettingsEntity?>

    @Upsert
    public abstract suspend fun upsertShift(entity: WorkShiftEntity)

    @Insert
    public abstract suspend fun insertBreaks(entities: List<WorkBreakEntity>)

    @Query("DELETE FROM work_breaks WHERE shiftId = :shiftId")
    public abstract suspend fun deleteBreaksOf(shiftId: String)

    /** Guarda la jornada y sustituye todas sus pausas, de modo que editar nunca deja pausas sueltas. */
    @Transaction
    public open suspend fun saveShift(
        shift: WorkShiftEntity,
        breaks: List<WorkBreakEntity>,
    ) {
        upsertShift(shift)
        deleteBreaksOf(shift.id)
        insertBreaks(breaks)
    }

    @Query("DELETE FROM work_shifts WHERE id = :id")
    public abstract suspend fun deleteShift(id: String)

    @Upsert
    public abstract suspend fun upsertProject(entity: WorkProjectEntity)

    @Query("DELETE FROM work_projects WHERE id = :id")
    public abstract suspend fun deleteProject(id: String)

    @Upsert
    public abstract suspend fun upsertFocusSession(entity: WorkFocusEntity)

    @Query("DELETE FROM work_focus_sessions WHERE id = :id")
    public abstract suspend fun deleteFocusSession(id: String)

    @Upsert
    public abstract suspend fun upsertNote(entity: WorkDayNoteEntity)

    @Query("DELETE FROM work_day_notes WHERE date = :date")
    public abstract suspend fun deleteNote(date: LocalDate)

    @Upsert
    public abstract suspend fun upsertSettings(entity: WorkSettingsEntity)

    @Query("DELETE FROM work_shifts")
    public abstract suspend fun deleteAllShifts()

    @Query("DELETE FROM work_projects")
    public abstract suspend fun deleteAllProjects()

    @Query("DELETE FROM work_focus_sessions")
    public abstract suspend fun deleteAllFocusSessions()

    @Query("DELETE FROM work_day_notes")
    public abstract suspend fun deleteAllNotes()

    @Query("DELETE FROM work_settings")
    public abstract suspend fun deleteAllSettings()

    @Insert
    public abstract suspend fun insertShifts(entities: List<WorkShiftEntity>)

    @Insert
    public abstract suspend fun insertProjects(entities: List<WorkProjectEntity>)

    @Insert
    public abstract suspend fun insertFocusSessions(entities: List<WorkFocusEntity>)

    @Insert
    public abstract suspend fun insertNotes(entities: List<WorkDayNoteEntity>)

    /** Sustituye todo el contenido de Trabajo (datos de demo, restaurar copia). Las pausas caen en cascada. */
    @Transaction
    @Suppress("LongParameterList") // Una lista por tabla: es el contenido completo del área.
    public open suspend fun replaceAll(
        shifts: List<WorkShiftEntity>,
        breaks: List<WorkBreakEntity>,
        projects: List<WorkProjectEntity>,
        focusSessions: List<WorkFocusEntity>,
        notes: List<WorkDayNoteEntity>,
        settings: WorkSettingsEntity,
    ) {
        deleteAllShifts()
        deleteAllProjects()
        deleteAllFocusSessions()
        deleteAllNotes()
        deleteAllSettings()
        insertShifts(shifts)
        insertBreaks(breaks)
        insertProjects(projects)
        insertFocusSessions(focusSessions)
        insertNotes(notes)
        upsertSettings(settings)
    }
}

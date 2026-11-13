package com.example.lifet.core.data.work

import com.example.lifet.core.common.AppResult
import com.example.lifet.core.model.work.FocusSession
import com.example.lifet.core.model.work.WorkDataSet
import com.example.lifet.core.model.work.WorkDayNote
import com.example.lifet.core.model.work.WorkProject
import com.example.lifet.core.model.work.WorkSettings
import com.example.lifet.core.model.work.WorkShift
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Fuente única de verdad de Trabajo (Room). Las lecturas son reactivas; las escrituras devuelven [AppResult] y nunca
 * lanzan, salvo cancelación. La validación de reglas (solapes, pausas) vive en `ShiftValidator`; el repositorio guarda
 * lo que recibe.
 */
@Suppress("TooManyFunctions") // Un repositorio por área: seis tipos de dato con alta, edición y borrado.
public interface WorkRepository {
    /** Todo el contenido de Trabajo; vuelve a emitir con cada cambio. */
    public fun observeData(): Flow<WorkDataSet>

    /** Inserta o actualiza la jornada con todas sus pausas. */
    public suspend fun saveShift(shift: WorkShift): AppResult<Unit>

    public suspend fun deleteShift(id: String): AppResult<Unit>

    public suspend fun saveProject(project: WorkProject): AppResult<Unit>

    /** Borra el proyecto; las jornadas y sesiones que lo usaban se conservan (pasan a "sin proyecto"). */
    public suspend fun deleteProject(id: String): AppResult<Unit>

    public suspend fun saveFocusSession(session: FocusSession): AppResult<Unit>

    public suspend fun deleteFocusSession(id: String): AppResult<Unit>

    /** Una nota por día: guardar otra el mismo día la sustituye. */
    public suspend fun saveNote(note: WorkDayNote): AppResult<Unit>

    public suspend fun deleteNote(date: LocalDate): AppResult<Unit>

    public suspend fun saveSettings(settings: WorkSettings): AppResult<Unit>

    /** Sustituye todos los datos de Trabajo (demo, restaurar copia). */
    public suspend fun replaceAll(data: WorkDataSet): AppResult<Unit>
}

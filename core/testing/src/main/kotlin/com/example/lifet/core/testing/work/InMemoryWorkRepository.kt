package com.example.lifet.core.testing.work

import com.example.lifet.core.common.AppError
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.data.work.WorkRepository
import com.example.lifet.core.model.work.FocusSession
import com.example.lifet.core.model.work.WorkDataSet
import com.example.lifet.core.model.work.WorkDayNote
import com.example.lifet.core.model.work.WorkProject
import com.example.lifet.core.model.work.WorkSettings
import com.example.lifet.core.model.work.WorkShift
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import java.time.LocalDate

/**
 * Repositorio de Trabajo en memoria con las mismas reglas que el de Room (una nota por día; borrar un proyecto
 * conserva sus jornadas). [failObserve] hace que la lectura falle, para probar el estado de error, y [failWrites] que
 * las escrituras devuelvan un error de almacenamiento.
 */
@Suppress("TooManyFunctions") // Implementa WorkRepository.
public class InMemoryWorkRepository(
    initial: WorkDataSet = WorkDataSet(),
) : WorkRepository {
    private val state = MutableStateFlow(initial)

    public var failObserve: Boolean = false
    public var failWrites: Boolean = false

    public val current: WorkDataSet get() = state.value

    // Se evalúa al recolectar, así un test puede activar el fallo después de crear el ViewModel.
    override fun observeData(): Flow<WorkDataSet> =
        flow {
            check(!failObserve) { "Fallo simulado" }
            emitAll(state)
        }

    override suspend fun saveShift(shift: WorkShift): AppResult<Unit> =
        edit { data -> data.copy(shifts = data.shifts.replacing(shift) { it.id == shift.id }) }

    override suspend fun deleteShift(id: String): AppResult<Unit> =
        edit { data -> data.copy(shifts = data.shifts.filterNot { it.id == id }) }

    override suspend fun saveProject(project: WorkProject): AppResult<Unit> =
        edit { data -> data.copy(projects = data.projects.replacing(project) { it.id == project.id }) }

    override suspend fun deleteProject(id: String): AppResult<Unit> =
        edit { data -> data.copy(projects = data.projects.filterNot { it.id == id }) }

    override suspend fun saveFocusSession(session: FocusSession): AppResult<Unit> =
        edit { data -> data.copy(focusSessions = data.focusSessions.replacing(session) { it.id == session.id }) }

    override suspend fun deleteFocusSession(id: String): AppResult<Unit> =
        edit { data -> data.copy(focusSessions = data.focusSessions.filterNot { it.id == id }) }

    override suspend fun saveNote(note: WorkDayNote): AppResult<Unit> =
        edit { data -> data.copy(notes = data.notes.replacing(note) { it.date == note.date }) }

    override suspend fun deleteNote(date: LocalDate): AppResult<Unit> =
        edit { data -> data.copy(notes = data.notes.filterNot { it.date == date }) }

    override suspend fun saveSettings(settings: WorkSettings): AppResult<Unit> =
        edit { data -> data.copy(settings = settings) }

    override suspend fun replaceAll(data: WorkDataSet): AppResult<Unit> = edit { data }

    private fun edit(change: (WorkDataSet) -> WorkDataSet): AppResult<Unit> =
        if (failWrites) {
            AppResult.Failure(AppError.Storage)
        } else {
            state.update(change)
            AppResult.Success(Unit)
        }

    private inline fun <T> List<T>.replacing(
        item: T,
        matches: (T) -> Boolean,
    ): List<T> = if (any(matches)) map { if (matches(it)) item else it } else this + item
}

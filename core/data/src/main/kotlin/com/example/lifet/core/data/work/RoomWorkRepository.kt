package com.example.lifet.core.data.work

import android.database.SQLException
import com.example.lifet.core.common.AppError
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.common.IoDispatcher
import com.example.lifet.core.common.runCatchingApp
import com.example.lifet.core.database.work.WorkBreakEntity
import com.example.lifet.core.database.work.WorkDao
import com.example.lifet.core.database.work.WorkShiftEntity
import com.example.lifet.core.model.work.FocusSession
import com.example.lifet.core.model.work.WorkDataSet
import com.example.lifet.core.model.work.WorkDayNote
import com.example.lifet.core.model.work.WorkProject
import com.example.lifet.core.model.work.WorkSettings
import com.example.lifet.core.model.work.WorkShift
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

@Suppress("TooManyFunctions") // Implementa WorkRepository.
internal class RoomWorkRepository
    @Inject
    constructor(
        private val dao: WorkDao,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : WorkRepository {
        override fun observeData(): Flow<WorkDataSet> =
            combine(
                combine(dao.observeShifts(), dao.observeBreaks(), ::Pair),
                dao.observeProjects(),
                dao.observeFocusSessions(),
                dao.observeNotes(),
                dao.observeSettings(),
            ) { (shifts, breaks), projects, focus, notes, settings ->
                WorkDataSet(
                    shifts = shifts.withBreaks(breaks),
                    projects = projects.map { it.toDomain() },
                    focusSessions = focus.map { it.toDomain() },
                    notes = notes.map { it.toDomain() },
                    settings = settings?.toDomain() ?: WorkSettings(),
                )
            }.flowOn(ioDispatcher)

        override suspend fun saveShift(shift: WorkShift): AppResult<Unit> =
            write { dao.saveShift(shift.toEntity(), shift.toBreakEntities()) }

        override suspend fun deleteShift(id: String): AppResult<Unit> = write { dao.deleteShift(id) }

        override suspend fun saveProject(project: WorkProject): AppResult<Unit> =
            write { dao.upsertProject(project.toEntity()) }

        override suspend fun deleteProject(id: String): AppResult<Unit> = write { dao.deleteProject(id) }

        override suspend fun saveFocusSession(session: FocusSession): AppResult<Unit> =
            write { dao.upsertFocusSession(session.toEntity()) }

        override suspend fun deleteFocusSession(id: String): AppResult<Unit> = write { dao.deleteFocusSession(id) }

        override suspend fun saveNote(note: WorkDayNote): AppResult<Unit> = write { dao.upsertNote(note.toEntity()) }

        override suspend fun deleteNote(date: LocalDate): AppResult<Unit> = write { dao.deleteNote(date) }

        override suspend fun saveSettings(settings: WorkSettings): AppResult<Unit> =
            write { dao.upsertSettings(settings.toEntity()) }

        override suspend fun replaceAll(data: WorkDataSet): AppResult<Unit> =
            write {
                dao.replaceAll(
                    shifts = data.shifts.map { it.toEntity() },
                    breaks = data.shifts.flatMap { it.toBreakEntities() },
                    projects = data.projects.map { it.toEntity() },
                    focusSessions = data.focusSessions.map { it.toEntity() },
                    notes = data.notes.map { it.toEntity() },
                    settings = data.settings.toEntity(),
                )
            }

        private fun List<WorkShiftEntity>.withBreaks(breaks: List<WorkBreakEntity>): List<WorkShift> {
            val byShift = breaks.groupBy { it.shiftId }
            return map { it.toDomain(byShift[it.id].orEmpty()) }
        }

        private suspend fun write(block: suspend () -> Unit): AppResult<Unit> =
            withContext(ioDispatcher) {
                runCatchingApp(onError = { if (it is SQLException) AppError.Storage else AppError.Unknown }) { block() }
            }
    }

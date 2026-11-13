package com.example.lifet.core.domain.work

import com.example.lifet.core.common.AppError
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.data.work.WorkRepository
import com.example.lifet.core.model.work.ProjectColor
import com.example.lifet.core.model.work.WorkClockIssue
import com.example.lifet.core.model.work.WorkDayNote
import com.example.lifet.core.model.work.WorkProject
import com.example.lifet.core.model.work.WorkRules
import com.example.lifet.core.model.work.WorkSettings
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/**
 * Proyectos, nota del día y ajustes de Trabajo. Valida en el borde: un nombre vacío o demasiado largo, un objetivo
 * fuera de rango o una nota excesiva se rechazan con [AppError.Validation] sin tocar la base de datos.
 */
@Suppress("TooManyFunctions") // Alta, edición y borrado de tres tipos de dato.
public class ManageWorkData
    @Inject
    constructor(
        private val repository: WorkRepository,
    ) {
        /** Crea el proyecto, o lo actualiza si [id] ya existe. El nombre se recorta de espacios. */
        public suspend fun saveProject(
            name: String,
            color: ProjectColor,
            id: String? = null,
        ): AppResult<WorkProject> {
            val clean = name.trim()
            val existingCount =
                repository
                    .observeData()
                    .first()
                    .projects
                    .count { it.id != id }
            val issue =
                when {
                    clean.isEmpty() -> WorkClockIssue.NAME_MISSING
                    clean.length > WorkRules.PROJECT_NAME_MAX_LENGTH -> WorkClockIssue.NAME_TOO_LONG
                    existingCount >= WorkRules.MAX_PROJECTS -> WorkClockIssue.TOO_MANY_PROJECTS
                    else -> null
                }
            if (issue != null) return AppResult.Failure(AppError.Validation(issue.name))
            val project = WorkProject(id ?: UUID.randomUUID().toString(), clean, color)
            return when (val saved = repository.saveProject(project)) {
                is AppResult.Success -> AppResult.Success(project)
                is AppResult.Failure -> saved
            }
        }

        public suspend fun deleteProject(id: String): AppResult<Unit> = repository.deleteProject(id)

        /** Restaura un proyecto borrado ("Deshacer"); sus jornadas nunca se tocaron, así que recupera todas sus horas. */
        public suspend fun restoreProject(project: WorkProject): AppResult<Unit> = repository.saveProject(project)

        /** Guarda la nota del día; un texto vacío la borra. */
        public suspend fun saveNote(
            date: LocalDate,
            text: String,
        ): AppResult<Unit> =
            when {
                text.length > WorkRules.DAY_NOTE_MAX_LENGTH -> {
                    AppResult.Failure(AppError.Validation(WorkClockIssue.NOTE_TOO_LONG.name))
                }

                text.isBlank() -> {
                    repository.deleteNote(date)
                }

                else -> {
                    repository.saveNote(WorkDayNote(date, text.trim()))
                }
            }

        public suspend fun restoreNote(note: WorkDayNote): AppResult<Unit> = repository.saveNote(note)

        public suspend fun saveSettings(settings: WorkSettings): AppResult<Unit> {
            val validTarget = settings.weeklyTargetMinutes in 1..WorkRules.MAX_WEEKLY_TARGET_MINUTES
            val issue =
                when {
                    settings.workingDays.isEmpty() -> WorkClockIssue.NO_WORKING_DAYS
                    !validTarget -> WorkClockIssue.TARGET_INVALID
                    else -> null
                }
            return if (issue == null) {
                repository.saveSettings(settings)
            } else {
                AppResult.Failure(AppError.Validation(issue.name))
            }
        }
    }

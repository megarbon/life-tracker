package com.example.lifet.feature.work

import com.example.lifet.core.common.AppError
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.domain.work.ManageFocusTimer
import com.example.lifet.core.domain.work.ManageWorkClock
import com.example.lifet.core.domain.work.ManageWorkData
import com.example.lifet.core.model.work.WorkClockIssue
import javax.inject.Inject

/**
 * Convierte los formularios de la UI en llamadas al dominio y deshace borrados. Separado del ViewModel para que éste
 * sólo enrute estado y eventos. Todas las reglas (solapes, pausas, límites) las aplican los casos de uso: un
 * formulario inválido devuelve [AppError.Validation] sin tocar la base de datos.
 */
internal class WorkWriter
    @Inject
    constructor(
        private val clock: ManageWorkClock,
        private val focus: ManageFocusTimer,
        private val data: ManageWorkData,
    ) {
        suspend fun save(form: WorkForm): AppResult<Unit> =
            when (form) {
                is WorkForm.Shift -> clock.saveShift(form.draft.toShift(form.existing?.zone ?: form.zone))
                is WorkForm.Close -> close(form)
                is WorkForm.Project -> saveProject(form.draft)
                is WorkForm.Note -> data.saveNote(form.date, form.text)
                is WorkForm.Settings -> saveSettings(form.draft)
            }

        suspend fun delete(item: WorkItem): AppResult<Unit> =
            when (item) {
                is WorkItem.Shift -> clock.deleteShift(item.value.id)
                is WorkItem.Project -> data.deleteProject(item.value.id)
                is WorkItem.Focus -> focus.delete(item.value.id)
                is WorkItem.Note -> data.saveNote(item.value.date, "")
            }

        suspend fun restore(item: WorkItem): AppResult<Unit> =
            when (item) {
                is WorkItem.Shift -> clock.restoreShift(item.value)
                is WorkItem.Project -> data.restoreProject(item.value)
                is WorkItem.Focus -> focus.restore(item.value)
                is WorkItem.Note -> data.restoreNote(item.value)
            }

        private suspend fun close(form: WorkForm.Close): AppResult<Unit> =
            when (val result = clock.closeAt(form.shift.id, instantAt(form.exitDate, form.exitMinute, form.zone))) {
                is AppResult.Success -> AppResult.Success(Unit)
                is AppResult.Failure -> result
            }

        private suspend fun saveProject(draft: ProjectDraft): AppResult<Unit> =
            when (val result = data.saveProject(draft.name, draft.color, draft.id)) {
                is AppResult.Success -> AppResult.Success(Unit)
                is AppResult.Failure -> result
            }

        private suspend fun saveSettings(draft: SettingsDraft): AppResult<Unit> {
            val settings = draft.toSettings()
            return if (settings == null) {
                AppResult.Failure(AppError.Validation(WorkClockIssue.TARGET_INVALID.name))
            } else {
                data.saveSettings(settings)
            }
        }
    }

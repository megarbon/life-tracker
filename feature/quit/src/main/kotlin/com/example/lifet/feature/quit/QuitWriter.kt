package com.example.lifet.feature.quit

import com.example.lifet.core.common.AppError
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.data.quit.QuitRepository
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * Valida los formularios de la UI y los guarda. Separado del ViewModel para que éste sólo enrute estado y eventos.
 * Un formulario inválido devuelve [AppError.Validation] sin tocar la base de datos.
 */
internal class QuitWriter
    @Inject
    constructor(
        private val repository: QuitRepository,
        private val clock: Clock,
    ) {
        suspend fun save(form: QuitForm): AppResult<Unit> =
            when (form) {
                is QuitForm.Habit -> saveHabit(form)
                is QuitForm.Craving -> saveCraving(form)
                is QuitForm.Consumption -> saveConsumption(form)
            }

        suspend fun delete(item: QuitItem): AppResult<Unit> =
            when (item) {
                is QuitItem.Habit -> repository.deleteHabit(item.habit.id)
                is QuitItem.Craving -> repository.deleteCraving(item.value.id)
                is QuitItem.Consumption -> repository.deleteConsumption(item.value.id)
            }

        suspend fun restore(item: QuitItem): AppResult<Unit> =
            when (item) {
                is QuitItem.Habit -> repository.restoreHabit(item.habit, item.cravings, item.consumptions)
                is QuitItem.Craving -> repository.saveCraving(item.value)
                is QuitItem.Consumption -> repository.saveConsumption(item.value)
            }

        private suspend fun saveHabit(form: QuitForm.Habit): AppResult<Unit> {
            val zone = form.existing?.zone ?: clock.zone
            return when (val result = form.draft.validate(zone, clock.instant())) {
                is DraftResult.Invalid -> invalid(result)
                is DraftResult.Valid -> repository.saveHabit(result.value)
            }
        }

        private suspend fun saveCraving(form: QuitForm.Craving): AppResult<Unit> {
            val zone = form.existing?.zone ?: clock.zone
            val start = habitStart(form.draft.habitId) ?: return AppResult.Failure(AppError.NotFound)
            return when (val result = form.draft.validate(zone, clock.instant(), start)) {
                is DraftResult.Invalid -> invalid(result)
                is DraftResult.Valid -> repository.saveCraving(result.value)
            }
        }

        private suspend fun saveConsumption(form: QuitForm.Consumption): AppResult<Unit> {
            val zone = form.existing?.zone ?: clock.zone
            val start = habitStart(form.draft.habitId) ?: return AppResult.Failure(AppError.NotFound)
            return when (val result = form.draft.validate(zone, clock.instant(), start)) {
                is DraftResult.Invalid -> invalid(result)
                is DraftResult.Valid -> repository.saveConsumption(result.value)
            }
        }

        /** El inicio del hábito tal como está ahora en la base de datos: un registro no puede ser anterior a él. */
        private suspend fun habitStart(habitId: String): Instant? =
            repository
                .observeData()
                .first()
                .habits
                .firstOrNull { it.id == habitId }
                ?.startedAt

        private fun invalid(result: DraftResult.Invalid): AppResult<Unit> =
            AppResult.Failure(AppError.Validation(result.issues.joinToString { it.name }))
    }

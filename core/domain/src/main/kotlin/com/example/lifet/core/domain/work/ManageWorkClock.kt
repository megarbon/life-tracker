package com.example.lifet.core.domain.work

import com.example.lifet.core.common.AppError
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.data.work.WorkRepository
import com.example.lifet.core.model.work.ShiftValidator
import com.example.lifet.core.model.work.WorkCalculator
import com.example.lifet.core.model.work.WorkClockIssue
import com.example.lifet.core.model.work.WorkShift
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Acciones de fichaje: entrada, pausa, reanudar, salida, cerrar a posteriori una jornada olvidada y guardar o borrar
 * una jornada a mano. Toda jornada que se guarda pasa por [ShiftValidator]: una pausa solapada, una salida anterior a
 * la entrada o una jornada que pisa a otra se rechazan con [AppError.Validation] sin tocar la base de datos.
 *
 * Es un singleton y cada acción mantiene [lock] durante todo el ciclo leer, validar y escribir: dos toques seguidos en
 * "Entrada" no pueden ver los dos "no hay jornada" y crear dos jornadas abiertas. El cerrojo no es reentrante, así que
 * sólo lo toman las funciones públicas y las privadas asumen que ya lo tienen.
 *
 * "Ahora" sale del [Clock] inyectado; el instante se guarda con la zona del reloj en ese momento.
 */
@Singleton
@Suppress("TooManyFunctions") // Una función por acción de fichaje.
public class ManageWorkClock
    @Inject
    constructor(
        private val repository: WorkRepository,
        private val clock: Clock,
    ) {
        private val lock = Mutex()

        /** Ficha la entrada ahora. Falla si ya hay una jornada en curso. */
        public suspend fun clockIn(projectId: String? = null): AppResult<WorkShift> =
            lock.withLock {
                val now = clock.instant()
                val data = repository.observeData().first()
                if (WorkCalculator(data, now, clock.zone).currentShift() != null) {
                    invalid(WorkClockIssue.ALREADY_CLOCKED_IN)
                } else {
                    val shift = WorkShift(UUID.randomUUID().toString(), now, clock.zone, projectId = projectId)
                    save(shift, data.shifts, now).thenReturn(shift)
                }
            }

        /** Empieza una pausa en la jornada en curso. */
        public suspend fun startBreak(): AppResult<WorkShift> =
            changeCurrent(onMissing = WorkClockIssue.NOT_CLOCKED_IN) { shift, now ->
                if (shift.isOnBreak) null else shift.withBreakStarted(now)
            }.orIssue(WorkClockIssue.ALREADY_ON_BREAK)

        /** Cierra la pausa en curso. */
        public suspend fun endBreak(): AppResult<WorkShift> =
            changeCurrent(onMissing = WorkClockIssue.NOT_CLOCKED_IN) { shift, now ->
                if (shift.isOnBreak) shift.withBreakEnded(now) else null
            }.orIssue(WorkClockIssue.NOT_ON_BREAK)

        /** Ficha la salida ahora (y cierra la pausa abierta, si la hay). */
        public suspend fun clockOut(): AppResult<WorkShift> =
            changeCurrent(onMissing = WorkClockIssue.NOT_CLOCKED_IN) { shift, now -> shift.closedAt(now) }
                .orIssue(WorkClockIssue.NOT_CLOCKED_IN)

        /**
         * Cierra a posteriori la jornada olvidada [shiftId] indicando la hora real de salida [end]. Se rechaza si la
         * salida es anterior a la entrada, está en el futuro, supera las 24 h o pisa otra jornada.
         */
        public suspend fun closeAt(
            shiftId: String,
            end: Instant,
        ): AppResult<WorkShift> =
            lock.withLock {
                val now = clock.instant()
                val data = repository.observeData().first()
                val shift = data.shifts.firstOrNull { it.id == shiftId && it.isOpen }
                if (shift == null) {
                    invalid(WorkClockIssue.SHIFT_NOT_FOUND)
                } else {
                    val closed = shift.closedAt(end)
                    save(closed, data.shifts, now).thenReturn(closed)
                }
            }

        /** Guarda una jornada creada o editada a mano (alta con entrada y salida, o corrección de una existente). */
        public suspend fun saveShift(shift: WorkShift): AppResult<Unit> =
            lock.withLock { save(shift, repository.observeData().first().shifts, clock.instant()) }

        public suspend fun deleteShift(id: String): AppResult<Unit> = lock.withLock { repository.deleteShift(id) }

        /**
         * Restaura una jornada borrada ("Deshacer"). Se vuelve a validar: si mientras tanto se creó otra que la pisa,
         * se rechaza en lugar de contar las mismas horas dos veces.
         */
        public suspend fun restoreShift(shift: WorkShift): AppResult<Unit> =
            lock.withLock { save(shift, repository.observeData().first().shifts, clock.instant()) }

        /** Valida y guarda; el id de la propia jornada se ignora en la comprobación de solapes. */
        private suspend fun save(
            shift: WorkShift,
            existing: List<WorkShift>,
            now: Instant,
        ): AppResult<Unit> {
            val issues = ShiftValidator.validate(shift, existing, now)
            return if (issues.isEmpty()) {
                repository.saveShift(shift)
            } else {
                AppResult.Failure(AppError.Validation(issues.joinToString { it.name }))
            }
        }

        /**
         * Aplica [change] a la jornada en curso. Si [change] devuelve `null` no hay nada que hacer y la acción se
         * rechaza (el motivo lo pone quien llama con [orIssue]).
         */
        private suspend fun changeCurrent(
            onMissing: WorkClockIssue,
            change: (WorkShift, Instant) -> WorkShift?,
        ): AppResult<WorkShift?> =
            lock.withLock {
                val now = clock.instant()
                val data = repository.observeData().first()
                val current = WorkCalculator(data, now, clock.zone).currentShift()
                val updated = current?.let { change(it, now) }
                when {
                    current == null -> invalid(onMissing)
                    updated == null -> AppResult.Success(null)
                    else -> save(updated, data.shifts, now).thenReturn(updated)
                }
            }

        private fun AppResult<WorkShift?>.orIssue(issue: WorkClockIssue): AppResult<WorkShift> =
            when (this) {
                is AppResult.Success -> value?.let { AppResult.Success(it) } ?: invalid(issue)
                is AppResult.Failure -> this
            }

        private fun <T> AppResult<Unit>.thenReturn(value: T): AppResult<T> =
            when (this) {
                is AppResult.Success -> AppResult.Success(value)
                is AppResult.Failure -> this
            }

        private fun invalid(issue: WorkClockIssue): AppResult.Failure =
            AppResult.Failure(AppError.Validation(issue.name))
    }

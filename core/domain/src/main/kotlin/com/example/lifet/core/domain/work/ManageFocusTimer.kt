package com.example.lifet.core.domain.work

import com.example.lifet.core.common.AppError
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.data.work.FocusTimerAlarm
import com.example.lifet.core.data.work.WorkRepository
import com.example.lifet.core.model.work.FocusKind
import com.example.lifet.core.model.work.FocusSession
import com.example.lifet.core.model.work.WorkClockIssue
import com.example.lifet.core.model.work.WorkRules
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.util.UUID
import javax.inject.Inject

/**
 * Acciones del temporizador de foco. El estado vive en la base de datos como una sesión con su hora de inicio
 * (`Instant`): lo que queda se calcula siempre con `ahora - inicio`, así que rotar la pantalla, mandar la app a
 * segundo plano o que muera el proceso no pierde ni desajusta el tiempo.
 *
 * Una sesión se da por terminada con [finishIfDue] cuando se agota su tiempo (la pantalla lo llama al verla; un
 * servicio futuro lo haría desde [FocusTimerAlarm]) y con [stop] cuando el usuario la corta antes.
 */
public class ManageFocusTimer
    @Inject
    constructor(
        private val repository: WorkRepository,
        private val alarm: FocusTimerAlarm,
        private val clock: Clock,
    ) {
        /**
         * Arranca un intervalo de [kind] de [minutes] minutos. Falla si ya hay uno en marcha o la duración no es
         * válida. Un intervalo agotado que nadie cerró se da por terminado antes de arrancar el siguiente.
         */
        public suspend fun start(
            kind: FocusKind,
            minutes: Int,
            projectId: String? = null,
            title: String = "",
        ): AppResult<FocusSession> {
            val issue = validate(minutes, title)
            if (issue == null) finishIfDue()
            return when {
                issue != null -> invalid(issue)
                running() != null -> invalid(WorkClockIssue.TIMER_ALREADY_RUNNING)
                else -> begin(kind, minutes, projectId, title)
            }
        }

        private suspend fun begin(
            kind: FocusKind,
            minutes: Int,
            projectId: String?,
            title: String,
        ): AppResult<FocusSession> {
            val session =
                FocusSession(
                    id = UUID.randomUUID().toString(),
                    kind = kind,
                    start = clock.instant(),
                    zone = clock.zone,
                    plannedSeconds = minutes * WorkRules.SECONDS_PER_MINUTE,
                    projectId = projectId.takeIf { kind.isFocus },
                    title = if (kind.isFocus) title.trim() else "",
                )
            return when (val saved = repository.saveFocusSession(session)) {
                is AppResult.Success -> {
                    alarm.schedule(session)
                    AppResult.Success(session)
                }

                is AppResult.Failure -> {
                    saved
                }
            }
        }

        /**
         * Si el intervalo en marcha ya agotó su tiempo, lo cierra en su hora prevista (no en la actual: si la app
         * estuvo horas en segundo plano, el foco dura lo que se planeó) y lo marca como completado. Devuelve la
         * sesión cerrada o `null` si no había nada vencido.
         */
        public suspend fun finishIfDue(): FocusSession? {
            val now = clock.instant()
            val due = running()?.takeIf { it.isDueAt(now) } ?: return null
            val finished = due.copy(end = due.plannedEnd, completed = true)
            val saved = repository.saveFocusSession(finished) is AppResult.Success
            if (saved) alarm.cancel(finished.id)
            return finished.takeIf { saved }
        }

        /**
         * El usuario corta el intervalo en marcha. Si ya ha pasado al menos [WorkRules.MIN_RECORDED_FOCUS_SECONDS] se
         * guarda como cancelado (cuenta lo transcurrido); si fue un toque accidental, se descarta.
         */
        public suspend fun stop(): AppResult<Unit> {
            val now = clock.instant()
            val current = running() ?: return invalid(WorkClockIssue.NO_TIMER_RUNNING)
            alarm.cancel(current.id)
            return if (current.elapsedSecondsAt(now) < WorkRules.MIN_RECORDED_FOCUS_SECONDS) {
                repository.deleteFocusSession(current.id)
            } else {
                repository.saveFocusSession(current.copy(end = minOf(now, current.plannedEnd), completed = false))
            }
        }

        public suspend fun delete(id: String): AppResult<Unit> = repository.deleteFocusSession(id)

        /** Restaura una sesión borrada ("Deshacer"). */
        public suspend fun restore(session: FocusSession): AppResult<Unit> = repository.saveFocusSession(session)

        private suspend fun running(): FocusSession? =
            repository
                .observeData()
                .first()
                .focusSessions
                .filter { it.isRunning }
                .maxByOrNull { it.start }

        private fun validate(
            minutes: Int,
            title: String,
        ): WorkClockIssue? =
            when {
                minutes !in 1..WorkRules.MAX_FOCUS_MINUTES -> WorkClockIssue.INVALID_DURATION
                title.length > WorkRules.FOCUS_TITLE_MAX_LENGTH -> WorkClockIssue.TITLE_TOO_LONG
                else -> null
            }

        private fun invalid(issue: WorkClockIssue): AppResult.Failure =
            AppResult.Failure(AppError.Validation(issue.name))
    }

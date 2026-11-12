package com.example.lifet.core.data.fitness

import com.example.lifet.core.common.AppResult
import com.example.lifet.core.model.fitness.DailySteps
import com.example.lifet.core.model.fitness.Workout
import java.time.LocalDate

/** Disponibilidad de una fuente externa de actividad. */
public enum class ActivitySourceStatus {
    /** Lista para leer. */
    AVAILABLE,

    /** El servicio existe pero hay que instalarlo o actualizarlo (p. ej. Health Connect). */
    NEEDS_INSTALL,

    /** El dispositivo no la admite o la iteración actual no la implementa. */
    UNAVAILABLE,
}

/**
 * Fuente externa de actividad (Health Connect). Room sigue siendo la fuente de verdad de la UI: una implementación
 * real lee de la fuente y sincroniza hacia [FitnessRepository], marcando los registros con
 * `RecordSource.HEALTH_CONNECT` y su `externalId` para deduplicar. Las lecturas nunca lanzan: devuelven
 * [AppResult]; sin permiso, `AppError.PermissionDenied`.
 *
 * En esta iteración sólo existe [UnavailableActivitySource]; el registro manual es el único origen de datos.
 */
public interface ActivitySource {
    public suspend fun status(): ActivitySourceStatus

    /** Entrenos que empiezan en `[from, toExclusive)`. */
    public suspend fun workouts(
        from: LocalDate,
        toExclusive: LocalDate,
    ): AppResult<List<Workout>>

    /** Pasos diarios de `[from, toExclusive)`. */
    public suspend fun steps(
        from: LocalDate,
        toExclusive: LocalDate,
    ): AppResult<List<DailySteps>>
}

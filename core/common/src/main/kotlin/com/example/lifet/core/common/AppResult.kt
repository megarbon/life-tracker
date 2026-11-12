package com.example.lifet.core.common

import kotlin.coroutines.cancellation.CancellationException

/** Errores tipados que pueden llegar hasta la UI. Nunca se exponen excepciones genéricas. */
public sealed interface AppError {
    /** Fallo de almacenamiento local (Room, DataStore). */
    public data object Storage : AppError

    /** Fallo de red o de una fuente externa. */
    public data object Network : AppError

    /** El usuario no ha concedido un permiso necesario. */
    public data object PermissionDenied : AppError

    /** Dato inexistente o ya borrado. */
    public data object NotFound : AppError

    /** Entrada del usuario no válida; [reason] es un identificador estable, no texto de UI. */
    public data class Validation(
        public val reason: String,
    ) : AppError

    public data object Unknown : AppError
}

/** Resultado de una operación que puede fallar de forma esperada. */
public sealed interface AppResult<out T> {
    public data class Success<out T>(
        public val value: T,
    ) : AppResult<T>

    public data class Failure(
        public val error: AppError,
    ) : AppResult<Nothing>
}

public inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> =
    when (this) {
        is AppResult.Success -> AppResult.Success(transform(value))
        is AppResult.Failure -> this
    }

public fun <T> AppResult<T>.getOrNull(): T? = (this as? AppResult.Success)?.value

/**
 * Ejecuta [block] y convierte las excepciones en [AppResult.Failure] usando [onError].
 * [CancellationException] se relanza siempre: capturarla rompe la cancelación estructurada.
 */
public inline fun <T> runCatchingApp(
    onError: (Throwable) -> AppError = { AppError.Unknown },
    block: () -> T,
): AppResult<T> =
    try {
        AppResult.Success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (
        @Suppress("TooGenericExceptionCaught") throwable: Throwable,
    ) {
        AppResult.Failure(onError(throwable))
    }

package com.example.lifet.feature.fitness

/*
 * Formularios de la UI. Son `Serializable` para que `rememberSaveable` los conserve tras una rotación. Validan en el
 * borde (texto tecleado -> dominio) y sólo entregan al ViewModel objetos ya válidos.
 */

internal enum class DraftIssue {
    DURATION_MISSING,
    DURATION_INVALID,
    DISTANCE_INVALID,
    ENERGY_INVALID,
    EXERCISE_NAME_MISSING,
    EXERCISE_WITHOUT_SETS,
    SET_INVALID,
    WEIGHT_MISSING,
    WEIGHT_INVALID,
    STEPS_MISSING,
    STEPS_INVALID,
    TEMPLATE_NEEDS_TITLE,
}

internal sealed interface DraftResult<out T> {
    data class Valid<out T>(
        val value: T,
    ) : DraftResult<T>

    data class Invalid(
        val issues: Set<DraftIssue>,
    ) : DraftResult<Nothing>
}

/** Problemas del borrador, o ninguno si es válido. */
internal fun <T> DraftResult<T>.issues(): Set<DraftIssue> = (this as? DraftResult.Invalid)?.issues.orEmpty()

/** Acumula problemas mientras se valida; [result] entrega el valor sólo si no hubo ninguno. */
internal class IssueCollector {
    private val issues = mutableSetOf<DraftIssue>()

    fun add(issue: DraftIssue) {
        issues += issue
    }

    /** Anota [issue] si [valid] es falso. */
    fun require(
        valid: Boolean,
        issue: DraftIssue,
    ) {
        if (!valid) issues += issue
    }

    /**
     * Número entero de [text] dentro de [range]. Anota [missing] si está vacío e [invalid] si no es un entero del
     * rango; devuelve `null` en ambos casos.
     */
    fun whole(
        text: String,
        range: IntRange,
        missing: DraftIssue,
        invalid: DraftIssue,
    ): Int? {
        val parsed = parseWholeNumber(text)?.takeIf { it in range }
        when {
            text.isBlank() -> issues += missing
            parsed == null -> issues += invalid
        }
        return parsed
    }

    /** [build] devuelve `null` si le falta algún dato; sólo ocurre si ya hay una incidencia anotada. */
    fun <T> result(build: () -> T?): DraftResult<T> {
        val value = if (issues.isEmpty()) build() else null
        return if (value != null) DraftResult.Valid(value) else DraftResult.Invalid(issues.toSet())
    }
}

package com.example.lifet.core.model.work

/**
 * Motivos por los que una acción de fichaje o del temporizador no se puede hacer. Identificadores estables que viajan
 * en `AppError.Validation.reason` (junto a los nombres de [ShiftIssue]); la UI los traduce a texto.
 */
public enum class WorkClockIssue {
    ALREADY_CLOCKED_IN,
    NOT_CLOCKED_IN,
    ALREADY_ON_BREAK,
    NOT_ON_BREAK,
    SHIFT_NOT_FOUND,
    TIMER_ALREADY_RUNNING,
    NO_TIMER_RUNNING,
    INVALID_DURATION,
    TITLE_TOO_LONG,
    NAME_MISSING,
    NAME_TOO_LONG,
    TOO_MANY_PROJECTS,
    NOTE_TOO_LONG,
    TARGET_INVALID,
    NO_WORKING_DAYS,
}

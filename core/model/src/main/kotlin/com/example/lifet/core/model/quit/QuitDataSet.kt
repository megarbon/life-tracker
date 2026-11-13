package com.example.lifet.core.model.quit

/** Todo el contenido de Dejar de fumar y beber. El repositorio lo emite entero; la calculadora lo resume. */
public data class QuitDataSet(
    public val habits: List<QuitHabit> = emptyList(),
    public val cravings: List<QuitCraving> = emptyList(),
    public val consumptions: List<QuitConsumption> = emptyList(),
) {
    /** `true` si el área no se ha usado nunca: sin hábitos no hay nada que mostrar. */
    public val hasNoRecords: Boolean get() = habits.isEmpty()
}

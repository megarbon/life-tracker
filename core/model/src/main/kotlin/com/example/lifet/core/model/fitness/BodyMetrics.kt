package com.example.lifet.core.model.fitness

import java.time.LocalDate

/** Peso corporal de un día en gramos. Hay uno por día: registrar otro el mismo día lo sustituye. */
public data class BodyWeight(
    public val date: LocalDate,
    public val weightGrams: Long,
    public val note: String = "",
)

/** Pasos de un día. Uno por día; [source] distingue el registro manual del que llegará de Health Connect. */
public data class DailySteps(
    public val date: LocalDate,
    public val steps: Int,
    public val source: RecordSource = RecordSource.MANUAL,
)

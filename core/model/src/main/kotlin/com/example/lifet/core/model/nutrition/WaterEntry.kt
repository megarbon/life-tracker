package com.example.lifet.core.model.nutrition

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Un registro de agua (ml). Cada toque de "+250 ml" es una entrada, así se puede deshacer. */
public data class WaterEntry(
    public val id: String,
    public val date: LocalDate,
    public val ml: Int,
    public val loggedAt: Instant,
    public val zone: ZoneId,
) {
    public companion object {
        public const val GLASS_ML: Int = 250
    }
}

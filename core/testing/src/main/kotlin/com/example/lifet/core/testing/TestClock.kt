package com.example.lifet.core.testing

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/** Reloj fijo para que los tests no dependan de la fecha real. */
public object TestClock {
    public val DEFAULT_ZONE: ZoneId = ZoneId.of("Europe/Madrid")

    public fun fixed(
        instant: String = "2026-03-10T09:00:00Z",
        zone: ZoneId = DEFAULT_ZONE,
    ): Clock = Clock.fixed(Instant.parse(instant), zone)
}

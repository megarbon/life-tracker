package com.example.lifet.core.domain

import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Devuelve el "día lógico" actual en la zona del [Clock] inyectado.
 * Punto único para saber qué es "hoy", de modo que los tests fijen el reloj en vez de depender de la fecha real.
 */
public class GetTodayUseCase
    @Inject
    constructor(
        private val clock: Clock,
    ) {
        public operator fun invoke(): LocalDate = LocalDate.now(clock)
    }

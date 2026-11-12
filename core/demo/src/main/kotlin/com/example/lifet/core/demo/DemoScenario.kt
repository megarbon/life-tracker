package com.example.lifet.core.demo

/** Escenarios de datos de prueba que cada área debe saber generar. */
public enum class DemoScenario {
    /** Sin datos: para probar estados vacíos. */
    EMPTY,

    /** Usuario recién llegado: unos 3 días de uso. */
    NEW_USER,

    /** Uso habitual: unos 6 meses con patrones semanales y estacionales creíbles. */
    REGULAR,

    /** Casos límite: medianoche, DST, divisas distintas, importes enormes, textos larguísimos, emojis. */
    EDGE_CASES,
}

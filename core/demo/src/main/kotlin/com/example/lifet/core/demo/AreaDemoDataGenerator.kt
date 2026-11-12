package com.example.lifet.core.demo

import com.example.lifet.core.model.Area
import java.time.Clock
import java.util.Random

/**
 * Contrato que cada área implementa para sembrar datos de prueba.
 *
 * Reglas: determinista (todo el azar sale de [DemoContext.random], con semilla fija) y fechas relativas a
 * [DemoContext.clock], nunca a la fecha real. Se registra con `@IntoSet` en un módulo Hilt del propio
 * área, de modo que añadir un generador no obliga a tocar ningún fichero compartido.
 */
public interface AreaDemoDataGenerator {
    public val area: Area

    /** Borra los datos del área y los sustituye por los de [scenario]. */
    public suspend fun seed(
        scenario: DemoScenario,
        context: DemoContext,
    )

    /** Elimina todos los datos del área. */
    public suspend fun clear()
}

/** Entorno determinista que recibe cada generador. */
public class DemoContext(
    public val clock: Clock,
    seed: Long = DEFAULT_SEED,
) {
    public val random: Random = Random(seed)

    public companion object {
        public const val DEFAULT_SEED: Long = 42L
    }
}

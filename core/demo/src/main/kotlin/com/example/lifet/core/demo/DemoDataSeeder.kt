package com.example.lifet.core.demo

import java.time.Clock
import javax.inject.Inject

/** Orquesta todos los generadores registrados por las áreas. */
public class DemoDataSeeder
    @Inject
    constructor(
        private val generators: Set<@JvmSuppressWildcards AreaDemoDataGenerator>,
        private val clock: Clock,
    ) {
        /** Carga [scenario] en todas las áreas con la misma semilla, así dos cargas dan los mismos datos. */
        public suspend fun load(scenario: DemoScenario) {
            generators.forEach { generator ->
                generator.seed(scenario, DemoContext(clock))
            }
        }

        public suspend fun clearAll() {
            generators.forEach { it.clear() }
        }

        public val registeredAreas: Set<com.example.lifet.core.model.Area>
            get() = generators.mapTo(mutableSetOf()) { it.area }
    }

package com.example.lifet.core.demo.quit

import com.example.lifet.core.common.AppResult
import com.example.lifet.core.data.quit.QuitRepository
import com.example.lifet.core.demo.AreaDemoDataGenerator
import com.example.lifet.core.demo.DemoContext
import com.example.lifet.core.demo.DemoScenario
import com.example.lifet.core.model.Area
import com.example.lifet.core.model.quit.QuitDataSet
import javax.inject.Inject

/**
 * Siembra los datos de Dejar de fumar y beber de cada [DemoScenario]. Los construye [QuitDemoData] (pura y
 * determinista); aquí sólo se guardan, sustituyendo lo que hubiera.
 */
public class QuitDemoDataGenerator
    @Inject
    constructor(
        private val repository: QuitRepository,
    ) : AreaDemoDataGenerator {
        override val area: Area = Area.QUIT

        override suspend fun seed(
            scenario: DemoScenario,
            context: DemoContext,
        ) {
            repository.replaceAll(QuitDemoData(context.clock, context.random).build(scenario)).orThrow()
        }

        override suspend fun clear() {
            repository.replaceAll(QuitDataSet()).orThrow()
        }

        // El seeder es una herramienta de depuración: si falla debe verse, no quedarse a medias en silencio.
        private fun AppResult<Unit>.orThrow() {
            if (this is AppResult.Failure) error("No se pudieron sembrar los datos de dejar de fumar: $error")
        }
    }

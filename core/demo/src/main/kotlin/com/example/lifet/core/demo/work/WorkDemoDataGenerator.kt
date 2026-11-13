package com.example.lifet.core.demo.work

import com.example.lifet.core.common.AppResult
import com.example.lifet.core.data.work.WorkRepository
import com.example.lifet.core.demo.AreaDemoDataGenerator
import com.example.lifet.core.demo.DemoContext
import com.example.lifet.core.demo.DemoScenario
import com.example.lifet.core.model.Area
import com.example.lifet.core.model.work.WorkDataSet
import javax.inject.Inject

/**
 * Siembra los datos de Trabajo de cada [DemoScenario]. Los construye [WorkDemoData] (pura y determinista); aquí sólo
 * se guardan, sustituyendo lo que hubiera (incluidos los ajustes, que vuelven a los de fábrica).
 */
public class WorkDemoDataGenerator
    @Inject
    constructor(
        private val repository: WorkRepository,
    ) : AreaDemoDataGenerator {
        override val area: Area = Area.WORK

        override suspend fun seed(
            scenario: DemoScenario,
            context: DemoContext,
        ) {
            repository.replaceAll(WorkDemoData(context.clock, context.random).build(scenario)).orThrow()
        }

        override suspend fun clear() {
            repository.replaceAll(WorkDataSet()).orThrow()
        }

        // El seeder es una herramienta de depuración: si falla debe verse, no quedarse a medias en silencio.
        private fun AppResult<Unit>.orThrow() {
            if (this is AppResult.Failure) error("No se pudieron sembrar los datos de trabajo: $error")
        }
    }

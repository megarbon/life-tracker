package com.example.lifet.core.demo.fitness

import com.example.lifet.core.common.AppResult
import com.example.lifet.core.data.fitness.FitnessRepository
import com.example.lifet.core.demo.AreaDemoDataGenerator
import com.example.lifet.core.demo.DemoContext
import com.example.lifet.core.demo.DemoScenario
import com.example.lifet.core.model.Area
import com.example.lifet.core.model.fitness.FitnessDataSet
import javax.inject.Inject

/**
 * Siembra los datos de Deporte de cada [DemoScenario]. Los construye [FitnessDemoData] (pura y determinista); aquí
 * sólo se guardan, sustituyendo lo que hubiera.
 */
public class FitnessDemoDataGenerator
    @Inject
    constructor(
        private val repository: FitnessRepository,
    ) : AreaDemoDataGenerator {
        override val area: Area = Area.FITNESS

        override suspend fun seed(
            scenario: DemoScenario,
            context: DemoContext,
        ) {
            repository.replaceAll(FitnessDemoData(context.clock, context.random).build(scenario)).orThrow()
        }

        override suspend fun clear() {
            repository.replaceAll(FitnessDataSet()).orThrow()
        }

        // El seeder es una herramienta de depuración: si falla debe verse, no quedarse a medias en silencio.
        private fun AppResult<Unit>.orThrow() {
            if (this is AppResult.Failure) error("No se pudieron sembrar los datos de deporte: $error")
        }
    }

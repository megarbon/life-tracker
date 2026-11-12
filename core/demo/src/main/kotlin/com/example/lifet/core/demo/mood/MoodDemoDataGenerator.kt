package com.example.lifet.core.demo.mood

import com.example.lifet.core.common.AppResult
import com.example.lifet.core.data.mood.MoodRepository
import com.example.lifet.core.demo.AreaDemoDataGenerator
import com.example.lifet.core.demo.DemoContext
import com.example.lifet.core.demo.DemoScenario
import com.example.lifet.core.model.Area
import javax.inject.Inject

/** Siembra los check-ins de ánimo de cada [DemoScenario] (ver [MoodDemoData]). */
public class MoodDemoDataGenerator
    @Inject
    constructor(
        private val repository: MoodRepository,
    ) : AreaDemoDataGenerator {
        override val area: Area = Area.MOOD

        override suspend fun seed(
            scenario: DemoScenario,
            context: DemoContext,
        ) {
            clear()
            val entries = MoodDemoData(context.clock, context.random).build(scenario)
            if (entries.isNotEmpty()) repository.saveAll(entries).orThrow()
        }

        override suspend fun clear() {
            repository.clear().orThrow()
        }

        // El seeder es una herramienta de depuración: si falla debe verse, no quedarse a medias en silencio.
        private fun AppResult<Unit>.orThrow() {
            if (this is AppResult.Failure) error("No se pudieron sembrar los datos de ánimo: $error")
        }
    }

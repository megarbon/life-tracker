package com.example.lifet.core.demo.finance

import com.example.lifet.core.data.finance.FinanceRepository
import com.example.lifet.core.demo.AreaDemoDataGenerator
import com.example.lifet.core.demo.DemoContext
import com.example.lifet.core.demo.DemoScenario
import com.example.lifet.core.model.Area
import com.example.lifet.core.model.finance.FinanceDataSet
import javax.inject.Inject

/**
 * Siembra datos de Finanzas para cada [DemoScenario]. Los datos los construye [FinanceDemoData] (pura y
 * determinista); aquí sólo se guardan, sustituyendo lo que hubiera.
 */
public class FinanceDemoDataGenerator
    @Inject
    constructor(
        private val repository: FinanceRepository,
    ) : AreaDemoDataGenerator {
        override val area: Area = Area.FINANCE

        override suspend fun seed(
            scenario: DemoScenario,
            context: DemoContext,
        ) {
            repository.replaceAll(FinanceDemoData(context.clock, context.random).build(scenario))
        }

        // Tras borrar se vuelven a crear las categorías de partida y la cuenta principal para no dejar el área rota.
        override suspend fun clear() {
            repository.replaceAll(FinanceDataSet())
            repository.ensureDefaults()
        }
    }

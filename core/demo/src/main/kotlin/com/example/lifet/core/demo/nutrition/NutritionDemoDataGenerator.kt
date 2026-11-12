package com.example.lifet.core.demo.nutrition

import com.example.lifet.core.common.AppResult
import com.example.lifet.core.data.nutrition.FoodRepository
import com.example.lifet.core.data.nutrition.NutritionGoalsRepository
import com.example.lifet.core.data.nutrition.NutritionLogRepository
import com.example.lifet.core.demo.AreaDemoDataGenerator
import com.example.lifet.core.demo.DemoContext
import com.example.lifet.core.demo.DemoScenario
import com.example.lifet.core.model.Area
import com.example.lifet.core.model.nutrition.FoodItem
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

/**
 * Datos de prueba de Alimentación. La base de alimentos de la semilla NO es dato de demo: existe siempre, también
 * con EMPTY; aquí sólo se escriben diario, agua, objetivos, alimentos personalizados y favoritos.
 */
public class NutritionDemoDataGenerator
    @Inject
    constructor(
        private val log: NutritionLogRepository,
        private val goals: NutritionGoalsRepository,
        private val foods: FoodRepository,
    ) : AreaDemoDataGenerator {
        override val area: Area = Area.NUTRITION

        override suspend fun seed(
            scenario: DemoScenario,
            context: DemoContext,
        ) {
            clear()
            val catalog = foods.searchFoods("").first().associateBy { it.id } + DEMO_EDGE_FOODS.associateBy { it.id }
            val builder = NutritionDemoBuilder(context.clock.zone, context.random, catalog + customCatalog())
            val today = LocalDate.now(context.clock)
            val planner = NutritionDemoPlanner(builder, today, NutritionEdgeCasePlanner(builder, today))
            persist(planner.plan(scenario))
        }

        override suspend fun clear() {
            log.clearAll()
            goals.clearGoals()
            foods.clearCustomFoodsAndFavorites()
        }

        private fun customCatalog(): Map<String, FoodItem> = DEMO_CUSTOM_FOODS.associateBy { it.id }

        private suspend fun persist(plan: NutritionDemoPlan) {
            plan.customFoods.forEach { check(foods.saveCustomFood(it) is AppResult.Success) }
            plan.favoriteIds.forEach { foods.setFavorite(it, favorite = true) }
            plan.goals?.let { check(goals.setGoals(it) is AppResult.Success) }
            check(log.saveMeals(plan.meals) is AppResult.Success)
            check(log.saveWater(plan.water) is AppResult.Success)
        }
    }

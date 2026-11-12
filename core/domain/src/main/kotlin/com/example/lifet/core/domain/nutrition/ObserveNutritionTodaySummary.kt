package com.example.lifet.core.domain.nutrition

import com.example.lifet.core.data.nutrition.NutritionGoalsRepository
import com.example.lifet.core.data.nutrition.NutritionLogRepository
import com.example.lifet.core.domain.GetTodayUseCase
import com.example.lifet.core.model.nutrition.DayTotals
import com.example.lifet.core.model.nutrition.NutritionTodaySummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Lo que la pantalla "Hoy" necesita de Alimentación: kcal y macros de hoy frente a sus objetivos, y agua.
 * "Hoy" se fija al empezar a recolectar; la pantalla Hoy debe volver a recolectar al cambiar de día.
 */
public class ObserveNutritionTodaySummary
    @Inject
    constructor(
        private val log: NutritionLogRepository,
        private val goals: NutritionGoalsRepository,
        private val getToday: GetTodayUseCase,
    ) {
        public operator fun invoke(): Flow<NutritionTodaySummary> {
            val today = getToday()
            val meals = log.observeMeals(today)
            val water = log.observeWater(today)
            return combine(meals, water, goals.observeGoals()) { mealList, waterList, goal ->
                val totals = DayTotals.of(today, mealList, waterList)
                NutritionTodaySummary(
                    date = today,
                    consumed = totals.nutrients,
                    goals = goal,
                    waterMl = totals.waterMl,
                    mealCount = totals.mealCount,
                )
            }
        }
    }

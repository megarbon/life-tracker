package com.example.lifet.core.domain.nutrition

import com.example.lifet.core.data.nutrition.NutritionGoalsRepository
import com.example.lifet.core.data.nutrition.NutritionLogRepository
import com.example.lifet.core.model.nutrition.NutritionDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

/** Todo lo registrado en un día (comidas y agua) con los objetivos vigentes: base de la pantalla Diario. */
public class ObserveNutritionDay
    @Inject
    constructor(
        private val log: NutritionLogRepository,
        private val goals: NutritionGoalsRepository,
    ) {
        public operator fun invoke(date: LocalDate): Flow<NutritionDay> =
            combine(log.observeMeals(date), log.observeWater(date), goals.observeGoals()) { meals, water, goal ->
                NutritionDay(date = date, meals = meals, water = water, goals = goal)
            }
    }

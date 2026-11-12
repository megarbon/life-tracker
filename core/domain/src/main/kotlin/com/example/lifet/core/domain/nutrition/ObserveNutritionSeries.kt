package com.example.lifet.core.domain.nutrition

import com.example.lifet.core.data.nutrition.NutritionGoalsRepository
import com.example.lifet.core.data.nutrition.NutritionLogRepository
import com.example.lifet.core.model.DateRange
import com.example.lifet.core.model.nutrition.DayTotals
import com.example.lifet.core.model.nutrition.NutritionSeries
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

/**
 * Serie diaria de Alimentación para gráficas e Insights. Incluye un [DayTotals] por cada día del rango; los
 * días sin ningún alimento registrado quedan con [DayTotals.hasMeals] = `false`.
 */
public class ObserveNutritionSeries
    @Inject
    constructor(
        private val log: NutritionLogRepository,
        private val goals: NutritionGoalsRepository,
    ) {
        public operator fun invoke(range: DateRange): Flow<NutritionSeries> = invoke(range.start, range.endExclusive)

        /** Ventana arbitraria `[start, endExclusive)`, p. ej. "últimos 7 días", que no coincide con un [DateRange]. */
        public operator fun invoke(
            start: LocalDate,
            endExclusive: LocalDate,
        ): Flow<NutritionSeries> =
            combine(
                log.observeMealsBetween(start, endExclusive),
                log.observeWaterBetween(start, endExclusive),
                goals.observeGoals(),
            ) { meals, water, goal ->
                val mealsByDay = meals.groupBy { it.date }
                val waterByDay = water.groupBy { it.date }
                val days =
                    generateSequence(start) { it.plusDays(1) }
                        .takeWhile { it < endExclusive }
                        .map { date -> DayTotals.of(date, mealsByDay[date].orEmpty(), waterByDay[date].orEmpty()) }
                        .toList()
                NutritionSeries(start = start, endExclusive = endExclusive, days = days, goals = goal)
            }
    }

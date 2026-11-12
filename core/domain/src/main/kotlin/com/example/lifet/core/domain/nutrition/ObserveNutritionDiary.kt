package com.example.lifet.core.domain.nutrition

import com.example.lifet.core.model.nutrition.NutritionDiary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

/** Datos de la pantalla Diario para [date]: el día completo y la semana (7 días) que termina en él. */
public class ObserveNutritionDiary
    @Inject
    constructor(
        private val observeDay: ObserveNutritionDay,
        private val observeSeries: ObserveNutritionSeries,
    ) {
        public operator fun invoke(date: LocalDate): Flow<NutritionDiary> =
            combine(
                observeDay(date),
                observeSeries(date.minusDays(WEEK_DAYS - 1), date.plusDays(1)),
            ) { day, week -> NutritionDiary(day, week) }

        private companion object {
            const val WEEK_DAYS = 7L
        }
    }

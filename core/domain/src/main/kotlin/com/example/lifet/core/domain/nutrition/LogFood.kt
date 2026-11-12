package com.example.lifet.core.domain.nutrition

import com.example.lifet.core.common.AppResult
import com.example.lifet.core.common.map
import com.example.lifet.core.data.nutrition.NutritionLogRepository
import com.example.lifet.core.model.nutrition.FoodItem
import com.example.lifet.core.model.nutrition.MealEntry
import com.example.lifet.core.model.nutrition.MealType
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject

/**
 * Registra [grams] gramos de un alimento en una comida de [date]. Devuelve la entrada creada para poder deshacerla.
 * La hora del registro es la actual, también al anotar un día pasado, para conservar el orden dentro del día.
 */
public class LogFood
    @Inject
    constructor(
        private val log: NutritionLogRepository,
        private val clock: Clock,
    ) {
        public suspend operator fun invoke(
            food: FoodItem,
            grams: Int,
            mealType: MealType,
            date: LocalDate,
        ): AppResult<MealEntry> {
            val entry =
                MealEntry(
                    id = UUID.randomUUID().toString(),
                    date = date,
                    mealType = mealType,
                    foodId = food.id,
                    foodName = food.name,
                    grams = grams,
                    nutrients = food.nutrientsFor(grams),
                    loggedAt = date.atTime(LocalTime.now(clock)).atZone(clock.zone).toInstant(),
                    zone = clock.zone,
                )
            return log.saveMeals(listOf(entry)).map { entry }
        }
    }

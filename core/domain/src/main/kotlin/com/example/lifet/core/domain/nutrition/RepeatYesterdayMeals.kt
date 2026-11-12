package com.example.lifet.core.domain.nutrition

import com.example.lifet.core.common.AppResult
import com.example.lifet.core.common.map
import com.example.lifet.core.data.nutrition.NutritionLogRepository
import com.example.lifet.core.model.nutrition.MealEntry
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/**
 * "Repetir ayer": copia al día [date] lo registrado el día anterior, con las mismas comidas y gramos.
 * Devuelve las entradas nuevas (vacío si ayer no había nada) para poder deshacer la operación.
 * Si [date] ya tiene alimentos se añaden igualmente: el usuario decide, la app no lo adivina.
 */
public class RepeatYesterdayMeals
    @Inject
    constructor(
        private val log: NutritionLogRepository,
    ) {
        public suspend operator fun invoke(date: LocalDate): AppResult<List<MealEntry>> =
            when (val yesterday = log.getMeals(date.minusDays(1))) {
                is AppResult.Failure -> yesterday
                is AppResult.Success -> copyTo(date, yesterday.value)
            }

        private suspend fun copyTo(
            date: LocalDate,
            source: List<MealEntry>,
        ): AppResult<List<MealEntry>> {
            val copies =
                source.map { entry ->
                    // Se conserva la hora local de ayer para que el orden dentro del día sea el mismo.
                    val loggedAt = date.atTime(entry.loggedAt.atZone(entry.zone).toLocalTime()).atZone(entry.zone)
                    entry.copy(id = UUID.randomUUID().toString(), date = date, loggedAt = loggedAt.toInstant())
                }
            return if (copies.isEmpty()) {
                AppResult.Success(copies)
            } else {
                log.saveMeals(copies).map { copies }
            }
        }
    }

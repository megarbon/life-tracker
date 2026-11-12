package com.example.lifet.core.domain.nutrition

import com.example.lifet.core.common.AppResult
import com.example.lifet.core.common.map
import com.example.lifet.core.data.nutrition.NutritionLogRepository
import com.example.lifet.core.model.nutrition.WaterEntry
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject

/** Suma [ml] de agua (por defecto un vaso de 250 ml) al día [date]. Devuelve la entrada para poder deshacerla. */
public class AddWater
    @Inject
    constructor(
        private val log: NutritionLogRepository,
        private val clock: Clock,
    ) {
        public suspend operator fun invoke(
            date: LocalDate,
            ml: Int = WaterEntry.GLASS_ML,
        ): AppResult<WaterEntry> {
            val entry =
                WaterEntry(
                    id = UUID.randomUUID().toString(),
                    date = date,
                    ml = ml,
                    loggedAt = date.atTime(LocalTime.now(clock)).atZone(clock.zone).toInstant(),
                    zone = clock.zone,
                )
            return log.saveWater(listOf(entry)).map { entry }
        }
    }

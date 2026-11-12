package com.example.lifet.core.domain.mood

import com.example.lifet.core.data.mood.MoodRepository
import com.example.lifet.core.model.mood.MoodRules
import com.example.lifet.core.model.mood.MoodStats
import com.example.lifet.core.model.mood.MoodTodaySummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Ánimo medio de hoy, número de check-ins y si falta el de hoy, más la media semanal y la racha baja que
 * necesita la tarjeta de Hoy. "Hoy" se calcula con el [Clock] inyectado en el momento de suscribirse.
 */
public class ObserveMoodTodaySummary
    @Inject
    constructor(
        private val repository: MoodRepository,
        private val clock: Clock,
    ) {
        public operator fun invoke(): Flow<MoodTodaySummary> {
            val today = LocalDate.now(clock)
            val from = today.minusDays(MoodRules.SUMMARY_WINDOW_DAYS.toLong())
            return repository
                .observeBetween(from, today.plusDays(1), clock.zone)
                .map { entries -> MoodStats.todaySummary(entries, today) }
        }
    }

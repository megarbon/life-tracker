package com.example.lifet.core.domain.mood

import com.example.lifet.core.data.mood.MoodRepository
import com.example.lifet.core.model.DateRange
import com.example.lifet.core.model.mood.MoodRules
import com.example.lifet.core.model.mood.MoodSeries
import com.example.lifet.core.model.mood.MoodStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject

/**
 * Serie de ánimo de un [DateRange] para gráficas e Insights. Carga también los días previos que necesitan la
 * media móvil de 7 días y la comparación con la semana anterior.
 */
public class ObserveMoodSeries
    @Inject
    constructor(
        private val repository: MoodRepository,
        private val clock: Clock,
    ) {
        public operator fun invoke(range: DateRange): Flow<MoodSeries> {
            val lookback = (MoodRules.MOVING_AVERAGE_DAYS * 2).toLong()
            return repository
                .observeBetween(range.start.minusDays(lookback), range.endExclusive, clock.zone)
                .map { entries -> MoodStats.series(range, entries) }
        }
    }

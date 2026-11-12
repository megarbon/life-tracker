package com.example.lifet.core.data.fitness

import com.example.lifet.core.common.AppResult
import com.example.lifet.core.model.fitness.DailySteps
import com.example.lifet.core.model.fitness.Workout
import java.time.LocalDate
import javax.inject.Inject

/** Sustituye a Health Connect hasta que se implemente: declara que no hay fuente y no devuelve datos. */
internal class UnavailableActivitySource
    @Inject
    constructor() : ActivitySource {
        override suspend fun status(): ActivitySourceStatus = ActivitySourceStatus.UNAVAILABLE

        override suspend fun workouts(
            from: LocalDate,
            toExclusive: LocalDate,
        ): AppResult<List<Workout>> = AppResult.Success(emptyList())

        override suspend fun steps(
            from: LocalDate,
            toExclusive: LocalDate,
        ): AppResult<List<DailySteps>> = AppResult.Success(emptyList())
    }

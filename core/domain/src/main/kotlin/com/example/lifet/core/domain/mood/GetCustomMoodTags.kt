package com.example.lifet.core.domain.mood

import com.example.lifet.core.common.AppResult
import com.example.lifet.core.common.map
import com.example.lifet.core.data.mood.MoodRepository
import com.example.lifet.core.data.mood.MoodTagHistory
import com.example.lifet.core.model.mood.MoodTags
import javax.inject.Inject

/** Etiquetas propias usadas antes (emociones y factores que no son de las predefinidas), para sugerirlas. */
public class GetCustomMoodTags
    @Inject
    constructor(
        private val repository: MoodRepository,
    ) {
        public suspend operator fun invoke(): AppResult<MoodTagHistory> =
            repository.tagHistory().map { history ->
                MoodTagHistory(
                    emotions = history.emotions - MoodTags.DEFAULT_EMOTIONS.toSet(),
                    factors = history.factors - MoodTags.DEFAULT_FACTORS.toSet(),
                )
            }
    }

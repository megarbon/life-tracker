package com.example.lifet.core.domain.mood

import com.example.lifet.core.data.mood.MoodRepository
import com.example.lifet.core.model.mood.MoodEntry
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Todos los check-ins, del más reciente al más antiguo, para el diario y el calendario. */
public class ObserveMoodEntries
    @Inject
    constructor(
        private val repository: MoodRepository,
    ) {
        public operator fun invoke(): Flow<List<MoodEntry>> = repository.observeAll()
    }

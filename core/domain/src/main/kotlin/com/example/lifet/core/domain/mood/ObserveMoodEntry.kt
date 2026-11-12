package com.example.lifet.core.domain.mood

import com.example.lifet.core.data.mood.MoodRepository
import com.example.lifet.core.model.mood.MoodEntry
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Un check-in concreto; emite `null` si no existe o se acaba de borrar. */
public class ObserveMoodEntry
    @Inject
    constructor(
        private val repository: MoodRepository,
    ) {
        public operator fun invoke(id: Long): Flow<MoodEntry?> = repository.observeEntry(id)
    }

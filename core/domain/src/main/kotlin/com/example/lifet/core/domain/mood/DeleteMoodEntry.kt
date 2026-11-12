package com.example.lifet.core.domain.mood

import com.example.lifet.core.common.AppResult
import com.example.lifet.core.data.mood.MoodRepository
import com.example.lifet.core.model.mood.MoodEntry
import javax.inject.Inject

/**
 * Borra un check-in. Para "Deshacer" quien llama conserva el [MoodEntry] y lo vuelve a guardar con
 * [SaveMoodEntry] usando el mismo id, así la entrada recupera su sitio en el diario.
 */
public class DeleteMoodEntry
    @Inject
    constructor(
        private val repository: MoodRepository,
    ) {
        public suspend operator fun invoke(entry: MoodEntry): AppResult<Unit> = repository.delete(entry.id)
    }

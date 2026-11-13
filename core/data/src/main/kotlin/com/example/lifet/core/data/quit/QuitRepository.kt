package com.example.lifet.core.data.quit

import com.example.lifet.core.common.AppResult
import com.example.lifet.core.model.quit.QuitConsumption
import com.example.lifet.core.model.quit.QuitCraving
import com.example.lifet.core.model.quit.QuitDataSet
import com.example.lifet.core.model.quit.QuitHabit
import kotlinx.coroutines.flow.Flow

/**
 * Fuente única de verdad de Dejar de fumar y beber (Room). Las lecturas son reactivas; las escrituras devuelven
 * [AppResult] y nunca lanzan, salvo cancelación.
 */
@Suppress("TooManyFunctions") // Un repositorio por área: tres entidades con alta, edición y borrado.
public interface QuitRepository {
    /** Todo el contenido del área; vuelve a emitir con cada cambio. */
    public fun observeData(): Flow<QuitDataSet>

    public suspend fun saveHabit(habit: QuitHabit): AppResult<Unit>

    /** Borra el hábito con todos sus antojos y consumos; [restoreHabit] los devuelve juntos para "Deshacer". */
    public suspend fun deleteHabit(id: String): AppResult<Unit>

    public suspend fun restoreHabit(
        habit: QuitHabit,
        cravings: List<QuitCraving>,
        consumptions: List<QuitConsumption>,
    ): AppResult<Unit>

    public suspend fun saveCraving(craving: QuitCraving): AppResult<Unit>

    public suspend fun deleteCraving(id: String): AppResult<Unit>

    public suspend fun saveConsumption(consumption: QuitConsumption): AppResult<Unit>

    public suspend fun deleteConsumption(id: String): AppResult<Unit>

    /** Sustituye todos los datos del área (demo, restaurar copia). */
    public suspend fun replaceAll(data: QuitDataSet): AppResult<Unit>
}

package com.example.lifet.core.testing.quit

import com.example.lifet.core.common.AppError
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.data.quit.QuitRepository
import com.example.lifet.core.model.quit.QuitConsumption
import com.example.lifet.core.model.quit.QuitCraving
import com.example.lifet.core.model.quit.QuitDataSet
import com.example.lifet.core.model.quit.QuitHabit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update

/**
 * Repositorio de Dejar de fumar y beber en memoria con las mismas reglas que el de Room (borrar un hábito borra sus
 * antojos y consumos). [failObserve] hace que la lectura falle, para probar el estado de error, y [failWrites] que
 * las escrituras devuelvan un error de almacenamiento.
 */
@Suppress("TooManyFunctions") // Implementa QuitRepository.
public class InMemoryQuitRepository(
    initial: QuitDataSet = QuitDataSet(),
) : QuitRepository {
    private val state = MutableStateFlow(initial)

    public var failObserve: Boolean = false
    public var failWrites: Boolean = false

    public val current: QuitDataSet get() = state.value

    // Se evalúa al recolectar, así un test puede activar el fallo después de crear el ViewModel.
    override fun observeData(): Flow<QuitDataSet> =
        flow {
            check(!failObserve) { "Fallo simulado" }
            emitAll(state)
        }

    override suspend fun saveHabit(habit: QuitHabit): AppResult<Unit> =
        edit { data -> data.copy(habits = data.habits.replacing(habit) { it.id == habit.id }) }

    override suspend fun deleteHabit(id: String): AppResult<Unit> =
        edit { data ->
            data.copy(
                habits = data.habits.filterNot { it.id == id },
                cravings = data.cravings.filterNot { it.habitId == id },
                consumptions = data.consumptions.filterNot { it.habitId == id },
            )
        }

    override suspend fun restoreHabit(
        habit: QuitHabit,
        cravings: List<QuitCraving>,
        consumptions: List<QuitConsumption>,
    ): AppResult<Unit> =
        edit { data ->
            data.copy(
                habits = data.habits.replacing(habit) { it.id == habit.id },
                cravings = cravings.fold(data.cravings) { list, item -> list.replacing(item) { it.id == item.id } },
                consumptions =
                    consumptions.fold(data.consumptions) { list, item -> list.replacing(item) { it.id == item.id } },
            )
        }

    override suspend fun saveCraving(craving: QuitCraving): AppResult<Unit> =
        edit { data -> data.copy(cravings = data.cravings.replacing(craving) { it.id == craving.id }) }

    override suspend fun deleteCraving(id: String): AppResult<Unit> =
        edit { data -> data.copy(cravings = data.cravings.filterNot { it.id == id }) }

    override suspend fun saveConsumption(consumption: QuitConsumption): AppResult<Unit> =
        edit { data -> data.copy(consumptions = data.consumptions.replacing(consumption) { it.id == consumption.id }) }

    override suspend fun deleteConsumption(id: String): AppResult<Unit> =
        edit { data -> data.copy(consumptions = data.consumptions.filterNot { it.id == id }) }

    override suspend fun replaceAll(data: QuitDataSet): AppResult<Unit> = edit { data }

    private fun edit(change: (QuitDataSet) -> QuitDataSet): AppResult<Unit> =
        if (failWrites) {
            AppResult.Failure(AppError.Storage)
        } else {
            state.update(change)
            AppResult.Success(Unit)
        }

    private inline fun <T> List<T>.replacing(
        item: T,
        matches: (T) -> Boolean,
    ): List<T> = if (any(matches)) map { if (matches(it)) item else it } else this + item
}

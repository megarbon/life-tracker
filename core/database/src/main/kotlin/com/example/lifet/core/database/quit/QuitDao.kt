package com.example.lifet.core.database.quit

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Acceso a las tablas de Dejar de fumar y beber. Antojos y consumos dependen de su hábito con borrado en cascada:
 * borrar el hábito los elimina, y [restoreHabit] los devuelve juntos para poder deshacer.
 */
@Dao
@Suppress("TooManyFunctions") // Un DAO por área: tres tablas que se escriben juntas.
public abstract class QuitDao {
    @Query("SELECT * FROM quit_habits ORDER BY startedAt, id")
    public abstract fun observeHabits(): Flow<List<QuitHabitEntity>>

    @Query("SELECT * FROM quit_cravings ORDER BY occurredAt DESC, id")
    public abstract fun observeCravings(): Flow<List<QuitCravingEntity>>

    @Query("SELECT * FROM quit_consumptions ORDER BY occurredAt DESC, id")
    public abstract fun observeConsumptions(): Flow<List<QuitConsumptionEntity>>

    // @Upsert actualiza en sitio: un REPLACE borraría la fila y arrastraría sus antojos por la cascada.
    @Upsert
    public abstract suspend fun upsertHabit(entity: QuitHabitEntity)

    @Upsert
    public abstract suspend fun upsertCraving(entity: QuitCravingEntity)

    @Upsert
    public abstract suspend fun upsertConsumption(entity: QuitConsumptionEntity)

    @Upsert
    public abstract suspend fun upsertHabits(entities: List<QuitHabitEntity>)

    @Upsert
    public abstract suspend fun upsertCravings(entities: List<QuitCravingEntity>)

    @Upsert
    public abstract suspend fun upsertConsumptions(entities: List<QuitConsumptionEntity>)

    @Query("DELETE FROM quit_habits WHERE id = :id")
    public abstract suspend fun deleteHabit(id: String)

    @Query("DELETE FROM quit_cravings WHERE id = :id")
    public abstract suspend fun deleteCraving(id: String)

    @Query("DELETE FROM quit_consumptions WHERE id = :id")
    public abstract suspend fun deleteConsumption(id: String)

    @Query("DELETE FROM quit_habits")
    public abstract suspend fun deleteAllHabits()

    /** Devuelve un hábito borrado con todo su historial, en una sola transacción. */
    @Transaction
    public open suspend fun restoreHabit(
        habit: QuitHabitEntity,
        cravings: List<QuitCravingEntity>,
        consumptions: List<QuitConsumptionEntity>,
    ) {
        upsertHabit(habit)
        upsertCravings(cravings)
        upsertConsumptions(consumptions)
    }

    /** Sustituye todo el contenido del área (datos de demo, restaurar copia). Los hijos caen en cascada. */
    @Transaction
    public open suspend fun replaceAll(
        habits: List<QuitHabitEntity>,
        cravings: List<QuitCravingEntity>,
        consumptions: List<QuitConsumptionEntity>,
    ) {
        deleteAllHabits()
        upsertHabits(habits)
        upsertCravings(cravings)
        upsertConsumptions(consumptions)
    }
}

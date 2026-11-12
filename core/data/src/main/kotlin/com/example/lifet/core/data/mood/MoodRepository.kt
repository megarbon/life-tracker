package com.example.lifet.core.data.mood

import com.example.lifet.core.common.AppResult
import com.example.lifet.core.model.mood.MoodEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId

/** Etiquetas propias que el usuario ha usado hace poco, de más reciente a más antigua y sin repetidos. */
public data class MoodTagHistory(
    public val emotions: List<String>,
    public val factors: List<String>,
)

/** Fuente de verdad de los check-ins de ánimo (Room). Los errores de almacenamiento llegan como [AppResult]. */
public interface MoodRepository {
    /** Todos los check-ins, del más reciente al más antiguo. */
    public fun observeAll(): Flow<List<MoodEntry>>

    /**
     * Check-ins cuyo día lógico está en `[from, toExclusive)`, del más reciente al más antiguo.
     * [zone] sólo sirve para acotar la consulta; el día de cada entrada es el de su propia zona.
     */
    public fun observeBetween(
        from: LocalDate,
        toExclusive: LocalDate,
        zone: ZoneId,
    ): Flow<List<MoodEntry>>

    /** Emite `null` si la entrada no existe (borrada). */
    public fun observeEntry(id: Long): Flow<MoodEntry?>

    /** Inserta (id = 0) o actualiza; devuelve el id resultante. */
    public suspend fun save(entry: MoodEntry): AppResult<Long>

    public suspend fun delete(id: Long): AppResult<Unit>

    public suspend fun tagHistory(): AppResult<MoodTagHistory>
}

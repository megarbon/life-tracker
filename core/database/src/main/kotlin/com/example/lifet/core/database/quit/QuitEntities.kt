package com.example.lifet.core.database.quit

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.lifet.core.model.quit.BaselinePeriod
import com.example.lifet.core.model.quit.CopingStrategy
import com.example.lifet.core.model.quit.CravingTrigger
import com.example.lifet.core.model.quit.HabitKind
import com.example.lifet.core.model.quit.HabitMode
import java.time.Instant
import java.time.ZoneId

// Importes como Long en unidades menores + código de divisa: nunca Double. Instante UTC + zona del usuario.
// Las columnas evitan palabras reservadas de SQLite (`trigger` se llama `cravingTrigger`).

@Entity(tableName = "quit_habits", indices = [Index("startedAt")])
public data class QuitHabitEntity(
    @PrimaryKey public val id: String,
    public val kind: HabitKind,
    public val name: String,
    public val mode: HabitMode,
    public val startedAt: Instant,
    public val zoneId: ZoneId,
    public val baselineUnits: Int,
    public val baselinePeriod: BaselinePeriod,
    public val unitsPerPack: Int,
    public val packPriceMinor: Long,
    public val currency: String,
    public val weeklyLimit: Int?,
)

/** Antojo de un hábito; borrar el hábito borra sus antojos. */
@Entity(
    tableName = "quit_cravings",
    foreignKeys =
        [
            ForeignKey(
                entity = QuitHabitEntity::class,
                parentColumns = ["id"],
                childColumns = ["habitId"],
                onDelete = ForeignKey.CASCADE,
            ),
        ],
    indices = [Index("habitId"), Index("occurredAt")],
)
public data class QuitCravingEntity(
    @PrimaryKey public val id: String,
    public val habitId: String,
    public val occurredAt: Instant,
    public val zoneId: ZoneId,
    public val intensity: Int,
    public val cravingTrigger: CravingTrigger,
    public val strategy: CopingStrategy?,
    public val note: String,
)

/** Consumo o recaída de un hábito; borrar el hábito borra sus consumos. */
@Entity(
    tableName = "quit_consumptions",
    foreignKeys =
        [
            ForeignKey(
                entity = QuitHabitEntity::class,
                parentColumns = ["id"],
                childColumns = ["habitId"],
                onDelete = ForeignKey.CASCADE,
            ),
        ],
    indices = [Index("habitId"), Index("occurredAt")],
)
public data class QuitConsumptionEntity(
    @PrimaryKey public val id: String,
    public val habitId: String,
    public val occurredAt: Instant,
    public val zoneId: ZoneId,
    public val units: Int,
    public val cravingTrigger: CravingTrigger?,
    public val note: String,
)

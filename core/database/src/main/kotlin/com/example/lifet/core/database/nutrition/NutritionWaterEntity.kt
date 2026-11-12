package com.example.lifet.core.database.nutrition

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Un registro de agua en mililitros. */
@Entity(tableName = "nutrition_water", indices = [Index(value = ["date"])])
public data class NutritionWaterEntity(
    @PrimaryKey public val id: String,
    public val date: LocalDate,
    public val ml: Int,
    public val loggedAt: Instant,
    public val zone: ZoneId,
)

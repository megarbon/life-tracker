package com.example.lifet.core.database

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Conversores globales. Se guardan en tipos primitivos estables:
 * `Instant` como epoch millis (UTC), `ZoneId` como su id IANA y `LocalDate` como epoch day.
 * `Money` no necesita conversor: cada entidad guarda `amountMinor` + `currency` en dos columnas.
 */
internal class Converters {
    @TypeConverter
    fun instantToLong(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun longToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun zoneIdToString(value: ZoneId?): String? = value?.id

    @TypeConverter
    fun stringToZoneId(value: String?): ZoneId? = value?.let(ZoneId::of)

    @TypeConverter
    fun localDateToLong(value: LocalDate?): Long? = value?.toEpochDay()

    @TypeConverter
    fun longToLocalDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)
}

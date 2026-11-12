package com.example.lifet.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Base de datos única de la app (SQLite vía Room), fuente de verdad offline-first.
 *
 * Las entidades de cada área viven en su subpaquete (`finance/`, `nutrition/`…) y se registran aquí.
 * Cada cambio de esquema exige subir [VERSION] y aportar una migración con su test; los esquemas
 * exportados en `schemas/` se versionan en git.
 */
@Database(
    entities = [
        AppMetaEntity::class,
        com.example.lifet.core.database.finance.FinanceAccountEntity::class,
        com.example.lifet.core.database.finance.FinanceCategoryEntity::class,
        com.example.lifet.core.database.finance.FinanceTransactionEntity::class,
        com.example.lifet.core.database.finance.FinanceTransferEntity::class,
        com.example.lifet.core.database.finance.FinanceBudgetEntity::class,
        com.example.lifet.core.database.finance.FinanceRecurringRuleEntity::class,
    ],
    version = LifeTrackerDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
internal abstract class LifeTrackerDatabase : RoomDatabase() {
    abstract fun appMetaDao(): AppMetaDao

    abstract fun financeDao(): com.example.lifet.core.database.finance.FinanceDao

    companion object {
        const val VERSION = 1
        const val NAME = "life-tracker.db"
    }
}

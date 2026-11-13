package com.example.lifet.core.database.di

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.room.Room
import com.example.lifet.core.database.AppMetaDao
import com.example.lifet.core.database.LifeTrackerDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    // Release: nunca se destruye la base; un esquema incompatible exige migración.
    // Debug: mientras el esquema se define, un hash distinto con el mismo VERSION se resuelve recreando la base,
    // pero SIEMPRE tras copiarla (ver DebugSchemaRecovery) y solo ante ese fallo concreto de Room.
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        clock: Clock,
    ): LifeTrackerDatabase {
        val debuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        val database = build(context)
        if (debuggable && rejectsSchema(database)) {
            database.close()
            val file = context.getDatabasePath(LifeTrackerDatabase.NAME)
            DebugSchemaRecovery.backupAndDelete(file, clock.millis()) { Log.w("SchemaRecovery", it) }
            return build(context)
        }
        return database
    }

    @Provides
    fun provideAppMetaDao(database: LifeTrackerDatabase): AppMetaDao = database.appMetaDao()

    private fun build(context: Context): LifeTrackerDatabase =
        Room.databaseBuilder(context, LifeTrackerDatabase::class.java, LifeTrackerDatabase.NAME).build()

    // Room valida el hash del esquema al abrir y lanza IllegalStateException si no coincide; cualquier otro error
    // (disco lleno, bloqueo, permisos) se propaga tal cual en lugar de vaciar la base.
    private fun rejectsSchema(database: LifeTrackerDatabase): Boolean =
        try {
            database.openHelper.writableDatabase
            false
        } catch (e: IllegalStateException) {
            e.message?.contains("Room cannot verify the data integrity") == true
        }
}

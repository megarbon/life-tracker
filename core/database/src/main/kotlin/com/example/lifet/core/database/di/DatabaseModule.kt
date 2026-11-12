package com.example.lifet.core.database.di

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.room.Room
import com.example.lifet.core.database.AppMetaDao
import com.example.lifet.core.database.LifeTrackerDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    // En release nunca se destruye la base: perder datos personales en silencio no es aceptable.
    // En builds debug el esquema aún se está definiendo y puede cambiar sin subir la versión, así que
    // si Room rechaza la base existente se borra y se recrea (solo contiene datos de demo).
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): LifeTrackerDatabase {
        val debuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        val database = build(context, debuggable)
        if (debuggable && !opensCleanly(database)) {
            database.close()
            context.deleteDatabase(LifeTrackerDatabase.NAME)
            return build(context, debuggable)
        }
        return database
    }

    @Provides
    fun provideAppMetaDao(database: LifeTrackerDatabase): AppMetaDao = database.appMetaDao()

    private fun build(
        context: Context,
        debuggable: Boolean,
    ): LifeTrackerDatabase =
        Room
            .databaseBuilder(context, LifeTrackerDatabase::class.java, LifeTrackerDatabase.NAME)
            .apply { if (debuggable) fallbackToDestructiveMigration(dropAllTables = true) }
            .build()

    // Abrir la base valida el hash del esquema; un cambio sin subir la versión lanza IllegalStateException.
    // Solo debug: cualquier fallo al abrir implica recrear la base.
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun opensCleanly(database: LifeTrackerDatabase): Boolean =
        try {
            database.openHelper.writableDatabase
            true
        } catch (e: RuntimeException) {
            false
        }
}

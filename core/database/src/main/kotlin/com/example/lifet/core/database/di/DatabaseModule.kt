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
    // En builds debug el esquema aún se está definiendo, así que se recrea sola (solo datos de demo).
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): LifeTrackerDatabase {
        val debuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        return Room
            .databaseBuilder(context, LifeTrackerDatabase::class.java, LifeTrackerDatabase.NAME)
            .apply { if (debuggable) fallbackToDestructiveMigration(dropAllTables = true) }
            .build()
    }

    @Provides
    fun provideAppMetaDao(database: LifeTrackerDatabase): AppMetaDao = database.appMetaDao()
}

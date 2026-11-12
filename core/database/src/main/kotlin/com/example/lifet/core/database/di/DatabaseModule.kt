package com.example.lifet.core.database.di

import android.content.Context
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
    // Sin fallbackToDestructiveMigration: perder datos personales en silencio no es aceptable.
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): LifeTrackerDatabase =
        Room.databaseBuilder(context, LifeTrackerDatabase::class.java, LifeTrackerDatabase.NAME).build()

    @Provides
    fun provideAppMetaDao(database: LifeTrackerDatabase): AppMetaDao = database.appMetaDao()
}

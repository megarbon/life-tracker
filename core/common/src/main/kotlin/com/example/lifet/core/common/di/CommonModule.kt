package com.example.lifet.core.common.di

import com.example.lifet.core.common.DefaultDispatcher
import com.example.lifet.core.common.IoDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.time.Clock
import java.time.ZoneId

@Module
@InstallIn(SingletonComponent::class)
internal object CommonModule {
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    /**
     * Reloj con la zona del sistema en el momento de crearlo. Los datos se guardan como `Instant` + zona
     * del usuario, así que quien necesite "hoy" debe volver a consultar la zona (ver `ZoneId.systemDefault()`).
     */
    @Provides
    fun provideClock(): Clock = Clock.system(ZoneId.systemDefault())
}

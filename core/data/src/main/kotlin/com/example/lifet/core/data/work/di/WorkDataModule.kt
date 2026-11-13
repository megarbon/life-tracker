package com.example.lifet.core.data.work.di

import com.example.lifet.core.data.work.FocusTimerAlarm
import com.example.lifet.core.data.work.NoOpFocusTimerAlarm
import com.example.lifet.core.data.work.RoomWorkRepository
import com.example.lifet.core.data.work.WorkRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class WorkDataModule {
    @Binds
    abstract fun bindWorkRepository(impl: RoomWorkRepository): WorkRepository

    /** Mientras no exista servicio foreground ni notificación, el aviso del temporizador no hace nada. */
    @Binds
    abstract fun bindFocusTimerAlarm(impl: NoOpFocusTimerAlarm): FocusTimerAlarm
}

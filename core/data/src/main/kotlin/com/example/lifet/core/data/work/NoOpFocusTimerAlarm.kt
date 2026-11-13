package com.example.lifet.core.data.work

import com.example.lifet.core.model.work.FocusSession
import javax.inject.Inject

/** Sin servicio ni notificación todavía: el temporizador sigue funcionando con la pantalla abierta. */
internal class NoOpFocusTimerAlarm
    @Inject
    constructor() : FocusTimerAlarm {
        override fun schedule(session: FocusSession) = Unit

        override fun cancel(sessionId: String) = Unit
    }

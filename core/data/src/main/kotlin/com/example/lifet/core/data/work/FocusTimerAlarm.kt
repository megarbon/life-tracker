package com.example.lifet.core.data.work

import com.example.lifet.core.model.work.FocusSession

/**
 * Costura para avisar al usuario cuando acaba un intervalo de foco con la app en segundo plano (servicio foreground
 * o alarma exacta + notificación). En esta iteración no hay implementación real: está enlazada a
 * [NoOpFocusTimerAlarm]. El temporizador no depende de ella: sobrevive a una rotación o a que el proceso muera porque
 * guarda su hora de inicio (`FocusSession.start`) y lo que queda se recalcula siempre con `ahora - inicio`.
 *
 * Una implementación real sustituye el `@Binds` de `WorkDataModule`: [schedule] programa el aviso para
 * `session.plannedEnd` y [cancel] lo retira si el usuario detiene el temporizador antes.
 */
public interface FocusTimerAlarm {
    public fun schedule(session: FocusSession)

    public fun cancel(sessionId: String)
}

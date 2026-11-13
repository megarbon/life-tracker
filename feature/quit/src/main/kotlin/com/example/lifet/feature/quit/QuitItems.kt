package com.example.lifet.feature.quit

import com.example.lifet.core.model.quit.QuitConsumption
import com.example.lifet.core.model.quit.QuitCraving
import com.example.lifet.core.model.quit.QuitEvent
import com.example.lifet.core.model.quit.QuitOverview

/** El elemento borrable de un antojo o un consumo. */
internal fun QuitEvent.toItem(): QuitItem =
    when (this) {
        is QuitCraving -> QuitItem.Craving(this)
        is QuitConsumption -> QuitItem.Consumption(this)
    }

/**
 * Un hábito con todo su historial, tal como está ahora, listo para borrarlo y poder restaurarlo entero con
 * "Deshacer". Devuelve `null` si el hábito ya no existe.
 */
internal fun QuitOverview?.habitItem(habitId: String): QuitItem.Habit? {
    val habit = this?.habits?.firstOrNull { it.habit.id == habitId }?.habit ?: return null
    val events = this.events.filter { it.habitId == habitId }
    return QuitItem.Habit(
        habit = habit,
        cravings = events.filterIsInstance<QuitCraving>(),
        consumptions = events.filterIsInstance<QuitConsumption>(),
    )
}

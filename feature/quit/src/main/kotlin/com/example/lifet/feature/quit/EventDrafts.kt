package com.example.lifet.feature.quit

import com.example.lifet.core.model.quit.CopingStrategy
import com.example.lifet.core.model.quit.CravingTrigger
import com.example.lifet.core.model.quit.QuitConsumption
import com.example.lifet.core.model.quit.QuitCraving
import com.example.lifet.core.model.quit.QuitRules
import java.io.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// --- Antojo y consumo ---

/** Antojo tecleado: [intensity] de 1 a 5, con su disparador y, si se quiere, lo que se hizo para atravesarlo. */
internal data class CravingDraft(
    val id: String,
    val habitId: String,
    val date: LocalDate,
    val minute: Int,
    val intensity: Int,
    val trigger: CravingTrigger,
    val strategy: CopingStrategy?,
    val note: String,
) : Serializable {
    private companion object {
        private const val serialVersionUID = 1L
    }
}

/** Consumo tecleado, sin culpa: unidades, un disparador opcional y una nota. */
internal data class ConsumptionDraft(
    val id: String,
    val habitId: String,
    val date: LocalDate,
    val minute: Int,
    val unitsText: String,
    val trigger: CravingTrigger?,
    val note: String,
) : Serializable {
    private companion object {
        private const val serialVersionUID = 1L
    }
}

private fun IssueCollector.checkMoment(
    at: Instant,
    now: Instant,
    habitStart: Instant,
) {
    require(at <= now, DraftIssue.EVENT_IN_FUTURE)
    require(at >= habitStart, DraftIssue.EVENT_BEFORE_START)
}

internal fun CravingDraft.validate(
    zone: ZoneId,
    now: Instant,
    habitStart: Instant,
): DraftResult<QuitCraving> {
    val collector = IssueCollector()
    val at = instantOf(date, minute, zone)
    collector.checkMoment(at, now, habitStart)
    return collector.result {
        QuitCraving(
            id = id,
            habitId = habitId,
            at = at,
            zone = zone,
            intensity = intensity.coerceIn(QuitRules.MIN_INTENSITY, QuitRules.MAX_INTENSITY),
            trigger = trigger,
            strategy = strategy,
            note = note.trim().take(QuitRules.NOTE_MAX_LENGTH),
        )
    }
}

internal fun ConsumptionDraft.validate(
    zone: ZoneId,
    now: Instant,
    habitStart: Instant,
): DraftResult<QuitConsumption> {
    val collector = IssueCollector()
    val units =
        collector.whole(
            unitsText,
            1..QuitRules.MAX_CONSUMPTION_UNITS,
            DraftIssue.UNITS_MISSING,
            DraftIssue.UNITS_INVALID,
        )
    val at = instantOf(date, minute, zone)
    collector.checkMoment(at, now, habitStart)
    return collector.result {
        units?.let {
            QuitConsumption(
                id = id,
                habitId = habitId,
                at = at,
                zone = zone,
                units = it,
                trigger = trigger,
                note = note.trim().take(QuitRules.NOTE_MAX_LENGTH),
            )
        }
    }
}

internal fun blankCravingDraft(
    id: String,
    habitId: String,
    today: LocalDate,
    nowMinute: Int,
    trigger: CravingTrigger = CravingTrigger.STRESS,
    strategy: CopingStrategy? = null,
): CravingDraft =
    CravingDraft(
        id = id,
        habitId = habitId,
        date = today,
        minute = nowMinute,
        intensity = DEFAULT_INTENSITY,
        trigger = trigger,
        strategy = strategy,
        note = "",
    )

internal fun blankConsumptionDraft(
    id: String,
    habitId: String,
    today: LocalDate,
    nowMinute: Int,
): ConsumptionDraft =
    ConsumptionDraft(
        id = id,
        habitId = habitId,
        date = today,
        minute = nowMinute,
        unitsText = "1",
        trigger = null,
        note = "",
    )

internal fun QuitCraving.toDraft(): CravingDraft {
    val local = at.atZone(zone)
    return CravingDraft(
        id = id,
        habitId = habitId,
        date = local.toLocalDate(),
        minute = local.hour * MINUTES_IN_HOUR + local.minute,
        intensity = intensity,
        trigger = trigger,
        strategy = strategy,
        note = note,
    )
}

internal fun QuitConsumption.toDraft(): ConsumptionDraft {
    val local = at.atZone(zone)
    return ConsumptionDraft(
        id = id,
        habitId = habitId,
        date = local.toLocalDate(),
        minute = local.hour * MINUTES_IN_HOUR + local.minute,
        unitsText = units.toString(),
        trigger = trigger,
        note = note,
    )
}

private const val DEFAULT_INTENSITY = 3
private const val MINUTES_IN_HOUR = 60

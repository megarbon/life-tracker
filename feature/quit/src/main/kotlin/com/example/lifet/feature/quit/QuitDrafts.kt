package com.example.lifet.feature.quit

import com.example.lifet.core.designsystem.util.parseAmount
import com.example.lifet.core.designsystem.util.toEditableText
import com.example.lifet.core.model.CurrencyCode
import com.example.lifet.core.model.Money
import com.example.lifet.core.model.quit.BaselinePeriod
import com.example.lifet.core.model.quit.CopingStrategy
import com.example.lifet.core.model.quit.CravingTrigger
import com.example.lifet.core.model.quit.HabitKind
import com.example.lifet.core.model.quit.HabitMode
import com.example.lifet.core.model.quit.QuitConsumption
import com.example.lifet.core.model.quit.QuitCraving
import com.example.lifet.core.model.quit.QuitHabit
import com.example.lifet.core.model.quit.QuitRules
import java.io.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/*
 * Formularios de la UI. Son `Serializable` para que `rememberSaveable` los conserve tras una rotación. Validan en el
 * borde (texto tecleado -> dominio) y sólo entregan al ViewModel objetos ya válidos.
 */

internal enum class DraftIssue {
    BASELINE_MISSING,
    BASELINE_INVALID,
    PACK_SIZE_MISSING,
    PACK_SIZE_INVALID,
    PRICE_MISSING,
    PRICE_INVALID,
    CURRENCY_INVALID,
    LIMIT_MISSING,
    LIMIT_INVALID,
    START_IN_FUTURE,
    UNITS_MISSING,
    UNITS_INVALID,
    EVENT_IN_FUTURE,
    EVENT_BEFORE_START,
}

internal sealed interface DraftResult<out T> {
    data class Valid<out T>(
        val value: T,
    ) : DraftResult<T>

    data class Invalid(
        val issues: Set<DraftIssue>,
    ) : DraftResult<Nothing>
}

/** Problemas del borrador, o ninguno si es válido. */
internal fun <T> DraftResult<T>.issues(): Set<DraftIssue> = (this as? DraftResult.Invalid)?.issues.orEmpty()

/** Acumula problemas mientras se valida; [result] entrega el valor sólo si no hubo ninguno. */
internal class IssueCollector {
    private val issues = mutableSetOf<DraftIssue>()

    fun add(issue: DraftIssue) {
        issues += issue
    }

    fun require(
        valid: Boolean,
        issue: DraftIssue,
    ) {
        if (!valid) issues += issue
    }

    /** Entero de [text] dentro de [range]; anota [missing] si está vacío e [invalid] si no es un entero del rango. */
    fun whole(
        text: String,
        range: IntRange,
        missing: DraftIssue,
        invalid: DraftIssue,
    ): Int? {
        val parsed = parseWholeNumber(text)?.takeIf { it in range }
        when {
            text.isBlank() -> issues += missing
            parsed == null -> issues += invalid
        }
        return parsed
    }

    fun <T> result(build: () -> T?): DraftResult<T> {
        val value = if (issues.isEmpty()) build() else null
        return if (value != null) DraftResult.Valid(value) else DraftResult.Invalid(issues.toSet())
    }
}

// --- Hábito ---

/**
 * Hábito tecleado. El alcohol se cuenta por copas (una unidad, un precio por copa); el tabaco por cigarrillos y
 * cajetilla. [currency] es el código ISO 4217 y no se edita: lo fija la UI al abrir la hoja.
 */
internal data class HabitDraft(
    val id: String,
    val kind: HabitKind,
    val mode: HabitMode,
    val name: String,
    val date: LocalDate,
    val startMinute: Int,
    val baselineText: String,
    val period: BaselinePeriod,
    val packSizeText: String,
    val priceText: String,
    val currency: String,
    val limitText: String,
) : Serializable {
    private companion object {
        private const val serialVersionUID = 1L
    }
}

/** El modo reducción sólo existe para el alcohol; el tabaco siempre se deja del todo. */
internal fun HabitDraft.effectiveMode(): HabitMode = if (kind == HabitKind.ALCOHOL) mode else HabitMode.QUIT

internal fun HabitDraft.validate(
    zone: ZoneId,
    now: Instant,
): DraftResult<QuitHabit> {
    val collector = IssueCollector()
    val baseline =
        collector.whole(
            baselineText,
            1..QuitRules.MAX_BASELINE_UNITS,
            DraftIssue.BASELINE_MISSING,
            DraftIssue.BASELINE_INVALID,
        )
    val packSize =
        if (kind == HabitKind.TOBACCO) {
            collector.whole(
                packSizeText,
                1..QuitRules.MAX_UNITS_PER_PACK,
                DraftIssue.PACK_SIZE_MISSING,
                DraftIssue.PACK_SIZE_INVALID,
            )
        } else {
            1
        }
    val price = price(collector)
    val reducing = effectiveMode() == HabitMode.REDUCE
    val limit =
        if (reducing) {
            collector.whole(
                limitText,
                0..QuitRules.MAX_WEEKLY_LIMIT,
                DraftIssue.LIMIT_MISSING,
                DraftIssue.LIMIT_INVALID,
            )
        } else {
            null
        }
    val startedAt = instantOf(date, startMinute, zone)
    collector.require(startedAt <= now, DraftIssue.START_IN_FUTURE)
    return collector.result {
        if (baseline == null || packSize == null || price == null || (reducing && limit == null)) {
            null
        } else {
            QuitHabit(
                id = id,
                kind = kind,
                name = name.trim().take(QuitRules.NAME_MAX_LENGTH),
                mode = effectiveMode(),
                startedAt = startedAt,
                zone = zone,
                baselineUnits = baseline,
                baselinePeriod = period,
                unitsPerPack = packSize,
                packPrice = price,
                weeklyLimit = limit,
            )
        }
    }
}

private fun HabitDraft.price(collector: IssueCollector): Money? {
    val code = runCatching { CurrencyCode(currency.trim().uppercase()) }.getOrNull()
    val parsed = code?.let { parseAmount(priceText, it) }?.takeIf { it.amountMinor <= QuitRules.MAX_PACK_PRICE_MINOR }
    when {
        code == null -> collector.add(DraftIssue.CURRENCY_INVALID)
        priceText.isBlank() -> collector.add(DraftIssue.PRICE_MISSING)
        parsed == null -> collector.add(DraftIssue.PRICE_INVALID)
    }
    return parsed
}

internal fun blankHabitDraft(
    id: String,
    kind: HabitKind,
    today: LocalDate,
    nowMinute: Int,
    currency: CurrencyCode,
): HabitDraft =
    HabitDraft(
        id = id,
        kind = kind,
        mode = HabitMode.QUIT,
        name = "",
        date = today,
        startMinute = nowMinute,
        baselineText = "",
        period = if (kind == HabitKind.TOBACCO) BaselinePeriod.DAY else BaselinePeriod.WEEK,
        packSizeText = QuitRules.DEFAULT_CIGARETTES_PER_PACK.toString(),
        priceText = "",
        currency = currency.value,
        limitText = "",
    )

/** Cambiar de tabaco a alcohol (o al revés) recoloca el periodo habitual sin tocar lo ya tecleado. */
internal fun HabitDraft.withKind(newKind: HabitKind): HabitDraft =
    if (newKind == kind) {
        this
    } else {
        copy(
            kind = newKind,
            mode = if (newKind == HabitKind.ALCOHOL) mode else HabitMode.QUIT,
            period = if (newKind == HabitKind.TOBACCO) BaselinePeriod.DAY else BaselinePeriod.WEEK,
        )
    }

internal fun QuitHabit.toDraft(): HabitDraft {
    val local = startedAt.atZone(zone)
    return HabitDraft(
        id = id,
        kind = kind,
        mode = mode,
        name = name,
        date = local.toLocalDate(),
        startMinute = local.hour * MINUTES_IN_HOUR + local.minute,
        baselineText = baselineUnits.toString(),
        period = baselinePeriod,
        packSizeText = unitsPerPack.toString(),
        priceText = packPrice.toEditableText(),
        currency = packPrice.currency.value,
        limitText = weeklyLimit?.toString().orEmpty(),
    )
}

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

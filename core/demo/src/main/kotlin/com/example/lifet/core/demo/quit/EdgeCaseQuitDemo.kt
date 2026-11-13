@file:Suppress("MagicNumber") // Fechas, horas e importes literales: cada caso límite se describe mejor con su valor.

package com.example.lifet.core.demo.quit

import com.example.lifet.core.model.CurrencyCode
import com.example.lifet.core.model.quit.BaselinePeriod
import com.example.lifet.core.model.quit.CopingStrategy
import com.example.lifet.core.model.quit.CravingTrigger
import com.example.lifet.core.model.quit.HabitKind
import com.example.lifet.core.model.quit.HabitMode
import com.example.lifet.core.model.quit.QuitConsumption
import com.example.lifet.core.model.quit.QuitCraving
import com.example.lifet.core.model.quit.QuitDataSet
import com.example.lifet.core.model.quit.QuitHabit
import com.example.lifet.core.model.quit.QuitRules
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

private const val YEARS_OF_ABSTINENCE_DAYS = 2_300L
private const val LONG_NOTE_SEED = "Hoy ha sido un día largo y difícil 😮‍💨🚭 pero aquí sigo, paso a paso 💪\n"
private const val LONG_NAME_SEED = "Sin humo 🚭 "
private const val CRAVING_STORM = 30
private const val TOKYO_ZONE = "Asia/Tokyo"

/**
 * Escenario EDGE_CASES: años de abstinencia, un hábito recién empezado, importes enormes (el precio máximo que acepta
 * el formulario y una divisa sin decimales), nombres y notas en su longitud máxima con emojis, recaídas a medianoche
 * y justo en el cambio de hora, otra zona horaria, una tormenta de antojos en un solo día y un hueco sin datos.
 */
internal class EdgeCaseQuitDemo(
    private val clock: Clock,
) {
    private val zone: ZoneId = clock.zone
    private val now: Instant = clock.instant()
    private val today: LocalDate = LocalDate.now(clock)

    fun build(): QuitDataSet {
        val habits = listOf(longTerm(), freshStart(), enormous(), longTexts(), yen(), tokyo(), limitOne())
        return QuitDataSet(
            habits = habits,
            cravings = longTermCravings() + storm() + extremes() + gap() + tokyoCravings(),
            consumptions = midnightSlips() + daylightSavingSlips() + longNoteSlip() + enormousSlips() + tokyoSlips(),
        )
    }

    private fun day(back: Long): LocalDate = today.minusDays(back)

    private fun at(
        date: LocalDate,
        time: LocalTime,
    ): Instant = instantAt(date, time, zone)

    // --- Hábitos ---

    /** Más de seis años sin fumar, con una recaída hace tiempo: récord largo, hitos de años y mucho ahorro. */
    private fun longTerm(): QuitHabit =
        demoHabit(
            id = "edge-quit-years",
            kind = HabitKind.TOBACCO,
            name = "Tabaco (años limpio)",
            startedAt = at(day(YEARS_OF_ABSTINENCE_DAYS), LocalTime.of(7, 0)),
            zone = zone,
            baselineUnits = 25,
            baselinePeriod = BaselinePeriod.DAY,
            unitsPerPack = 20,
            priceMinor = 650,
        )

    /** Empezó hace unos minutos: cero días, cero euros y el primer hito (24 h) aún muy lejos. */
    private fun freshStart(): QuitHabit =
        demoHabit(
            id = "edge-quit-fresh",
            kind = HabitKind.TOBACCO,
            name = "Vapeo recién empezado",
            startedAt = now.minusSeconds(FRESH_MINUTES * 60),
            zone = zone,
            baselineUnits = 300,
            baselinePeriod = BaselinePeriod.DAY,
            unitsPerPack = 600,
            priceMinor = 1_990,
        )

    /** El precio máximo que acepta el formulario y el consumo más alto, durante años: el ahorro no desborda. */
    private fun enormous(): QuitHabit =
        demoHabit(
            id = "edge-quit-huge",
            kind = HabitKind.ALCOHOL,
            name = "Cava de coleccionista 🍾",
            startedAt = at(day(YEARS_OF_ABSTINENCE_DAYS), LocalTime.of(9, 0)),
            zone = zone,
            baselineUnits = QuitRules.MAX_BASELINE_UNITS,
            baselinePeriod = BaselinePeriod.DAY,
            unitsPerPack = 1,
            priceMinor = QuitRules.MAX_PACK_PRICE_MINOR,
            mode = HabitMode.REDUCE,
            weeklyLimit = QuitRules.MAX_WEEKLY_LIMIT,
        )

    private fun longTexts(): QuitHabit =
        demoHabit(
            id = "edge-quit-long",
            kind = HabitKind.TOBACCO,
            name = fitText(LONG_NAME_SEED, QuitRules.NAME_MAX_LENGTH),
            startedAt = at(day(40), LocalTime.of(12, 0)),
            zone = zone,
            baselineUnits = 10,
            baselinePeriod = BaselinePeriod.DAY,
            unitsPerPack = 10,
            priceMinor = 380,
        )

    /** Una divisa sin decimales: 1 unidad menor es 1 yen. */
    private fun yen(): QuitHabit =
        demoHabit(
            id = "edge-quit-yen",
            kind = HabitKind.TOBACCO,
            name = "Cigarrillos en yenes",
            startedAt = at(day(20), LocalTime.of(8, 0)),
            zone = zone,
            baselineUnits = 15,
            baselinePeriod = BaselinePeriod.DAY,
            unitsPerPack = 20,
            priceMinor = 600,
            currency = CurrencyCode("JPY"),
        )

    private fun tokyo(): QuitHabit {
        val tokyo = ZoneId.of(TOKYO_ZONE)
        return demoHabit(
            id = "edge-quit-tokyo",
            kind = HabitKind.ALCOHOL,
            name = "Sake en Tokio 🍶",
            startedAt = instantAt(day(30), LocalTime.of(9, 0), tokyo),
            zone = tokyo,
            baselineUnits = 21,
            baselinePeriod = BaselinePeriod.WEEK,
            unitsPerPack = 1,
            priceMinor = 700,
            mode = HabitMode.REDUCE,
            weeklyLimit = 7,
            currency = CurrencyCode("JPY"),
        )
    }

    /** Límite semanal de una sola copa, para probar la semana "al límite". */
    private fun limitOne(): QuitHabit =
        demoHabit(
            id = "edge-quit-limit-one",
            kind = HabitKind.ALCOHOL,
            name = "Cerveza (una a la semana)",
            startedAt = at(day(60), LocalTime.of(18, 0)),
            zone = zone,
            baselineUnits = 7,
            baselinePeriod = BaselinePeriod.WEEK,
            unitsPerPack = 1,
            priceMinor = 300,
            mode = HabitMode.REDUCE,
            weeklyLimit = 1,
        )

    // --- Consumos ---

    /** Recaídas a las 00:00 en punto y a las 23:59:59: cada una pertenece al día lógico que le toca. */
    private fun midnightSlips(): List<QuitConsumption> =
        listOf(
            demoConsumption(
                id = "edge-slip-midnight",
                habitId = "edge-quit-long",
                at = at(day(9), LocalTime.MIDNIGHT),
                zone = zone,
                units = 1,
                note = "Justo a las 00:00.",
            ),
            demoConsumption(
                id = "edge-slip-last-second",
                habitId = "edge-quit-long",
                at = at(day(12), LocalTime.of(23, 59, 59)),
                zone = zone,
                units = 2,
                trigger = CravingTrigger.TIREDNESS,
            ),
            demoConsumption(
                id = "edge-slip-limit-one",
                habitId = "edge-quit-limit-one",
                at = at(day(2), LocalTime.of(23, 30)),
                zone = zone,
                units = 3,
                trigger = CravingTrigger.SOCIAL,
                note = "Se pasó del límite y no pasa nada: la semana que viene seguimos.",
            ),
        )

    /** Una recaída justo antes y otra justo después del último cambio de hora, con la hora saltándose o repitiéndose. */
    private fun daylightSavingSlips(): List<QuitConsumption> {
        val transition = zone.rules.previousTransition(now) ?: return emptyList()
        val first =
            demoConsumption(
                id = "edge-slip-dst-before",
                habitId = "edge-quit-years",
                at = transition.instant.minusSeconds(1),
                zone = zone,
                units = 1,
                note = "Un segundo antes del cambio de hora.",
            )
        val second =
            demoConsumption(
                id = "edge-slip-dst-after",
                habitId = "edge-quit-years",
                at = transition.instant.plusSeconds(1),
                zone = zone,
                units = 1,
                note = "Un segundo después del cambio de hora.",
            )
        return listOf(first, second)
    }

    /** Una nota de 2.000 caracteres con emojis y saltos de línea. */
    private fun longNoteSlip(): List<QuitConsumption> =
        listOf(
            demoConsumption(
                id = "edge-slip-long-note",
                habitId = "edge-quit-long",
                at = at(day(20), LocalTime.of(22, 15)),
                zone = zone,
                units = 1,
                trigger = CravingTrigger.EMOTIONS,
                note = fitText(LONG_NOTE_SEED, QuitRules.NOTE_MAX_LENGTH),
            ),
        )

    /** Un consumo enorme: más que lo previsto, para comprobar que el ahorro nunca baja de cero. */
    private fun enormousSlips(): List<QuitConsumption> =
        listOf(
            demoConsumption(
                id = "edge-slip-huge",
                habitId = "edge-quit-huge",
                at = at(day(5), LocalTime.of(21, 0)),
                zone = zone,
                units = QuitRules.MAX_CONSUMPTION_UNITS,
            ),
        )

    private fun tokyoSlips(): List<QuitConsumption> {
        val tokyo = ZoneId.of(TOKYO_ZONE)
        return listOf(
            demoConsumption(
                "edge-slip-tokyo-1",
                "edge-quit-tokyo",
                instantAt(day(3), LocalTime.of(0, 30), tokyo),
                tokyo,
                4,
            ),
            demoConsumption(
                "edge-slip-tokyo-2",
                "edge-quit-tokyo",
                instantAt(day(1), LocalTime.of(23, 50), tokyo),
                tokyo,
                5,
            ),
        )
    }

    // --- Antojos ---

    private fun longTermCravings(): List<QuitCraving> =
        listOf(
            demoCraving(
                id = "edge-craving-years",
                habitId = "edge-quit-years",
                at = at(day(100), LocalTime.of(10, 5)),
                zone = zone,
                intensity = 2,
                trigger = CravingTrigger.COFFEE,
                strategy = CopingStrategy.WAIT,
                note = "Tras seis años, un antojo suelto. Pasó solo.",
            ),
        )

    /** Treinta antojos en un solo día, a lo largo de todas las horas y con todas las intensidades. */
    private fun storm(): List<QuitCraving> =
        List(CRAVING_STORM) { index ->
            demoCraving(
                id = "edge-storm-$index",
                habitId = "edge-quit-long",
                at = at(day(6), LocalTime.of(index % 24, (index * 7) % 60)),
                zone = zone,
                intensity = index % QuitRules.MAX_INTENSITY + 1,
                trigger = CravingTrigger.entries[index % CravingTrigger.entries.size],
                strategy = CopingStrategy.entries.getOrNull(index % (CopingStrategy.entries.size + 1)),
                note = if (index == 0) fitText(LONG_NOTE_SEED, QuitRules.NOTE_MAX_LENGTH) else "",
            )
        }

    /** Antojos de la intensidad mínima y máxima y uno justo a medianoche. */
    private fun extremes(): List<QuitCraving> =
        listOf(
            demoCraving(
                "edge-craving-min",
                "edge-quit-long",
                at(day(3), LocalTime.of(14, 0)),
                zone,
                1,
                CravingTrigger.OTHER,
            ),
            demoCraving(
                "edge-craving-max",
                "edge-quit-long",
                at(day(3), LocalTime.of(14, 5)),
                zone,
                5,
                CravingTrigger.STRESS,
            ),
            demoCraving(
                "edge-craving-midnight",
                "edge-quit-long",
                at(day(4), LocalTime.MIDNIGHT),
                zone,
                3,
                CravingTrigger.BOREDOM,
            ),
        )

    /** Un hueco: la divisa en yenes tiene antojos al principio y al final y nada en medio (once días sin datos). */
    private fun gap(): List<QuitCraving> =
        listOf(18L, 17L, 16L, 4L, 3L).map { back ->
            demoCraving(
                id = "edge-gap-$back",
                habitId = "edge-quit-yen",
                at = at(day(back), LocalTime.of(11, 0)),
                zone = zone,
                intensity = 3,
                trigger = CravingTrigger.AFTER_MEAL,
            )
        }

    private fun tokyoCravings(): List<QuitCraving> {
        val tokyo = ZoneId.of(TOKYO_ZONE)
        return listOf(
            demoCraving(
                "edge-craving-tokyo",
                "edge-quit-tokyo",
                instantAt(day(2), LocalTime.of(1, 0), tokyo),
                tokyo,
                4,
                CravingTrigger.SOCIAL,
            ),
        )
    }

    private companion object {
        const val FRESH_MINUTES = 40L
    }
}

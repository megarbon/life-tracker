package com.example.lifet.feature.quit

import com.example.lifet.core.model.CurrencyCode
import com.example.lifet.core.model.quit.BaselinePeriod
import com.example.lifet.core.model.quit.CravingTrigger
import com.example.lifet.core.model.quit.HabitKind
import com.example.lifet.core.model.quit.HabitMode
import com.example.lifet.core.model.quit.QuitRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class QuitDraftsTest {
    private val zone = ZoneId.of("Europe/Madrid")
    private val today = LocalDate.of(2026, 3, 10)
    private val now = Instant.parse("2026-03-10T15:00:00Z")
    private val habitStart = Instant.parse("2026-02-01T08:00:00Z")

    private fun tobacco(
        baseline: String = "20",
        packSize: String = "20",
        price: String = "5,40",
        startMinute: Int = 8 * 60,
    ) = HabitDraft(
        id = "h",
        kind = HabitKind.TOBACCO,
        mode = HabitMode.QUIT,
        name = "  Tabaco  ",
        date = today,
        startMinute = startMinute,
        baselineText = baseline,
        period = BaselinePeriod.DAY,
        packSizeText = packSize,
        priceText = price,
        currency = "EUR",
        limitText = "",
    )

    private fun valid(draft: HabitDraft) = (draft.validate(zone, now) as DraftResult.Valid).value

    private fun issues(draft: HabitDraft) = draft.validate(zone, now).issues()

    @Test
    fun `a tobacco draft converts the price to minor units without doubles`() {
        val habit = valid(tobacco())

        assertEquals(540L, habit.packPrice.amountMinor)
        assertEquals(CurrencyCode.EUR, habit.packPrice.currency)
        assertEquals(20, habit.baselineUnits)
        assertEquals(20, habit.unitsPerPack)
        assertEquals("Tabaco", habit.name)
        assertEquals(zone, habit.zone)
        assertEquals(today.atTime(8, 0).atZone(zone).toInstant(), habit.startedAt)
        assertNull(habit.weeklyLimit)
    }

    @Test
    fun `alcohol counts one unit per drink and keeps the weekly limit in reduction mode`() {
        val draft =
            tobacco().copy(
                kind = HabitKind.ALCOHOL,
                mode = HabitMode.REDUCE,
                period = BaselinePeriod.WEEK,
                baselineText = "14",
                priceText = "4,50",
                limitText = "4",
            )

        val habit = valid(draft)

        assertEquals(1, habit.unitsPerPack)
        assertEquals(HabitMode.REDUCE, habit.mode)
        assertEquals(4, habit.weeklyLimit)
        assertEquals(450L, habit.packPrice.amountMinor)
    }

    @Test
    fun `tobacco ignores the reduction mode and the limit`() {
        val habit = valid(tobacco().copy(mode = HabitMode.REDUCE, limitText = "3"))

        assertEquals(HabitMode.QUIT, habit.mode)
        assertNull(habit.weeklyLimit)
    }

    @Test
    fun `reduction needs a weekly limit and zero is allowed`() {
        val reducing = tobacco().copy(kind = HabitKind.ALCOHOL, mode = HabitMode.REDUCE)

        assertEquals(setOf(DraftIssue.LIMIT_MISSING), issues(reducing))
        assertEquals(setOf(DraftIssue.LIMIT_INVALID), issues(reducing.copy(limitText = "1001")))
        assertEquals(0, valid(reducing.copy(limitText = "0")).weeklyLimit)
    }

    @Test
    fun `required numbers are checked with their own message`() {
        assertEquals(setOf(DraftIssue.BASELINE_MISSING), issues(tobacco(baseline = "")))
        assertEquals(setOf(DraftIssue.BASELINE_INVALID), issues(tobacco(baseline = "0")))
        assertEquals(setOf(DraftIssue.BASELINE_INVALID), issues(tobacco(baseline = "1001")))
        assertEquals(setOf(DraftIssue.PACK_SIZE_MISSING), issues(tobacco(packSize = "")))
        assertEquals(setOf(DraftIssue.PRICE_MISSING), issues(tobacco(price = "")))
    }

    @Test
    fun `a price with more decimals than the currency is rejected and never rounded`() {
        assertEquals(setOf(DraftIssue.PRICE_INVALID), issues(tobacco(price = "5,401")))
        assertEquals(setOf(DraftIssue.PRICE_INVALID), issues(tobacco(price = "abc")))
    }

    @Test
    fun `a price above the maximum is rejected`() {
        val over = (QuitRules.MAX_PACK_PRICE_MINOR / 100 + 1).toString()

        assertEquals(setOf(DraftIssue.PRICE_INVALID), issues(tobacco(price = over)))
        assertEquals(
            QuitRules.MAX_PACK_PRICE_MINOR,
            valid(tobacco(price = (QuitRules.MAX_PACK_PRICE_MINOR / 100).toString())).packPrice.amountMinor,
        )
    }

    @Test
    fun `a currency without decimals only accepts whole amounts`() {
        val yen = tobacco(price = "600").copy(currency = "jpy")

        assertEquals(600L, valid(yen).packPrice.amountMinor)
        assertEquals("JPY", valid(yen).packPrice.currency.value)
        assertEquals(setOf(DraftIssue.PRICE_INVALID), issues(yen.copy(priceText = "600,5")))
    }

    @Test
    fun `an invalid currency code is reported`() {
        assertEquals(setOf(DraftIssue.CURRENCY_INVALID), issues(tobacco().copy(currency = "EU")))
    }

    @Test
    fun `the start cannot be in the future`() {
        // Las 18:00 de hoy en Madrid son las 17:00Z, posterior a "now" (15:00Z).
        assertEquals(setOf(DraftIssue.START_IN_FUTURE), issues(tobacco(startMinute = 18 * 60)))
        assertTrue(issues(tobacco(startMinute = 16 * 60)).isEmpty())
    }

    @Test
    fun `an overlong name is trimmed to the limit`() {
        val habit = valid(tobacco().copy(name = "x".repeat(QuitRules.NAME_MAX_LENGTH + 40)))

        assertEquals(QuitRules.NAME_MAX_LENGTH, habit.name.length)
    }

    @Test
    fun `a stored habit comes back as the same draft`() {
        val habit = valid(tobacco())

        assertEquals(habit, valid(habit.toDraft()))
    }

    // --- Antojos y consumos ---

    private fun craving(
        minute: Int = 9 * 60,
        date: LocalDate = today,
        note: String = "",
    ) = CravingDraft("c", "h", date, minute, 4, CravingTrigger.COFFEE, null, note)

    private fun consumption(
        units: String = "2",
        minute: Int = 9 * 60,
        date: LocalDate = today,
    ) = ConsumptionDraft("s", "h", date, minute, units, null, "")

    @Test
    fun `a craving keeps intensity, trigger and a trimmed note`() {
        val result = craving(note = "  tras el café  ").validate(zone, now, habitStart) as DraftResult.Valid

        assertEquals(4, result.value.intensity)
        assertEquals(CravingTrigger.COFFEE, result.value.trigger)
        assertEquals("tras el café", result.value.note)
        assertEquals(today.atTime(9, 0).atZone(zone).toInstant(), result.value.at)
    }

    @Test
    fun `an out of range intensity is clamped`() {
        val result = craving().copy(intensity = 99).validate(zone, now, habitStart) as DraftResult.Valid

        assertEquals(QuitRules.MAX_INTENSITY, result.value.intensity)
    }

    @Test
    fun `a craving cannot be in the future or before the habit started`() {
        assertEquals(
            setOf(DraftIssue.EVENT_IN_FUTURE),
            craving(minute = 20 * 60).validate(zone, now, habitStart).issues(),
        )
        assertEquals(
            setOf(DraftIssue.EVENT_BEFORE_START),
            craving(date = LocalDate.of(2026, 1, 15)).validate(zone, now, habitStart).issues(),
        )
    }

    @Test
    fun `a consumption needs a whole number of units within the limit`() {
        assertEquals(setOf(DraftIssue.UNITS_MISSING), consumption(units = "").validate(zone, now, habitStart).issues())
        assertEquals(setOf(DraftIssue.UNITS_INVALID), consumption(units = "0").validate(zone, now, habitStart).issues())
        assertEquals(
            setOf(DraftIssue.UNITS_INVALID),
            consumption(units = "1001").validate(zone, now, habitStart).issues(),
        )
        assertTrue(consumption(units = "1000").validate(zone, now, habitStart).issues().isEmpty())
    }

    @Test
    fun `a record at midnight sharp belongs to that day`() {
        val result = craving(minute = 0).validate(zone, now, habitStart) as DraftResult.Valid

        assertEquals(today, result.value.date)
        assertEquals(
            0,
            result.value.at
                .atZone(zone)
                .hour,
        )
    }

    @Test
    fun `a time that does not exist because of the daylight change moves forward`() {
        // El 29 de marzo de 2026 a las 02:30 no existe en Madrid: java.time lo lleva a las 03:30.
        val springForward = LocalDate.of(2026, 3, 29)
        val later = Instant.parse("2026-03-30T00:00:00Z")

        val result = craving(minute = 2 * 60 + 30, date = springForward).validate(zone, later, habitStart)

        val value = (result as DraftResult.Valid).value
        assertEquals(3, value.at.atZone(zone).hour)
        assertEquals(springForward, value.date)
    }

    @Test
    fun `a stored craving and consumption come back as the same drafts`() {
        val craving = (craving(note = "nota").validate(zone, now, habitStart) as DraftResult.Valid).value
        val consumption = (consumption().validate(zone, now, habitStart) as DraftResult.Valid).value

        assertEquals(craving, (craving.toDraft().validate(zone, now, habitStart) as DraftResult.Valid).value)
        assertEquals(consumption, (consumption.toDraft().validate(zone, now, habitStart) as DraftResult.Valid).value)
    }

    @Test
    fun `digits are filtered and capped`() {
        assertEquals("1234", sanitizeDigits("1a2-3 4,5", 4))
        assertEquals("", sanitizeDigits("abc", 4))
        assertNull(parseWholeNumber("12.5"))
        assertEquals(12, parseWholeNumber(" 12 "))
    }
}

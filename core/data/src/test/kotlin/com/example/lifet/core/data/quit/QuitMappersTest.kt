package com.example.lifet.core.data.quit

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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class QuitMappersTest {
    private val zone = ZoneId.of("Asia/Tokyo")
    private val instant = Instant.parse("2026-03-10T15:30:00Z")

    @Test
    fun `a habit survives the round trip with money and zone intact`() {
        val habit =
            QuitHabit(
                id = "h",
                kind = HabitKind.ALCOHOL,
                name = "Alcohol 🍷",
                mode = HabitMode.REDUCE,
                startedAt = instant,
                zone = zone,
                baselineUnits = 14,
                baselinePeriod = BaselinePeriod.WEEK,
                unitsPerPack = 1,
                packPrice = Money(Long.MAX_VALUE, CurrencyCode("JPY")),
                weeklyLimit = 6,
            )

        assertEquals(habit, habit.toEntity().toDomain())
        assertEquals("JPY", habit.toEntity().currency)
        assertEquals(Long.MAX_VALUE, habit.toEntity().packPriceMinor)
    }

    @Test
    fun `a craving keeps its trigger, strategy and note`() {
        val craving =
            QuitCraving("c", "h", instant, zone, 5, CravingTrigger.AFTER_MEAL, CopingStrategy.WALK, "Tras la comida")

        assertEquals(craving, craving.toEntity().toDomain())
        assertEquals(CravingTrigger.AFTER_MEAL, craving.toEntity().cravingTrigger)
    }

    @Test
    fun `a craving without a strategy stays without one`() {
        val craving = QuitCraving("c", "h", instant, zone, 2, CravingTrigger.OTHER)

        assertNull(craving.toEntity().toDomain().strategy)
    }

    @Test
    fun `a consumption keeps its optional trigger`() {
        val withTrigger = QuitConsumption("a", "h", instant, zone, 3, CravingTrigger.SOCIAL, "Cumpleaños")
        val without = QuitConsumption("b", "h", instant, zone, 1)

        assertEquals(withTrigger, withTrigger.toEntity().toDomain())
        assertNull(without.toEntity().toDomain().trigger)
    }
}

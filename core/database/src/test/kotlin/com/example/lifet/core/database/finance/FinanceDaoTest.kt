package com.example.lifet.core.database.finance

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.example.lifet.core.database.LifeTrackerDatabase
import com.example.lifet.core.model.finance.Frequency
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class FinanceDaoTest {
    private lateinit var database: LifeTrackerDatabase
    private lateinit var dao: FinanceDao
    private val today = LocalDate.of(2026, 3, 10)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room.inMemoryDatabaseBuilder(context, LifeTrackerDatabase::class.java).allowMainThreadQueries().build()
        dao = database.financeDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun tx(
        id: String,
        date: LocalDate,
        note: String = "",
        ruleId: String? = null,
    ) = FinanceTransactionEntity(
        id = id,
        accountId = "bank",
        categoryId = "default_groceries",
        amountMinor = -1_250,
        currency = "EUR",
        date = date,
        note = note,
        recurringRuleId = ruleId,
        createdAt = Instant.parse("2026-03-10T09:00:00Z"),
        zoneId = ZoneId.of("Europe/Madrid"),
    )

    private fun rule(generatedThrough: LocalDate?) =
        FinanceRecurringRuleEntity(
            id = "netflix",
            name = "Netflix",
            amountMinor = -1_299,
            currency = "EUR",
            categoryId = "default_subscriptions",
            accountId = "bank",
            frequency = Frequency.MONTHLY,
            startDate = LocalDate.of(2026, 1, 15),
            endDate = null,
            active = true,
            generatedThrough = generatedThrough,
        )

    @Test
    fun `transactions round trip with dates, zone and signed amounts, newest first`() =
        runTest {
            dao.observeTransactions().test {
                assertTrue(awaitItem().isEmpty())
                dao.upsertTransaction(tx("a", today.minusDays(1), note = "Mercadona 🛒"))
                awaitItem()
                dao.upsertTransaction(tx("b", today))
                val items = awaitItem()
                assertEquals(listOf("b", "a"), items.map { it.id })
                assertEquals("Mercadona 🛒", items.last().note)
                assertEquals(-1_250L, items.last().amountMinor)
                assertEquals(ZoneId.of("Europe/Madrid"), items.last().zoneId)
            }
        }

    @Test
    fun `generated inserts never overwrite an edited occurrence`() =
        runTest {
            dao.upsertTransaction(tx("rec_netflix_2026-03-15", today.plusDays(5), note = "editado", ruleId = "netflix"))

            dao.insertGeneratedTransactions(listOf(tx("rec_netflix_2026-03-15", today.plusDays(5), ruleId = "netflix")))

            dao.observeTransactions().test {
                assertEquals("editado", awaitItem().single().note)
            }
        }

    @Test
    fun `saving a rule drops its future occurrences and rewinds generatedThrough to today`() =
        runTest {
            dao.upsertTransaction(tx("past", today.minusDays(20), ruleId = "netflix"))
            dao.upsertTransaction(tx("future", today.plusDays(5), ruleId = "netflix"))

            dao.saveRule(rule(generatedThrough = today.plusDays(60)), today)

            assertEquals(listOf("past"), dao.observeTransactions().first().map { it.id })
            assertEquals(today, dao.rules().single().generatedThrough)
        }

    @Test
    fun `replaceAll wipes previous data`() =
        runTest {
            dao.upsertTransaction(tx("old", today))

            dao.replaceAll(emptyList(), emptyList(), listOf(tx("new", today)), emptyList(), emptyList(), emptyList())

            assertEquals(listOf("new"), dao.observeTransactions().first().map { it.id })
        }

}

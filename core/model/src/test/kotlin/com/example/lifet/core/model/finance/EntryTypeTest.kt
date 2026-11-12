package com.example.lifet.core.model.finance

import com.example.lifet.core.model.CurrencyCode
import com.example.lifet.core.model.Money
import org.junit.Assert.assertEquals
import org.junit.Test

class EntryTypeTest {
    private val typed = Money(1_250, CurrencyCode.EUR)

    @Test
    fun `expense is stored negative, income and refund positive`() {
        assertEquals(Money(-1_250, CurrencyCode.EUR), EntryType.EXPENSE.signed(typed))
        assertEquals(typed, EntryType.INCOME.signed(typed))
        assertEquals(typed, EntryType.REFUND.signed(typed))
    }

    @Test
    fun `a positive amount on an expense category is a refund`() {
        assertEquals(EntryType.REFUND, EntryType.of(typed, CategoryKind.EXPENSE))
        assertEquals(EntryType.INCOME, EntryType.of(typed, CategoryKind.INCOME))
        assertEquals(EntryType.EXPENSE, EntryType.of(-typed, CategoryKind.EXPENSE))
    }
}

package com.example.lifet.feature.finance

import com.example.lifet.core.model.CurrencyCode
import com.example.lifet.core.model.Money
import com.example.lifet.core.model.finance.Account
import com.example.lifet.core.model.finance.AccountType
import com.example.lifet.core.model.finance.CategoryKind
import com.example.lifet.core.model.finance.DefaultCategory
import com.example.lifet.core.model.finance.EntryType
import com.example.lifet.core.model.finance.FinanceCategory
import com.example.lifet.core.model.finance.Frequency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class DraftsTest {
    private val eur = CurrencyCode.EUR
    private val zone = ZoneId.of("Europe/Madrid")
    private val bank = Account("bank", "Banco", AccountType.BANK, Money.zero(eur))
    private val cash = Account("cash", "Efectivo", AccountType.CASH, Money.zero(eur))
    private val usd = Account("usd", "Dólares", AccountType.BANK, Money.zero(CurrencyCode("USD")))
    private val today = LocalDate.of(2026, 3, 10)

    private fun draft(
        type: EntryType = EntryType.EXPENSE,
        amount: String = "12,50",
        category: String? = DefaultCategory.GROCERIES.id,
        account: String? = "bank",
    ) = TransactionDraft("id", type, amount, account, category, today, " Mercadona ")

    private fun TransactionDraft.valid() = (validate(bank, Instant.EPOCH, zone) as DraftResult.Valid).value

    @Test
    fun `an expense is saved negative with a trimmed note`() {
        val tx = draft().valid()

        assertEquals(Money(-1_250, eur), tx.amount)
        assertEquals("Mercadona", tx.note)
        assertEquals("bank", tx.accountId)
    }

    @Test
    fun `income and refund are saved positive`() {
        assertEquals(Money(1_250, eur), draft(type = EntryType.INCOME).valid().amount)
        assertEquals(Money(1_250, eur), draft(type = EntryType.REFUND).valid().amount)
    }

    @Test
    fun `an empty amount, a zero amount and too many decimals are all rejected`() {
        assertEquals(setOf(DraftIssue.AMOUNT_MISSING), draft(amount = "").issues(bank))
        assertEquals(setOf(DraftIssue.AMOUNT_INVALID), draft(amount = "0").issues(bank))
        assertEquals(setOf(DraftIssue.AMOUNT_INVALID), draft(amount = "1,999").issues(bank))
    }

    @Test
    fun `missing category and account are reported together`() {
        val issues = draft(category = null, account = null).issues(null)

        assertEquals(setOf(DraftIssue.ACCOUNT_MISSING, DraftIssue.CATEGORY_MISSING), issues)
    }

    @Test
    fun `a huge amount that overflows a Long is rejected instead of wrapping`() {
        assertEquals(setOf(DraftIssue.AMOUNT_INVALID), draft(amount = "99999999999999999999").issues(bank))
    }

    @Test
    fun `an amount that fits a Long but could overflow when summed is rejected`() {
        // 5·10^16 € cabe en un Long, pero dos movimientos así desbordan al sumarse y tumbaban el resumen entero.
        assertEquals(setOf(DraftIssue.AMOUNT_INVALID), draft(amount = "50000000000000000").issues(bank))
    }

    @Test
    fun `an amount exactly at the limit is accepted and one cent above is rejected`() {
        assertTrue(draft(amount = "10000000000").issues(bank).isEmpty())
        assertEquals(setOf(DraftIssue.AMOUNT_INVALID), draft(amount = "10000000000,01").issues(bank))
    }

    private fun transfer(
        from: String? = "bank",
        to: String? = "cash",
        amount: String = "20",
    ) = TransferDraft("t", from, to, amount, today, "")

    @Test
    fun `a transfer needs two different accounts in the same currency`() {
        assertTrue(transfer().issues(bank, cash).isEmpty())
        assertEquals(setOf(DraftIssue.SAME_ACCOUNT), transfer(to = "bank").issues(bank, bank))
        assertEquals(setOf(DraftIssue.CURRENCY_MISMATCH), transfer(to = "usd").issues(bank, usd))
        assertEquals(setOf(DraftIssue.ACCOUNT_MISSING), transfer(to = null).issues(bank, null))
    }

    @Test
    fun `an account needs a name and a valid currency`() {
        val ok = AccountDraft("a", "Ahorro", AccountType.SAVINGS, "1.500,00".replace(".", ""), "eur", false)
        val saved = (ok.validate(null, 3) as DraftResult.Valid).value
        assertEquals(Money(150_000, eur), saved.openingBalance)
        assertEquals(3, saved.sortOrder)

        val bad = AccountDraft("a", " ", AccountType.BANK, "", "EURO", false)
        val issues = (bad.validate(null, 0) as DraftResult.Invalid).issues
        assertEquals(setOf(DraftIssue.NAME_MISSING, DraftIssue.CURRENCY_INVALID), issues)
    }

    @Test
    fun `a default category with a blank name goes back to its localized name`() {
        val default =
            FinanceCategory(
                "default_leisure",
                CategoryKind.EXPENSE,
                "celebration",
                defaultCategory = DefaultCategory.LEISURE,
            )
        val reset = CategoryDraft(default.id, default.kind, "  ", false)
        assertEquals(null, (reset.validate(default, 0) as DraftResult.Valid).value.customName)

        val custom = CategoryDraft("new", CategoryKind.EXPENSE, "  ", false)
        assertEquals(setOf(DraftIssue.NAME_MISSING), (custom.validate(null, 0) as DraftResult.Invalid).issues)
    }

    @Test
    fun `a rule keeps its progress when edited`() {
        val draft =
            RuleDraft(
                "r",
                EntryType.EXPENSE,
                "Netflix",
                "12,99",
                "bank",
                DefaultCategory.SUBSCRIPTIONS.id,
                Frequency.MONTHLY,
                today,
                true,
            )
        val existing = (draft.validate(bank, null) as DraftResult.Valid).value.copy(generatedThrough = today)

        val edited = (draft.copy(name = "Netflix 4K").validate(bank, existing) as DraftResult.Valid).value

        assertEquals(today, edited.generatedThrough)
        assertEquals(Money(-1_299, eur), edited.amount)
    }
}

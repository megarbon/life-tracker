package com.example.lifet.feature.finance

import androidx.compose.runtime.Composable
import com.example.lifet.core.designsystem.preview.LTPreviews
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.model.CurrencyCode
import com.example.lifet.core.model.Money
import com.example.lifet.core.model.finance.Account
import com.example.lifet.core.model.finance.AccountType
import com.example.lifet.core.model.finance.Budget
import com.example.lifet.core.model.finance.DefaultCategory
import com.example.lifet.core.model.finance.FinanceCalculator
import com.example.lifet.core.model.finance.FinanceDataSet
import com.example.lifet.core.model.finance.FinanceOverview
import com.example.lifet.core.model.finance.FinanceTransaction
import com.example.lifet.core.model.finance.LedgerFilter
import com.example.lifet.core.model.finance.defaultFinanceCategories
import kotlinx.coroutines.flow.emptyFlow
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

private const val BASE_GROCERIES_CENTS = 18_000L
private const val GROCERIES_STEP_CENTS = 2_500L
private const val BASE_LEISURE_CENTS = 6_500L
private const val LEISURE_STEP_CENTS = 900L
private const val PREVIEW_MONTHS = 6L
private const val OPENING_CENTS = 150_000L
private const val GROCERIES_BUDGET_CENTS = 35_000L
private const val LEISURE_BUDGET_CENTS = 8_000L

private val PREVIEW_TODAY: LocalDate = LocalDate.of(2026, 3, 18)
private val PREVIEW_ZONE: ZoneId = ZoneId.of("Europe/Madrid")
private val EUR = CurrencyCode.EUR

private fun previewTransaction(
    id: String,
    category: DefaultCategory,
    cents: Long,
    date: LocalDate,
    note: String,
) = FinanceTransaction(
    id = id,
    accountId = Account.DEFAULT_ID,
    categoryId = category.id,
    amount = Money(cents, EUR),
    date = date,
    note = note,
    createdAt = Instant.parse("2026-03-18T08:00:00Z").plusSeconds(date.toEpochDay()),
    zone = PREVIEW_ZONE,
)

private fun previewData(withTransactions: Boolean): FinanceDataSet {
    val march = YearMonth.of(2026, 3)
    val transactions =
        if (withTransactions) {
            (0L until PREVIEW_MONTHS).flatMap { back ->
                val month = march.minusMonths(back)
                listOf(
                    previewTransaction("s$back", DefaultCategory.SALARY, 210_000, month.atDay(1), "Nómina"),
                    previewTransaction("r$back", DefaultCategory.HOUSING, -75_000, month.atDay(5), "Alquiler"),
                    previewTransaction(
                        "g$back",
                        DefaultCategory.GROCERIES,
                        -(BASE_GROCERIES_CENTS + back * GROCERIES_STEP_CENTS),
                        month.atDay(9),
                        "Mercadona",
                    ),
                    previewTransaction(
                        "l$back",
                        DefaultCategory.LEISURE,
                        -(BASE_LEISURE_CENTS + back * LEISURE_STEP_CENTS),
                        month.atDay(12),
                        "Cine 🎬",
                    ),
                )
            } +
                listOf(
                    previewTransaction(
                        "n",
                        DefaultCategory.SUBSCRIPTIONS,
                        -1_299,
                        PREVIEW_TODAY.plusDays(4),
                        "Netflix",
                    ),
                    previewTransaction("t", DefaultCategory.GROCERIES, -2_340, PREVIEW_TODAY, "Frutería"),
                )
        } else {
            emptyList()
        }
    return FinanceDataSet(
        accounts = listOf(Account(Account.DEFAULT_ID, "Cuenta corriente", AccountType.BANK, Money(OPENING_CENTS, EUR))),
        categories = defaultFinanceCategories(),
        transactions = transactions,
        budgets =
            listOf(
                Budget(DefaultCategory.GROCERIES.id, Money(GROCERIES_BUDGET_CENTS, EUR)),
                Budget(DefaultCategory.LEISURE.id, Money(LEISURE_BUDGET_CENTS, EUR)),
            ),
    )
}

private fun previewOverview(withTransactions: Boolean): FinanceOverview =
    FinanceCalculator(previewData(withTransactions), EUR, PREVIEW_TODAY)
        .overview(YearMonth.from(PREVIEW_TODAY)) {
            it.defaultCategory
                ?.name
                ?.lowercase()
                .orEmpty()
        }

@Composable
private fun PreviewScreen(state: FinanceUiState) {
    LifeTrackerTheme {
        FinanceScreen(state = state, editor = null, effects = emptyFlow(), onAction = {})
    }
}

@LTPreviews
@Composable
private fun FinanceSuccessPreview() {
    PreviewScreen(FinanceUiState.Success(previewOverview(withTransactions = true), LedgerFilter()))
}

@LTPreviews
@Composable
private fun FinanceEmptyPreview() {
    PreviewScreen(FinanceUiState.Empty(previewOverview(withTransactions = false), LedgerFilter()))
}

@LTPreviews
@Composable
private fun FinanceLoadingPreview() {
    PreviewScreen(FinanceUiState.Loading)
}

@LTPreviews
@Composable
private fun FinanceErrorPreview() {
    PreviewScreen(FinanceUiState.Error(R.string.finance_error_load))
}

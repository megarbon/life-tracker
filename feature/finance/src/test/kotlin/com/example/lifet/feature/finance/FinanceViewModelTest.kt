package com.example.lifet.feature.finance

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.lifet.core.data.finance.FinanceRepository
import com.example.lifet.core.domain.finance.ObserveFinanceOverview
import com.example.lifet.core.domain.finance.SyncRecurringTransactions
import com.example.lifet.core.model.CurrencyCode
import com.example.lifet.core.model.Money
import com.example.lifet.core.model.finance.Account
import com.example.lifet.core.model.finance.AccountType
import com.example.lifet.core.model.finance.DefaultCategory
import com.example.lifet.core.model.finance.EntryType
import com.example.lifet.core.model.finance.FinanceDataSet
import com.example.lifet.core.model.finance.FinanceTransaction
import com.example.lifet.core.model.finance.LedgerFilter
import com.example.lifet.core.model.finance.defaultFinanceCategories
import com.example.lifet.core.testing.MainDispatcherRule
import com.example.lifet.core.testing.TestClock
import com.example.lifet.core.testing.finance.InMemoryFinanceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class FinanceViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = TestClock.fixed("2026-03-10T09:00:00Z")
    private val today = LocalDate.of(2026, 3, 10)
    private val eur = CurrencyCode.EUR
    private val bank = Account(Account.DEFAULT_ID, "Banco", AccountType.BANK, Money(100_000, eur))

    private fun tx(
        id: String,
        cents: Long,
        category: DefaultCategory,
        date: LocalDate = today,
    ) = FinanceTransaction(
        id = id,
        accountId = bank.id,
        categoryId = category.id,
        amount = Money(cents, eur),
        date = date,
        note = id,
        createdAt = Instant.EPOCH,
        zone = clock.zone,
    )

    private fun data(vararg transactions: FinanceTransaction) =
        FinanceDataSet(
            accounts = listOf(bank),
            categories = defaultFinanceCategories(),
            transactions = transactions.toList(),
        )

    private fun viewModel(repository: FinanceRepository): FinanceViewModel =
        FinanceViewModel(
            observeOverview = ObserveFinanceOverview(repository, clock),
            syncRecurring = SyncRecurringTransactions(repository, clock),
            repository = repository,
            writer = FinanceWriter(repository, FormSaver(repository, clock), clock),
            clock = clock,
            savedState = SavedStateHandle(),
        )

    private fun draft(amount: String = "12,50") =
        TransactionDraft("new", EntryType.EXPENSE, amount, bank.id, DefaultCategory.GROCERIES.id, today, "Café")

    @Test
    fun `emits Loading and then Success when there are movements`() =
        runTest {
            val viewModel = viewModel(InMemoryFinanceRepository(data(tx("a", -1_000, DefaultCategory.GROCERIES))))

            viewModel.uiState.test {
                assertEquals(FinanceUiState.Loading, awaitItem())
                val success = awaitItem() as FinanceUiState.Success
                assertEquals(YearMonth.of(2026, 3), success.overview.month)
                assertEquals(Money(1_000, eur), success.overview.summary.spent)
            }
        }

    @Test
    fun `emits Empty when nothing has been recorded`() =
        runTest {
            val viewModel = viewModel(InMemoryFinanceRepository(data()))

            viewModel.uiState.test {
                assertEquals(FinanceUiState.Loading, awaitItem())
                assertTrue(awaitItem() is FinanceUiState.Empty)
            }
        }

    @Test
    fun `emits Error when the data source fails and recovers on retry`() =
        runTest {
            var failing = true
            val failingRepository =
                object : FinanceRepository by InMemoryFinanceRepository(
                    data(tx("a", -1_000, DefaultCategory.GROCERIES)),
                ) {
                    override fun observeData(): Flow<FinanceDataSet> =
                        flow {
                            if (failing) error("boom")
                            emit(data(tx("a", -1_000, DefaultCategory.GROCERIES)))
                        }
                }
            val viewModel = viewModel(failingRepository)

            viewModel.uiState.test {
                assertEquals(FinanceUiState.Loading, awaitItem())
                assertEquals(FinanceUiState.Error(R.string.finance_error_load), awaitItem())
                failing = false
                viewModel.onAction(FinanceAction.Retry)
                assertTrue(awaitItem() is FinanceUiState.Success)
            }
        }

    @Test
    fun `selecting a month changes the overview and survives in the saved state`() =
        runTest {
            val viewModel = viewModel(InMemoryFinanceRepository(data(tx("a", -1_000, DefaultCategory.GROCERIES))))

            viewModel.uiState.test {
                skipItems(2)
                viewModel.onAction(FinanceAction.SelectMonth(YearMonth.of(2026, 2)))
                assertEquals(YearMonth.of(2026, 2), (awaitItem() as FinanceUiState.Success).overview.month)
            }
        }

    @Test
    fun `a filter that matches nothing keeps Success with an empty ledger`() =
        runTest {
            val viewModel = viewModel(InMemoryFinanceRepository(data(tx("a", -1_000, DefaultCategory.GROCERIES))))

            viewModel.uiState.test {
                skipItems(2)
                viewModel.onAction(FinanceAction.SetFilter(LedgerFilter(query = "zzz")))
                val state = awaitItem() as FinanceUiState.Success
                assertTrue(state.overview.ledger.isEmpty())
                assertTrue(state.filter.isActive)
            }
        }

    @Test
    fun `saving a valid form stores the movement closes the sheet and emits Saved`() =
        runTest {
            val repository = InMemoryFinanceRepository(data())
            val viewModel = viewModel(repository)

            viewModel.effects.test {
                viewModel.onAction(FinanceAction.Open(FinanceEditor.EditTransaction()))
                viewModel.onAction(FinanceAction.Save(FinanceForm.Transaction(draft(), existing = null)))
                advanceUntilIdle()

                assertEquals(FinanceEffect.Saved, awaitItem())
                assertNull(viewModel.editor.value)
                assertEquals(
                    Money(-1_250, eur),
                    repository.current.transactions
                        .single { it.id == "new" }
                        .amount,
                )
            }
        }

    @Test
    fun `saving the same draft twice updates instead of duplicating`() =
        runTest {
            val repository = InMemoryFinanceRepository(data())
            val viewModel = viewModel(repository)

            viewModel.onAction(FinanceAction.Save(FinanceForm.Transaction(draft(), null)))
            viewModel.onAction(FinanceAction.Save(FinanceForm.Transaction(draft("20"), null)))
            advanceUntilIdle()

            assertEquals(1, repository.current.transactions.count { it.id == "new" })
            assertEquals(
                Money(-2_000, eur),
                repository.current.transactions
                    .single { it.id == "new" }
                    .amount,
            )
        }

    @Test
    fun `an invalid form reports an error and writes nothing`() =
        runTest {
            val repository = InMemoryFinanceRepository(data())
            val viewModel = viewModel(repository)

            viewModel.effects.test {
                viewModel.onAction(FinanceAction.Save(FinanceForm.Transaction(draft(amount = ""), null)))
                advanceUntilIdle()

                assertEquals(FinanceEffect.Message(R.string.finance_error_invalid), awaitItem())
                assertTrue(repository.current.transactions.none { it.id == "new" })
            }
        }

    @Test
    fun `a storage failure is reported and the sheet stays open`() =
        runTest {
            val repository = InMemoryFinanceRepository(data()).apply { failWrites = true }
            val viewModel = viewModel(repository)

            viewModel.effects.test {
                viewModel.onAction(FinanceAction.Open(FinanceEditor.EditTransaction()))
                viewModel.onAction(FinanceAction.Save(FinanceForm.Transaction(draft(), null)))
                advanceUntilIdle()

                assertEquals(FinanceEffect.Message(R.string.finance_error_save), awaitItem())
                assertTrue(viewModel.editor.value is FinanceEditor.EditTransaction)
            }
        }

    @Test
    fun `deleting offers undo and undo restores the movement`() =
        runTest {
            val original = tx("a", -1_000, DefaultCategory.GROCERIES)
            val repository = InMemoryFinanceRepository(data(original))
            val viewModel = viewModel(repository)

            viewModel.effects.test {
                viewModel.onAction(FinanceAction.Delete(FinanceItem.Transaction(original)))
                advanceUntilIdle()
                val deleted = awaitItem() as FinanceEffect.Deleted
                assertTrue(repository.current.transactions.isEmpty())

                viewModel.onAction(FinanceAction.Undo(deleted.item))
                advanceUntilIdle()

                assertEquals(listOf(original), repository.current.transactions)
            }
        }

    @Test
    fun `a budget is stored and an empty amount removes it`() =
        runTest {
            val repository = InMemoryFinanceRepository(data())
            val viewModel = viewModel(repository)
            val groceries = DefaultCategory.GROCERIES.id

            viewModel.onAction(FinanceAction.Save(FinanceForm.Budget(groceries, "350")))
            advanceUntilIdle()
            assertEquals(
                Money(35_000, eur),
                repository.current.budgets
                    .single()
                    .limit,
            )

            viewModel.onAction(FinanceAction.Save(FinanceForm.Budget(groceries, "")))
            advanceUntilIdle()
            assertTrue(repository.current.budgets.isEmpty())
        }
}

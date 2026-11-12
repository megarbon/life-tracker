package com.example.lifet.core.model.finance

import com.example.lifet.core.model.CurrencyCode
import com.example.lifet.core.model.DateRange
import com.example.lifet.core.model.Money
import java.time.LocalDate
import java.time.YearMonth

/**
 * Cálculos de finanzas como funciones puras sobre un [FinanceDataSet].
 *
 * Reglas comunes:
 * - Sólo se agregan importes en la divisa principal [currency]; el resto se informa aparte (no hay conversión).
 * - Confirmado = fecha <= hoy; previsto = fecha > hoy.
 * - El gasto es neto de devoluciones (importes positivos en categorías de gasto).
 * - Las transferencias nunca son gasto ni ingreso, sólo mueven saldo entre cuentas.
 */
@Suppress("TooManyFunctions") // Un único punto de cálculo sobre el mismo conjunto de datos.
public class FinanceCalculator(
    private val data: FinanceDataSet,
    private val currency: CurrencyCode,
    private val today: LocalDate,
) {
    private val categoriesById = data.categories.associateBy { it.id }
    private val inCurrency = data.transactions.filter { it.amount.currency == currency }
    private val zero = Money.zero(currency)

    /** Divisas con movimientos que no se pueden sumar a la principal. */
    public val otherCurrencies: Set<CurrencyCode> =
        (data.transactions.map { it.amount.currency } + data.accounts.map { it.currency })
            .filterTo(mutableSetOf()) { it != currency }

    public fun kindOf(transaction: FinanceTransaction): CategoryKind =
        categoriesById[transaction.categoryId]?.kind
            ?: if (transaction.amount.isNegative) CategoryKind.EXPENSE else CategoryKind.INCOME

    public fun monthSummary(month: YearMonth): MonthSummary {
        val ofMonth = inCurrency.filter { YearMonth.from(it.date) == month }
        val (planned, confirmed) = ofMonth.partition { it.isPlannedAt(today) }
        val income = incomeOf(confirmed)
        val spent = spentOf(confirmed)
        val plannedIncome = incomeOf(planned)
        val plannedSpent = spentOf(planned)
        val byCategory = categorySpend(confirmed)
        val budgeted = byCategory.filter { it.budget != null }
        return MonthSummary(
            month = month,
            currency = currency,
            income = income,
            spent = spent,
            plannedIncome = plannedIncome,
            plannedSpent = plannedSpent,
            available = income + plannedIncome - spent - plannedSpent,
            budgetTotal = budgeted.mapNotNull { it.budget }.sumOrNull(),
            budgetSpent = budgeted.map { it.spent }.sumOrZero(),
            byCategory = byCategory,
        )
    }

    /** Ingresos y gasto confirmados de los [count] meses hasta [lastMonth], del más antiguo al más reciente. */
    public fun monthlyTotals(
        lastMonth: YearMonth,
        count: Int,
    ): List<MonthTotals> {
        val confirmedByMonth = inCurrency.filterNot { it.isPlannedAt(today) }.groupBy { YearMonth.from(it.date) }
        return (count - 1 downTo 0).map { back ->
            val month = lastMonth.minusMonths(back.toLong())
            val ofMonth = confirmedByMonth[month].orEmpty()
            MonthTotals(month, incomeOf(ofMonth), spentOf(ofMonth))
        }
    }

    /** Saldo total confirmado al cierre de cada día de [from]..[toInclusive] (los días futuros no se incluyen). */
    public fun dailyBalances(
        from: LocalDate,
        toInclusive: LocalDate,
    ): List<DayBalance> {
        val last = minOf(toInclusive, today)
        if (from > last) return emptyList()
        val deltas = dailyDeltas()
        var running = openingTotal() + deltas.filterKeys { it < from }.values.sumOrZero()
        return generateSequence(from) { it.plusDays(1) }
            .takeWhile { it <= last }
            .map { day ->
                running += deltas[day] ?: zero
                DayBalance(day, running)
            }.toList()
    }

    /**
     * Saldo total al cierre de cada uno de los [count] meses que terminan en [lastMonth] (del más antiguo al
     * más reciente). El mes en curso usa el saldo de hoy; un mes futuro no tiene saldo confirmado y se omite.
     */
    public fun monthEndBalances(
        lastMonth: YearMonth,
        count: Int,
    ): List<DayBalance> =
        (count - 1 downTo 0).mapNotNull { back ->
            val month = lastMonth.minusMonths(back.toLong())
            val day = minOf(month.atEndOfMonth(), today)
            dailyBalances(day, day).firstOrNull()
        }

    /** Saldo total confirmado hoy (sólo cuentas en la divisa principal). */
    public fun totalBalance(): Money = dailyBalances(today, today).firstOrNull()?.balance ?: zero

    /** Saldo de cada cuenta en su propia divisa (movimientos confirmados y transferencias). */
    public fun accountBalances(): List<AccountBalance> =
        data.accounts.sortedWith(compareBy({ it.archived }, { it.sortOrder }, { it.name })).map { account ->
            val own = account.currency
            val txs =
                data.transactions
                    .filter { it.accountId == account.id && it.amount.currency == own && !it.isPlannedAt(today) }
            val transfers = data.transfers.filter { it.amount.currency == own && it.date <= today }
            val incoming = transfers.filter { it.toAccountId == account.id }.map { it.amount }
            val outgoing = transfers.filter { it.fromAccountId == account.id }.map { -it.amount }
            val moves = txs.map { it.amount } + incoming + outgoing
            AccountBalance(account, moves.fold(account.openingBalance) { acc, money -> acc + money })
        }

    /**
     * Movimientos y transferencias agrupados por día (el más reciente primero). Con búsqueda de texto se
     * recorre todo el histórico; si no, sólo [month].
     */
    public fun ledger(
        month: YearMonth,
        filter: LedgerFilter = LedgerFilter(),
        categoryName: (FinanceCategory) -> String = { it.customName.orEmpty() },
    ): List<DayGroup> {
        val inScope: (LocalDate) -> Boolean = { filter.searchesHistory || YearMonth.from(it) == month }
        val transactions =
            data.transactions
                .filter { inScope(it.date) && matches(it, filter, categoryName) }
                .map(::TransactionEntry)
        val transfers =
            data.transfers
                .filter { inScope(it.date) && matchesTransfer(it, filter) }
                .map(::TransferEntry)
        return (transactions + transfers)
            .groupBy { it.date }
            .entries
            .sortedByDescending { it.key }
            .map { (date, entries) ->
                val amounts =
                    entries
                        .filterIsInstance<TransactionEntry>()
                        .map { it.transaction.amount }
                        .filter { it.currency == currency }
                DayGroup(date, entries.sortedByDescending { it.createdAt }, amounts.sumOrNull())
            }
    }

    /** Próximos movimientos previstos (fecha > hoy) en los siguientes [days] días, del más cercano al más lejano. */
    public fun upcoming(days: Long = UPCOMING_DAYS): List<FinanceTransaction> {
        val horizon = today.plusDays(days)
        return data.transactions.filter { it.isPlannedAt(today) && it.date <= horizon }.sortedBy { it.date }
    }

    /** Últimos movimientos distintos (por nota y categoría) para repetirlos con un toque. */
    public fun recentTemplates(limit: Int = RECENT_LIMIT): List<FinanceTransaction> =
        data.transactions
            .filter { !it.isPlannedAt(today) && it.recurringRuleId == null && it.note.isNotBlank() }
            .sortedByDescending { it.createdAt }
            .distinctBy { it.note.trim().lowercase() to it.categoryId }
            .take(limit)

    /** Todo lo que pinta la pantalla de Finanzas para [month]: resumen, gráficas, listado y catálogos. */
    public fun overview(
        month: YearMonth,
        filter: LedgerFilter = LedgerFilter(),
        categoryName: (FinanceCategory) -> String = { it.customName.orEmpty() },
    ): FinanceOverview =
        FinanceOverview(
            currency = currency,
            month = month,
            today = today,
            summary = monthSummary(month),
            monthlyTotals = monthlyTotals(month, TREND_MONTHS),
            balanceTrend = monthEndBalances(month, TREND_MONTHS),
            ledger = ledger(month, filter, categoryName),
            upcoming = upcoming(),
            accounts = accountBalances(),
            categories = data.categories.sortedBy { it.sortOrder },
            rules = data.rules.sortedBy { it.name },
            budgets = data.budgets,
            recentTemplates = recentTemplates(),
            otherCurrencies = otherCurrencies,
            hasAnyEntry = data.transactions.isNotEmpty() || data.transfers.isNotEmpty(),
        )

    public fun todaySummary(): FinanceTodaySummary {
        val summary = monthSummary(YearMonth.from(today))
        return FinanceTodaySummary(
            currency = currency,
            spentToday = spentOf(inCurrency.filter { it.date == today }),
            availableThisMonth = summary.available,
            budgetRemaining = summary.budgetRemaining,
            hasData = data.transactions.isNotEmpty(),
        )
    }

    /** Serie diaria (ingresos, gasto neto y saldo confirmados) de [range]. */
    public fun series(range: DateRange): FinanceSeries {
        val byDay = inCurrency.filterNot { it.isPlannedAt(today) }.groupBy { it.date }
        val balances = dailyBalances(range.start, range.endInclusive).associateBy { it.date }
        var lastBalance = balances.values.firstOrNull()?.balance ?: zero
        val days =
            range.days.filter { it <= today }.map { day ->
                val ofDay = byDay[day].orEmpty()
                lastBalance = balances[day]?.balance ?: lastBalance
                FinanceDayPoint(day, incomeOf(ofDay), spentOf(ofDay), lastBalance)
            }
        return FinanceSeries(range, currency, days)
    }

    private fun categorySpend(confirmed: List<FinanceTransaction>): List<CategorySpend> {
        val spentByCategory =
            confirmed
                .filter { kindOf(it) == CategoryKind.EXPENSE }
                .groupBy { it.categoryId }
                .mapValues { (_, txs) -> -txs.map { it.amount }.sumOrZero() }
        val budgets = data.budgets.filter { it.limit.currency == currency }.associateBy { it.categoryId }
        return data.categories
            .filter { it.kind == CategoryKind.EXPENSE }
            .filter { it.id in spentByCategory || it.id in budgets }
            .map { CategorySpend(it, spentByCategory[it.id] ?: zero, budgets[it.id]?.limit) }
            .sortedByDescending { it.spent.amountMinor }
    }

    private fun incomeOf(transactions: List<FinanceTransaction>): Money =
        transactions.filter { kindOf(it) == CategoryKind.INCOME }.map { it.amount }.sumOrZero()

    private fun spentOf(transactions: List<FinanceTransaction>): Money =
        -transactions.filter { kindOf(it) == CategoryKind.EXPENSE }.map { it.amount }.sumOrZero()

    private fun openingTotal(): Money =
        data.accounts
            .filter { it.currency == currency }
            .map { it.openingBalance }
            .sumOrZero()

    // Las transferencias entre cuentas de la divisa principal se anulan en el total; sólo cuentan los movimientos.
    private fun dailyDeltas(): Map<LocalDate, Money> {
        val accountIds = data.accounts.filter { it.currency == currency }.mapTo(mutableSetOf()) { it.id }
        return inCurrency
            .filter { it.accountId in accountIds && !it.isPlannedAt(today) }
            .groupBy { it.date }
            .mapValues { (_, txs) -> txs.map { it.amount }.sumOrZero() }
    }

    private fun matches(
        transaction: FinanceTransaction,
        filter: LedgerFilter,
        categoryName: (FinanceCategory) -> String,
    ): Boolean {
        val category = categoriesById[transaction.categoryId]
        val query = filter.query.trim()
        val matchesQuery =
            query.isEmpty() ||
                transaction.note.contains(query, ignoreCase = true) ||
                category?.let { categoryName(it).contains(query, ignoreCase = true) } == true
        return matchesQuery &&
            (filter.kind == null || kindOf(transaction) == filter.kind) &&
            (filter.categoryIds.isEmpty() || transaction.categoryId in filter.categoryIds) &&
            (filter.accountIds.isEmpty() || transaction.accountId in filter.accountIds)
    }

    // Las transferencias no tienen categoría: se ocultan si se filtra por tipo o categoría.
    private fun matchesTransfer(
        transfer: FinanceTransfer,
        filter: LedgerFilter,
    ): Boolean {
        val query = filter.query.trim()
        return filter.kind == null &&
            filter.categoryIds.isEmpty() &&
            (query.isEmpty() || transfer.note.contains(query, ignoreCase = true)) &&
            (
                filter.accountIds.isEmpty() ||
                    transfer.fromAccountId in filter.accountIds ||
                    transfer.toAccountId in filter.accountIds
            )
    }

    private fun Collection<Money>.sumOrZero(): Money = fold(zero) { acc, money -> acc + money }

    private fun Collection<Money>.sumOrNull(): Money? = if (isEmpty()) null else sumOrZero()

    public companion object {
        public const val UPCOMING_DAYS: Long = 30L
        public const val RECENT_LIMIT: Int = 6
        public const val TREND_MONTHS: Int = 6
    }
}

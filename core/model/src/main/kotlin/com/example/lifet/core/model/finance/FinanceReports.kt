package com.example.lifet.core.model.finance

import com.example.lifet.core.model.CurrencyCode
import com.example.lifet.core.model.DateRange
import com.example.lifet.core.model.Money
import java.time.LocalDate
import java.time.YearMonth

/** Gasto neto de una categoría en el mes frente a su presupuesto (si lo tiene). */
public data class CategorySpend(
    val category: FinanceCategory,
    val spent: Money,
    val budget: Money?,
) {
    public val remaining: Money? get() = budget?.let { it - spent }
    public val isOver: Boolean get() = budget != null && spent > budget

    /** Fracción gastada del presupuesto (puede superar 1); 0 si no hay presupuesto o el gasto es negativo. */
    public val ratio: Float
        get() {
            val limit = budget?.amountMinor ?: return 0f
            val used = spent.amountMinor.coerceAtLeast(0L)
            return when {
                limit > 0L -> used.toDouble().div(limit).toFloat()
                used > 0L -> 1f
                else -> 0f
            }
        }
}

/**
 * Resumen de un mes en la divisa principal. [spent] es neto de devoluciones.
 * [available] = ingresos (reales + previstos) - gastos (reales + previstos): lo que sobra del mes si se cumple lo previsto.
 */
public data class MonthSummary(
    val month: YearMonth,
    val currency: CurrencyCode,
    val income: Money,
    val spent: Money,
    val plannedIncome: Money,
    val plannedSpent: Money,
    val available: Money,
    val budgetTotal: Money?,
    val budgetSpent: Money,
    val byCategory: List<CategorySpend>,
) {
    public val budgetRemaining: Money? get() = budgetTotal?.let { it - budgetSpent }
}

/** Ingresos y gasto neto confirmados de un mes. */
public data class MonthTotals(
    val month: YearMonth,
    val income: Money,
    val spent: Money,
)

/** Saldo total (suma de cuentas en la divisa principal) al cierre del día [date]. */
public data class DayBalance(
    val date: LocalDate,
    val balance: Money,
)

/** Un día con sus movimientos. [total] es el neto confirmado en la divisa principal, o `null` si no hay ninguno. */
public data class DayGroup(
    val date: LocalDate,
    val entries: List<LedgerEntry>,
    val total: Money?,
)

/**
 * Filtros del listado. [query] busca en la nota y en el nombre personalizado de la categoría; con texto,
 * la búsqueda abarca todo el histórico en lugar de sólo el mes visible.
 */
public data class LedgerFilter(
    val query: String = "",
    val kind: CategoryKind? = null,
    val categoryIds: Set<String> = emptySet(),
    val accountIds: Set<String> = emptySet(),
) {
    public val searchesHistory: Boolean get() = query.isNotBlank()
    public val isActive: Boolean
        get() = query.isNotBlank() || kind != null || categoryIds.isNotEmpty() || accountIds.isNotEmpty()
}

/** Todo lo que necesita la pantalla de Finanzas para un mes. */
public data class FinanceOverview(
    val currency: CurrencyCode,
    val month: YearMonth,
    val today: LocalDate,
    val summary: MonthSummary,
    val monthlyTotals: List<MonthTotals>,
    val dailyBalances: List<DayBalance>,
    val ledger: List<DayGroup>,
    val upcoming: List<FinanceTransaction>,
    val accounts: List<AccountBalance>,
    val categories: List<FinanceCategory>,
    val rules: List<RecurringRule>,
    val recentTemplates: List<FinanceTransaction>,
    val otherCurrencies: Set<CurrencyCode>,
    val hasAnyEntry: Boolean,
)

/** Lo que la pantalla Hoy necesita de Finanzas. */
public data class FinanceTodaySummary(
    val currency: CurrencyCode,
    val spentToday: Money,
    val availableThisMonth: Money,
    val budgetRemaining: Money?,
    val hasData: Boolean,
)

/** Un día de la serie para Insights. */
public data class FinanceDayPoint(
    val date: LocalDate,
    val income: Money,
    val spent: Money,
    val balance: Money,
)

/** Serie diaria de ingresos, gasto neto y saldo para un [DateRange]. */
public data class FinanceSeries(
    val range: DateRange,
    val currency: CurrencyCode,
    val days: List<FinanceDayPoint>,
) {
    public val totalIncome: Money get() = days.fold(Money.zero(currency)) { acc, day -> acc + day.income }
    public val totalSpent: Money get() = days.fold(Money.zero(currency)) { acc, day -> acc + day.spent }
}

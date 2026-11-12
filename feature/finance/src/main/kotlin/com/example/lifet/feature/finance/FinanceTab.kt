package com.example.lifet.feature.finance

import androidx.annotation.StringRes

/** Pestañas de la pantalla de Finanzas, en el orden en que se muestran. */
internal enum class FinanceTab(
    @param:StringRes val labelRes: Int,
) {
    SUMMARY(R.string.finance_tab_summary),
    LEDGER(R.string.finance_tab_ledger),
    BUDGETS(R.string.finance_tab_budgets),
    ACCOUNTS(R.string.finance_tab_accounts),
}

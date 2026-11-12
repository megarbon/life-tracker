package com.example.lifet.feature.finance

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Commute
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.lifet.core.designsystem.util.fractionDigits
import com.example.lifet.core.model.CurrencyCode
import com.example.lifet.core.model.Money
import com.example.lifet.core.model.finance.AccountType
import com.example.lifet.core.model.finance.DefaultCategory
import com.example.lifet.core.model.finance.EntryType
import com.example.lifet.core.model.finance.Frequency
import com.example.lifet.core.model.finance.MonthTotals
import java.math.BigDecimal
import java.math.RoundingMode

/** Icono de una categoría a partir de su clave estable. Desconocidas usan el genérico. */
internal fun iconFor(iconKey: String): ImageVector =
    when (iconKey) {
        "cart" -> Icons.Rounded.ShoppingCart
        "home" -> Icons.Rounded.Home
        "repeat" -> Icons.Rounded.Repeat
        "restaurant" -> Icons.Rounded.Restaurant
        "celebration" -> Icons.Rounded.Celebration
        "commute" -> Icons.Rounded.Commute
        "health" -> Icons.Rounded.HealthAndSafety
        "bag" -> Icons.Rounded.ShoppingBag
        "work" -> Icons.Rounded.Work
        "savings" -> Icons.Rounded.Savings
        "pets" -> Icons.Rounded.Pets
        else -> Icons.Rounded.Category
    }

@StringRes
internal fun DefaultCategory.labelRes(): Int =
    when (this) {
        DefaultCategory.GROCERIES -> R.string.finance_cat_groceries
        DefaultCategory.HOUSING -> R.string.finance_cat_housing
        DefaultCategory.SUBSCRIPTIONS -> R.string.finance_cat_subscriptions
        DefaultCategory.RESTAURANTS -> R.string.finance_cat_restaurants
        DefaultCategory.LEISURE -> R.string.finance_cat_leisure
        DefaultCategory.TRANSPORT -> R.string.finance_cat_transport
        DefaultCategory.HEALTH -> R.string.finance_cat_health
        DefaultCategory.SHOPPING -> R.string.finance_cat_shopping
        DefaultCategory.OTHER_EXPENSE -> R.string.finance_cat_other_expense
        DefaultCategory.SALARY -> R.string.finance_cat_salary
        DefaultCategory.OTHER_INCOME -> R.string.finance_cat_other_income
    }

@StringRes
internal fun EntryType.labelRes(): Int =
    when (this) {
        EntryType.EXPENSE -> R.string.finance_entry_expense
        EntryType.INCOME -> R.string.finance_entry_income
        EntryType.REFUND -> R.string.finance_entry_refund
    }

@StringRes
internal fun AccountType.labelRes(): Int =
    when (this) {
        AccountType.CASH -> R.string.finance_account_type_cash
        AccountType.BANK -> R.string.finance_account_type_bank
        AccountType.CARD -> R.string.finance_account_type_card
        AccountType.SAVINGS -> R.string.finance_account_type_savings
    }

@StringRes
internal fun Frequency.labelRes(): Int =
    when (this) {
        Frequency.WEEKLY -> R.string.finance_frequency_weekly
        Frequency.MONTHLY -> R.string.finance_frequency_monthly
        Frequency.YEARLY -> R.string.finance_frequency_yearly
    }

/**
 * Cambio porcentual del último mes frente al anterior, redondeado. `null` si no hay base de comparación
 * (el mes anterior no tiene gasto). Trabaja con `Long` para no usar `Double` sobre importes.
 */
internal fun spendChangePercent(totals: List<MonthTotals>): Int? {
    val previous =
        totals
            .dropLast(1)
            .lastOrNull()
            ?.spent
            ?.amountMinor
    val current = totals.lastOrNull()?.spent?.amountMinor
    return if (previous == null || current == null || previous <= 0L) {
        null
    } else {
        ((current - previous) * PERCENT / previous).toInt()
    }
}

private const val PERCENT = 100L

/** Valor de una gráfica en unidades mayores. Sólo para pintar: los importes nunca se calculan en `Double`. */
internal fun Money.toChartValue(): Double =
    BigDecimal.valueOf(amountMinor).movePointLeft(currency.fractionDigits()).toDouble()

/** Inverso de [toChartValue], para formatear los ejes y la tabla con la divisa de la serie. */
internal fun Double.toChartMoney(currency: CurrencyCode): Money {
    val minor = BigDecimal.valueOf(this).movePointRight(currency.fractionDigits()).setScale(0, RoundingMode.HALF_UP)
    return Money(minor.toLong(), currency)
}

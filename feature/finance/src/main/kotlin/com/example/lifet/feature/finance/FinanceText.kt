package com.example.lifet.feature.finance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.example.lifet.core.designsystem.util.format
import com.example.lifet.core.model.Money
import com.example.lifet.core.model.finance.Account
import com.example.lifet.core.model.finance.FinanceCategory
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/** Nombre localizado de la categoría: el del usuario, o el de partida traducido. */
@Composable
internal fun FinanceCategory.displayName(): String =
    customName ?: defaultCategory?.let { stringResource(it.labelRes()) }.orEmpty()

/** Las cuentas de partida se guardan sin nombre para poder mostrarlo en el idioma del dispositivo. */
@Composable
internal fun Account.displayName(): String = name.ifBlank { stringResource(R.string.finance_account_default) }

@Composable
internal fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

@Composable
internal fun rememberMoneyFormatter(): (Money) -> String {
    val locale = currentLocale()
    return remember(locale) { { money -> money.format(locale) } }
}

@Composable
internal fun monthTitle(month: YearMonth): String {
    val locale = currentLocale()
    return remember(month, locale) {
        DateTimeFormatter.ofPattern("LLLL yyyy", locale).format(month).replaceFirstChar { it.titlecase(locale) }
    }
}

@Composable
internal fun shortMonthLabel(month: YearMonth): String {
    val locale = currentLocale()
    return remember(month, locale) { month.month.getDisplayName(TextStyle.SHORT, locale) }
}

@Composable
internal fun fullDate(date: LocalDate): String {
    val locale = currentLocale()
    return remember(date, locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale).format(date)
    }
}

@Composable
internal fun mediumDate(date: LocalDate): String {
    val locale = currentLocale()
    return remember(date, locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(date)
    }
}

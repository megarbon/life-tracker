package com.example.lifet.feature.finance

import com.example.lifet.core.designsystem.util.parseAmount
import com.example.lifet.core.model.CurrencyCode
import com.example.lifet.core.model.Money
import com.example.lifet.core.model.finance.Account
import com.example.lifet.core.model.finance.AccountType
import com.example.lifet.core.model.finance.CategoryKind
import com.example.lifet.core.model.finance.EntryType
import com.example.lifet.core.model.finance.FinanceCategory
import com.example.lifet.core.model.finance.FinanceTransaction
import com.example.lifet.core.model.finance.FinanceTransfer
import com.example.lifet.core.model.finance.Frequency
import com.example.lifet.core.model.finance.RecurringRule
import java.io.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/*
 * Formularios de la UI. Son `Serializable` para que `rememberSaveable` los conserve tras una rotación.
 * Validan en el borde (texto tecleado -> dominio) y sólo entregan al ViewModel objetos ya válidos.
 */

internal enum class DraftIssue {
    AMOUNT_MISSING,
    AMOUNT_INVALID,
    ACCOUNT_MISSING,
    CATEGORY_MISSING,
    SAME_ACCOUNT,
    CURRENCY_MISMATCH,
    CURRENCY_INVALID,
    NAME_MISSING,
}

internal sealed interface DraftResult<out T> {
    data class Valid<out T>(
        val value: T,
    ) : DraftResult<T>

    data class Invalid(
        val issues: Set<DraftIssue>,
    ) : DraftResult<Nothing>
}

/** Problemas del borrador, o ninguno si es válido. */
internal fun <T> DraftResult<T>.issues(): Set<DraftIssue> = (this as? DraftResult.Invalid)?.issues.orEmpty()

/** Acumula problemas mientras se valida; [result] entrega el valor sólo si no hubo ninguno. */
private class Validation {
    private val issues = mutableSetOf<DraftIssue>()

    fun require(
        condition: Boolean,
        issue: DraftIssue,
    ) {
        if (!condition) issues += issue
    }

    /** Importe positivo tecleado en [currency]; anota la incidencia si falta o no es válido. */
    fun amount(
        text: String,
        currency: CurrencyCode?,
    ): Money? {
        val parsed = currency?.let { parseAmount(text, it) }?.takeUnless { it.isZero }
        when {
            currency == null -> Unit
            text.isBlank() -> issues += DraftIssue.AMOUNT_MISSING
            parsed == null -> issues += DraftIssue.AMOUNT_INVALID
        }
        return parsed
    }

    /** [build] devuelve `null` si le falta algún dato; sólo ocurre si ya hay una incidencia anotada. */
    fun <T> result(build: () -> T?): DraftResult<T> {
        val value = if (issues.isEmpty()) build() else null
        return if (value != null) DraftResult.Valid(value) else DraftResult.Invalid(issues.toSet())
    }
}

internal data class TransactionDraft(
    val id: String,
    val type: EntryType,
    val amountText: String,
    val accountId: String?,
    val categoryId: String?,
    val date: LocalDate,
    val note: String,
) : Serializable {
    fun validate(
        account: Account?,
        createdAt: Instant,
        zone: ZoneId,
    ): DraftResult<FinanceTransaction> {
        val check = Validation()
        check.require(account != null, DraftIssue.ACCOUNT_MISSING)
        check.require(categoryId != null, DraftIssue.CATEGORY_MISSING)
        val amount = check.amount(amountText, account?.currency)
        return check.result {
            val accountId = account?.id
            val category = categoryId
            if (accountId == null || category == null || amount == null) {
                null
            } else {
                FinanceTransaction(
                    id = id,
                    accountId = accountId,
                    categoryId = category,
                    amount = type.signed(amount),
                    date = date,
                    note = note.trim(),
                    createdAt = createdAt,
                    zone = zone,
                )
            }
        }
    }

    /** Problemas del formulario, para pintarlos en la hoja antes de enviar nada al ViewModel. */
    fun issues(account: Account?): Set<DraftIssue> = validate(account, Instant.EPOCH, ZoneOffset.UTC).issues()

    companion object {
        private const val serialVersionUID = 1L
    }
}

internal data class TransferDraft(
    val id: String,
    val fromAccountId: String?,
    val toAccountId: String?,
    val amountText: String,
    val date: LocalDate,
    val note: String,
) : Serializable {
    fun validate(
        from: Account?,
        to: Account?,
        createdAt: Instant,
        zone: ZoneId,
    ): DraftResult<FinanceTransfer> {
        val check = Validation()
        check.require(from != null && to != null, DraftIssue.ACCOUNT_MISSING)
        if (from != null && to != null) {
            check.require(from.id != to.id, DraftIssue.SAME_ACCOUNT)
            check.require(from.currency == to.currency, DraftIssue.CURRENCY_MISMATCH)
        }
        val amount = check.amount(amountText, from?.currency)
        return check.result {
            if (from == null || to == null || amount == null) {
                null
            } else {
                FinanceTransfer(id, from.id, to.id, amount, date, note.trim(), createdAt, zone)
            }
        }
    }

    fun issues(
        from: Account?,
        to: Account?,
    ): Set<DraftIssue> = validate(from, to, Instant.EPOCH, ZoneOffset.UTC).issues()

    companion object {
        private const val serialVersionUID = 1L
    }
}

internal data class RuleDraft(
    val id: String,
    val type: EntryType,
    val name: String,
    val amountText: String,
    val accountId: String?,
    val categoryId: String?,
    val frequency: Frequency,
    val startDate: LocalDate,
    val active: Boolean,
) : Serializable {
    /** [existing] conserva el fin de la regla y su progreso de generación al editarla. */
    fun validate(
        account: Account?,
        existing: RecurringRule?,
    ): DraftResult<RecurringRule> {
        val check = Validation()
        check.require(name.isNotBlank(), DraftIssue.NAME_MISSING)
        check.require(account != null, DraftIssue.ACCOUNT_MISSING)
        check.require(categoryId != null, DraftIssue.CATEGORY_MISSING)
        val amount = check.amount(amountText, account?.currency)
        return check.result {
            val accountId = account?.id
            val category = categoryId
            if (accountId == null || category == null || amount == null) {
                null
            } else {
                RecurringRule(
                    id = id,
                    name = name.trim(),
                    amount = type.signed(amount),
                    categoryId = category,
                    accountId = accountId,
                    frequency = frequency,
                    startDate = startDate,
                    endDate = existing?.endDate,
                    active = active,
                    generatedThrough = existing?.generatedThrough,
                )
            }
        }
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}

internal data class AccountDraft(
    val id: String,
    val name: String,
    val type: AccountType,
    val openingText: String,
    val currencyText: String,
    val archived: Boolean,
) : Serializable {
    fun validate(
        existing: Account?,
        nextSortOrder: Int,
    ): DraftResult<Account> {
        val check = Validation()
        check.require(name.isNotBlank(), DraftIssue.NAME_MISSING)
        val currency = runCatching { CurrencyCode(currencyText.trim().uppercase()) }.getOrNull()
        check.require(currency != null, DraftIssue.CURRENCY_INVALID)
        val opening = currency?.let { if (openingText.isBlank()) Money.zero(it) else parseAmount(openingText, it) }
        check.require(currency == null || opening != null, DraftIssue.AMOUNT_INVALID)
        return check.result {
            opening?.let {
                Account(
                    id = id,
                    name = name.trim(),
                    type = type,
                    openingBalance = it,
                    archived = archived,
                    sortOrder = existing?.sortOrder ?: nextSortOrder,
                )
            }
        }
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}

internal data class CategoryDraft(
    val id: String,
    val kind: CategoryKind,
    val name: String,
    val archived: Boolean,
) : Serializable {
    /** Una categoría de partida con el nombre en blanco vuelve a mostrar su nombre localizado. */
    fun validate(
        existing: FinanceCategory?,
        nextSortOrder: Int,
    ): DraftResult<FinanceCategory> {
        val custom = name.trim().takeIf { it.isNotEmpty() }
        val check = Validation()
        check.require(custom != null || existing?.defaultCategory != null, DraftIssue.NAME_MISSING)
        return check.result {
            FinanceCategory(
                id = id,
                kind = existing?.kind ?: kind,
                iconKey = existing?.iconKey ?: DEFAULT_ICON,
                customName = custom,
                defaultCategory = existing?.defaultCategory,
                archived = archived,
                sortOrder = existing?.sortOrder ?: nextSortOrder,
            )
        }
    }

    companion object {
        private const val serialVersionUID = 1L
        private const val DEFAULT_ICON = "category"
    }
}

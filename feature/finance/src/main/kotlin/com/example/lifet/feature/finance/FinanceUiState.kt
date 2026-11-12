package com.example.lifet.feature.finance

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.example.lifet.core.model.finance.Account
import com.example.lifet.core.model.finance.FinanceCategory
import com.example.lifet.core.model.finance.FinanceOverview
import com.example.lifet.core.model.finance.FinanceTransaction
import com.example.lifet.core.model.finance.FinanceTransfer
import com.example.lifet.core.model.finance.LedgerFilter
import com.example.lifet.core.model.finance.RecurringRule
import java.time.YearMonth

/** Estado de la pantalla de Finanzas. [Empty] conserva el resumen para poder configurar cuentas y categorías. */
internal sealed interface FinanceUiState {
    data object Loading : FinanceUiState

    @Immutable
    data class Empty(
        val overview: FinanceOverview,
        val filter: LedgerFilter,
    ) : FinanceUiState

    @Immutable
    data class Success(
        val overview: FinanceOverview,
        val filter: LedgerFilter,
    ) : FinanceUiState

    data class Error(
        @param:StringRes val messageRes: Int,
    ) : FinanceUiState
}

/** Hoja de edición abierta. Vive en el ViewModel para sobrevivir a una rotación. */
internal sealed interface FinanceEditor {
    /** [template] rellena el formulario con un movimiento reciente (importe, categoría, cuenta y nota). */
    data class EditTransaction(
        val existing: FinanceTransaction? = null,
        val template: FinanceTransaction? = null,
    ) : FinanceEditor

    data class EditTransfer(
        val existing: FinanceTransfer? = null,
    ) : FinanceEditor

    data class EditBudget(
        val categoryId: String,
    ) : FinanceEditor

    data class EditAccount(
        val existing: Account? = null,
    ) : FinanceEditor

    data class EditCategory(
        val existing: FinanceCategory? = null,
    ) : FinanceEditor

    data class EditRule(
        val existing: RecurringRule? = null,
    ) : FinanceEditor
}

/** Un elemento borrable; sirve tanto para borrar como para restaurar con "Deshacer". */
internal sealed interface FinanceItem {
    data class Transaction(
        val value: FinanceTransaction,
    ) : FinanceItem

    data class Transfer(
        val value: FinanceTransfer,
    ) : FinanceItem

    data class Rule(
        val value: RecurringRule,
    ) : FinanceItem
}

/** Formulario enviado desde una hoja de edición. `existing` es lo que se está editando, si hay algo. */
internal sealed interface FinanceForm {
    data class Transaction(
        val draft: TransactionDraft,
        val existing: FinanceTransaction?,
    ) : FinanceForm

    data class Transfer(
        val draft: TransferDraft,
        val existing: FinanceTransfer?,
    ) : FinanceForm

    data class Account(
        val draft: AccountDraft,
        val existing: com.example.lifet.core.model.finance.Account?,
    ) : FinanceForm

    data class Category(
        val draft: CategoryDraft,
        val existing: FinanceCategory?,
    ) : FinanceForm

    data class Rule(
        val draft: RuleDraft,
        val existing: RecurringRule?,
    ) : FinanceForm

    /** Texto vacío elimina el presupuesto de la categoría. */
    data class Budget(
        val categoryId: String,
        val amountText: String,
    ) : FinanceForm
}

/** Eventos de un solo uso hacia la UI (snackbars, háptica). */
internal sealed interface FinanceEffect {
    data class Message(
        @param:StringRes val messageRes: Int,
    ) : FinanceEffect

    data class Deleted(
        @param:StringRes val messageRes: Int,
        val item: FinanceItem,
    ) : FinanceEffect

    data object Saved : FinanceEffect
}

internal sealed interface FinanceAction {
    data class SelectMonth(
        val month: YearMonth,
    ) : FinanceAction

    data class SetFilter(
        val filter: LedgerFilter,
    ) : FinanceAction

    /** Nombres localizados de las categorías de partida, para que la búsqueda de texto los encuentre. */
    data class SetCategoryNames(
        val names: Map<String, String>,
    ) : FinanceAction

    data object Retry : FinanceAction

    /** `null` cierra la hoja abierta. */
    data class Open(
        val editor: FinanceEditor?,
    ) : FinanceAction

    data class Save(
        val form: FinanceForm,
    ) : FinanceAction

    data class Delete(
        val item: FinanceItem,
    ) : FinanceAction

    data class Undo(
        val item: FinanceItem,
    ) : FinanceAction

    data class SetArchived(
        val category: FinanceCategory,
        val archived: Boolean,
    ) : FinanceAction
}

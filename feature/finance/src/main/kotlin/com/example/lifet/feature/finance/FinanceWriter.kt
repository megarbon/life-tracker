package com.example.lifet.feature.finance

import com.example.lifet.core.common.AppError
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.common.runCatchingApp
import com.example.lifet.core.data.finance.FinanceRepository
import com.example.lifet.core.model.finance.Account
import com.example.lifet.core.model.finance.FinanceCategory
import com.example.lifet.core.model.finance.FinanceDataSet
import com.example.lifet.core.model.finance.primaryCurrency
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Valida los formularios de la UI y los guarda. Separado del ViewModel para que éste sólo enrute
 * estado y eventos. Un formulario inválido devuelve [AppError.Validation] sin tocar la base de datos.
 */
internal class FinanceWriter
    @Inject
    constructor(
        private val repository: FinanceRepository,
        private val formSaver: FormSaver,
        private val clock: Clock,
    ) {
        suspend fun save(form: FinanceForm): AppResult<Unit> =
            when (val read = runCatchingApp(onError = { AppError.Storage }) { repository.observeData().first() }) {
                is AppResult.Failure -> read
                is AppResult.Success -> saveWith(form, read.value)
            }

        private suspend fun saveWith(
            form: FinanceForm,
            data: FinanceDataSet,
        ): AppResult<Unit> =
            when (form) {
                is FinanceForm.Transaction -> formSaver.saveTransaction(form, data)
                is FinanceForm.Transfer -> formSaver.saveTransfer(form, data)
                is FinanceForm.Account -> formSaver.saveAccount(form, data)
                is FinanceForm.Category -> formSaver.saveCategory(form, data)
                is FinanceForm.Rule -> formSaver.saveRule(form, data)
                is FinanceForm.Budget -> formSaver.saveBudget(form, data)
            }

        suspend fun delete(item: FinanceItem): AppResult<Unit> =
            when (item) {
                is FinanceItem.Transaction -> repository.deleteTransaction(item.value.id)
                is FinanceItem.Transfer -> repository.deleteTransfer(item.value.id)
                is FinanceItem.Rule -> repository.deleteRule(item.value.id, today())
            }

        suspend fun restore(item: FinanceItem): AppResult<Unit> =
            when (item) {
                is FinanceItem.Transaction -> repository.saveTransaction(item.value)
                is FinanceItem.Transfer -> repository.saveTransfer(item.value)
                is FinanceItem.Rule -> repository.saveRule(item.value, today())
            }

        suspend fun setArchived(
            category: FinanceCategory,
            archived: Boolean,
        ): AppResult<Unit> = repository.saveCategory(category.copy(archived = archived))

        private fun today(): LocalDate = LocalDate.now(clock)
    }

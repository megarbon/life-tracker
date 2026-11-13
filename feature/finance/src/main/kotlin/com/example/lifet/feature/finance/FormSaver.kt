package com.example.lifet.feature.finance

import com.example.lifet.core.common.AppError
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.data.finance.FinanceRepository
import com.example.lifet.core.model.finance.FinanceDataSet
import com.example.lifet.core.model.finance.primaryCurrency
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Valida cada formulario contra los datos actuales y lo guarda; un formulario inválido no toca la base de datos. */
internal class FormSaver
    @Inject
    constructor(
        private val repository: FinanceRepository,
        private val clock: Clock,
    ) {
        suspend fun saveTransaction(
            form: FinanceForm.Transaction,
            data: FinanceDataSet,
        ): AppResult<Unit> {
            val account = data.accounts.firstOrNull { it.id == form.draft.accountId }
            val created = form.existing?.createdAt ?: clock.instant()
            val zone = form.existing?.zone ?: clock.zone
            return when (val result = form.draft.validate(account, created, zone)) {
                is DraftResult.Invalid -> {
                    invalid(result)
                }

                is DraftResult.Valid -> {
                    repository.saveTransaction(
                        result.value.copy(recurringRuleId = form.existing?.recurringRuleId),
                    )
                }
            }
        }

        suspend fun saveTransfer(
            form: FinanceForm.Transfer,
            data: FinanceDataSet,
        ): AppResult<Unit> {
            val from = data.accounts.firstOrNull { it.id == form.draft.fromAccountId }
            val to = data.accounts.firstOrNull { it.id == form.draft.toAccountId }
            val created = form.existing?.createdAt ?: clock.instant()
            val zone = form.existing?.zone ?: clock.zone
            return when (val result = form.draft.validate(from, to, created, zone)) {
                is DraftResult.Invalid -> invalid(result)
                is DraftResult.Valid -> repository.saveTransfer(result.value)
            }
        }

        suspend fun saveAccount(
            form: FinanceForm.Account,
            data: FinanceDataSet,
        ): AppResult<Unit> =
            when (
                val result =
                    form.draft.validate(
                        form.existing,
                        (data.accounts.maxOfOrNull { it.sortOrder } ?: -1) + 1,
                    )
            ) {
                is DraftResult.Invalid -> invalid(result)
                is DraftResult.Valid -> repository.saveAccount(result.value)
            }

        suspend fun saveCategory(
            form: FinanceForm.Category,
            data: FinanceDataSet,
        ): AppResult<Unit> =
            when (
                val result =
                    form.draft.validate(form.existing, (data.categories.maxOfOrNull { it.sortOrder } ?: -1) + 1)
            ) {
                is DraftResult.Invalid -> invalid(result)
                is DraftResult.Valid -> repository.saveCategory(result.value)
            }

        suspend fun saveRule(
            form: FinanceForm.Rule,
            data: FinanceDataSet,
        ): AppResult<Unit> {
            val account = data.accounts.firstOrNull { it.id == form.draft.accountId }
            return when (val result = form.draft.validate(account, form.existing)) {
                is DraftResult.Invalid -> invalid(result)
                is DraftResult.Valid -> repository.saveRule(result.value, today())
            }
        }

        suspend fun saveBudget(
            form: FinanceForm.Budget,
            data: FinanceDataSet,
        ): AppResult<Unit> {
            if (form.amountText.isBlank()) return repository.setBudget(form.categoryId, null)
            val limit = parseFinanceAmount(form.amountText, data.primaryCurrency())
            return if (limit == null) {
                AppResult.Failure(AppError.Validation(DraftIssue.AMOUNT_INVALID.name))
            } else {
                repository.setBudget(form.categoryId, limit)
            }
        }

        private fun invalid(result: DraftResult.Invalid): AppResult<Unit> =
            AppResult.Failure(AppError.Validation(result.issues.joinToString { it.name }))

        private fun today(): LocalDate = LocalDate.now(clock)
    }

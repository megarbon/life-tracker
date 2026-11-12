package com.example.lifet.feature.finance

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifet.core.common.AppError
import com.example.lifet.core.common.AppResult
import com.example.lifet.core.data.finance.FinanceRepository
import com.example.lifet.core.domain.finance.ObserveFinanceOverview
import com.example.lifet.core.domain.finance.SyncRecurringTransactions
import com.example.lifet.core.model.finance.FinanceCategory
import com.example.lifet.core.model.finance.LedgerFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
internal class FinanceViewModel
    @Inject
    constructor(
        private val observeOverview: ObserveFinanceOverview,
        private val syncRecurring: SyncRecurringTransactions,
        private val repository: FinanceRepository,
        private val writer: FinanceWriter,
        private val clock: Clock,
        private val savedState: SavedStateHandle,
    ) : ViewModel() {
        private val month = MutableStateFlow(restoreMonth())
        private val filter = MutableStateFlow(LedgerFilter())
        private val categoryNames = MutableStateFlow<Map<String, String>>(emptyMap())
        private val retry = MutableStateFlow(0)
        private val _editor = MutableStateFlow<FinanceEditor?>(null)
        private val _effects = Channel<FinanceEffect>(Channel.BUFFERED)

        val editor: StateFlow<FinanceEditor?> = _editor
        val effects: Flow<FinanceEffect> = _effects.receiveAsFlow()

        @OptIn(ExperimentalCoroutinesApi::class)
        val uiState: StateFlow<FinanceUiState> =
            combine(month, filter, categoryNames, retry) { month, filter, names, _ -> Triple(month, filter, names) }
                .flatMapLatest { (month, filter, names) ->
                    observeOverview(month, filter) { names[it.id] ?: it.customName.orEmpty() }
                        .map<_, FinanceUiState> { overview ->
                            if (overview.hasAnyEntry) {
                                FinanceUiState.Success(overview, filter)
                            } else {
                                FinanceUiState.Empty(overview, filter)
                            }
                        }.catch { emit(FinanceUiState.Error(R.string.finance_error_load)) }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), FinanceUiState.Loading)

        init {
            viewModelScope.launch {
                // Categorías y cuenta de partida en el primer uso; después, las ocurrencias de los recurrentes.
                repository.ensureDefaults()
                syncRecurring()
            }
        }

        fun onAction(action: FinanceAction) {
            when (action) {
                is FinanceAction.SelectMonth -> {
                    selectMonth(action.month)
                }

                is FinanceAction.SetFilter -> {
                    filter.value = action.filter
                }

                is FinanceAction.SetCategoryNames -> {
                    categoryNames.value = action.names
                }

                FinanceAction.Retry -> {
                    retry.value += 1
                }

                is FinanceAction.Open -> {
                    _editor.value = action.editor
                }

                is FinanceAction.Save -> {
                    save(action.form)
                }

                is FinanceAction.Delete -> {
                    delete(action.item)
                }

                is FinanceAction.Undo -> {
                    undo(action.item)
                }

                is FinanceAction.SetArchived -> {
                    launchWrite(R.string.finance_error_save) {
                        writer.setArchived(action.category, action.archived)
                    }
                }
            }
        }

        private fun selectMonth(target: YearMonth) {
            // No hay saldo que mostrar en meses lejanos del futuro: se limita a un año vista.
            val limit = YearMonth.from(LocalDate.now(clock)).plusMonths(MAX_FUTURE_MONTHS)
            month.value = minOf(target, limit)
            savedState[KEY_MONTH] = month.value.toString()
        }

        private fun save(form: FinanceForm) {
            viewModelScope.launch {
                when (val result = writer.save(form)) {
                    is AppResult.Success -> {
                        _editor.value = null
                        _effects.send(FinanceEffect.Saved)
                    }

                    is AppResult.Failure -> {
                        _effects.send(FinanceEffect.Message(result.error.messageRes()))
                    }
                }
            }
        }

        private fun delete(item: FinanceItem) {
            viewModelScope.launch {
                when (val result = writer.delete(item)) {
                    is AppResult.Success -> {
                        _editor.value = null
                        _effects.send(FinanceEffect.Deleted(item.deletedMessageRes(), item))
                    }

                    is AppResult.Failure -> {
                        _effects.send(FinanceEffect.Message(result.error.messageRes()))
                    }
                }
            }
        }

        private fun undo(item: FinanceItem) = launchWrite(R.string.finance_error_save) { writer.restore(item) }

        private fun launchWrite(
            @StringRes errorRes: Int,
            block: suspend () -> AppResult<Unit>,
        ) {
            viewModelScope.launch {
                if (block() is AppResult.Failure) _effects.send(FinanceEffect.Message(errorRes))
            }
        }

        private fun restoreMonth(): YearMonth =
            savedState.get<String>(KEY_MONTH)?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
                ?: YearMonth.from(LocalDate.now(clock))

        private fun AppError.messageRes(): Int =
            when (this) {
                is AppError.Validation -> R.string.finance_error_invalid
                else -> R.string.finance_error_save
            }

        private fun FinanceItem.deletedMessageRes(): Int =
            when (this) {
                is FinanceItem.Transaction -> R.string.finance_deleted_transaction
                is FinanceItem.Transfer -> R.string.finance_deleted_transfer
                is FinanceItem.Rule -> R.string.finance_deleted_rule
            }

        private companion object {
            const val KEY_MONTH = "month"
            const val STOP_TIMEOUT_MS = 5_000L
            const val MAX_FUTURE_MONTHS = 12L
        }
    }

package com.example.lifet.feature.finance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifet.core.designsystem.component.LTEmptyState
import com.example.lifet.core.designsystem.component.LTErrorState
import com.example.lifet.core.designsystem.component.LTLoadingSkeleton
import com.example.lifet.core.designsystem.component.showUndo
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.theme.MinTouchTarget
import com.example.lifet.core.model.finance.DefaultCategory
import com.example.lifet.core.model.finance.FinanceOverview
import com.example.lifet.core.model.finance.LedgerFilter
import com.example.lifet.feature.finance.ui.AccountSheet
import com.example.lifet.feature.finance.ui.AccountsTab
import com.example.lifet.feature.finance.ui.BudgetSheet
import com.example.lifet.feature.finance.ui.BudgetsTab
import com.example.lifet.feature.finance.ui.CategorySheet
import com.example.lifet.feature.finance.ui.LedgerTab
import com.example.lifet.feature.finance.ui.RuleSheet
import com.example.lifet.feature.finance.ui.SummaryTab
import com.example.lifet.feature.finance.ui.TransactionSheet
import com.example.lifet.feature.finance.ui.TransferSheet
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth
import com.example.lifet.core.designsystem.R as DesignR

/** Punto de entrada de la ruta de Finanzas: conecta el ViewModel con la pantalla sin estado. */
@Composable
internal fun FinanceRoute(
    modifier: Modifier = Modifier,
    viewModel: FinanceViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val editor by viewModel.editor.collectAsStateWithLifecycle()
    FinanceScreen(
        state = state,
        editor = editor,
        effects = viewModel.effects,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FinanceScreen(
    state: FinanceUiState,
    editor: FinanceEditor?,
    effects: Flow<FinanceEffect>,
    onAction: (FinanceAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val tab = FinanceTab.entries[selected]
    val snackbar = remember { SnackbarHostState() }
    val categoryNames = defaultCategoryNames()

    LaunchedEffect(categoryNames) { onAction(FinanceAction.SetCategoryNames(categoryNames)) }
    EffectsHandler(effects, snackbar, onAction)

    val overview = state.overviewOrNull()
    Scaffold(
        modifier = modifier,
        topBar = {
            Column(Modifier.fillMaxWidth()) {
                TopAppBar(title = { Text(stringResource(R.string.finance_title)) })
                if (overview != null) {
                    MonthSwitcher(overview.month) { onAction(FinanceAction.SelectMonth(it)) }
                    PrimaryTabRow(selectedTabIndex = selected) {
                        FinanceTab.entries.forEachIndexed { index, item ->
                            Tab(
                                selected = index == selected,
                                onClick = { selected = index },
                                text = { Text(stringResource(item.labelRes)) },
                            )
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (overview != null) {
                FloatingActionButton(onClick = { onAction(FinanceAction.Open(FinanceEditor.EditTransaction())) }) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.finance_add))
                }
            }
        },
    ) { innerPadding ->
        val padding =
            PaddingValues(
                start = LifeTrackerTheme.spacing.md,
                end = LifeTrackerTheme.spacing.md,
                top = innerPadding.calculateTopPadding() + LifeTrackerTheme.spacing.sm,
                bottom = innerPadding.calculateBottomPadding() + FAB_CLEARANCE,
            )
        FinanceBody(state, tab, padding, innerPadding, onAction) { selected = it.ordinal }
    }

    if (overview != null) EditorHost(overview, editor, onAction)
}

@Composable
private fun FinanceBody(
    state: FinanceUiState,
    tab: FinanceTab,
    padding: PaddingValues,
    innerPadding: PaddingValues,
    onAction: (FinanceAction) -> Unit,
    onSelectTab: (FinanceTab) -> Unit,
) {
    val inner = Modifier.padding(top = innerPadding.calculateTopPadding())
    when (state) {
        FinanceUiState.Loading -> {
            LTLoadingSkeleton(Modifier.padding(innerPadding))
        }

        is FinanceUiState.Error -> {
            LTErrorState(
                message = stringResource(state.messageRes),
                retryLabel = stringResource(R.string.finance_retry),
                onRetry = { onAction(FinanceAction.Retry) },
                modifier = Modifier.padding(innerPadding),
            )
        }

        is FinanceUiState.Empty -> {
            if (tab == FinanceTab.SUMMARY || tab == FinanceTab.LEDGER) {
                LTEmptyState(
                    icon = Icons.Rounded.Add,
                    title = stringResource(R.string.finance_empty_title),
                    message = stringResource(R.string.finance_empty_message),
                    actionLabel = stringResource(R.string.finance_empty_cta),
                    onAction = { onAction(FinanceAction.Open(FinanceEditor.EditTransaction())) },
                    modifier = inner,
                )
            } else {
                TabContent(state.overview, state.filter, tab, padding, onAction, onSelectTab)
            }
        }

        is FinanceUiState.Success -> {
            TabContent(state.overview, state.filter, tab, padding, onAction, onSelectTab)
        }
    }
}

@Composable
private fun EffectsHandler(
    effects: Flow<FinanceEffect>,
    snackbar: SnackbarHostState,
    onAction: (FinanceAction) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val undoLabel = stringResource(DesignR.string.lt_undo)
    val resources = LocalResources.current
    LaunchedEffect(effects) {
        effects.collect { effect ->
            when (effect) {
                is FinanceEffect.Message -> {
                    snackbar.showSnackbar(resources.getString(effect.messageRes))
                }

                is FinanceEffect.Deleted -> {
                    snackbar.showUndo(resources.getString(effect.messageRes), undoLabel) {
                        onAction(FinanceAction.Undo(effect.item))
                    }
                }

                FinanceEffect.Saved -> {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                }
            }
        }
    }
}

@Composable
private fun TabContent(
    overview: FinanceOverview,
    filter: LedgerFilter,
    tab: FinanceTab,
    padding: PaddingValues,
    onAction: (FinanceAction) -> Unit,
    onSelectTab: (FinanceTab) -> Unit,
) {
    when (tab) {
        FinanceTab.SUMMARY -> {
            SummaryTab(
                overview = overview,
                contentPadding = padding,
                onOpenBudgets = { onSelectTab(FinanceTab.BUDGETS) },
                onEditTransaction = { onAction(FinanceAction.Open(FinanceEditor.EditTransaction(existing = it))) },
            )
        }

        FinanceTab.LEDGER -> {
            LedgerTab(
                overview = overview,
                filter = filter,
                contentPadding = padding,
                onFilterChange = { onAction(FinanceAction.SetFilter(it)) },
                onEditTransaction = { onAction(FinanceAction.Open(FinanceEditor.EditTransaction(existing = it))) },
                onEditTransfer = { onAction(FinanceAction.Open(FinanceEditor.EditTransfer(existing = it))) },
            )
        }

        FinanceTab.BUDGETS -> {
            BudgetsTab(
                overview = overview,
                contentPadding = padding,
                onEditBudget = { onAction(FinanceAction.Open(FinanceEditor.EditBudget(it))) },
            )
        }

        FinanceTab.ACCOUNTS -> {
            AccountsTab(
                overview = overview,
                contentPadding = padding,
                onEditAccount = { onAction(FinanceAction.Open(FinanceEditor.EditAccount(it))) },
                onEditCategory = { onAction(FinanceAction.Open(FinanceEditor.EditCategory(it))) },
                onEditRule = { onAction(FinanceAction.Open(FinanceEditor.EditRule(it))) },
                onAddTransfer = { onAction(FinanceAction.Open(FinanceEditor.EditTransfer())) },
            )
        }
    }
}

@Composable
private fun EditorHost(
    overview: FinanceOverview,
    editor: FinanceEditor?,
    onAction: (FinanceAction) -> Unit,
) {
    val dismiss = { onAction(FinanceAction.Open(null)) }
    when (editor) {
        null -> {
            Unit
        }

        is FinanceEditor.EditTransaction -> {
            TransactionSheet(
                overview = overview,
                existing = editor.existing,
                template = editor.template,
                onDismiss = dismiss,
                onSave = { onAction(FinanceAction.Save(FinanceForm.Transaction(it, editor.existing))) },
                onDelete =
                    editor.existing?.let { tx ->
                        { onAction(FinanceAction.Delete(FinanceItem.Transaction(tx))) }
                    },
            )
        }

        is FinanceEditor.EditTransfer -> {
            TransferSheet(
                overview = overview,
                existing = editor.existing,
                onDismiss = dismiss,
                onSave = { onAction(FinanceAction.Save(FinanceForm.Transfer(it, editor.existing))) },
                onDelete = editor.existing?.let { tr -> { onAction(FinanceAction.Delete(FinanceItem.Transfer(tr))) } },
            )
        }

        else -> {
            SetupEditorHost(overview, editor, onAction)
        }
    }
}

/** Hojas de configuración: presupuesto, cuenta, categoría y recurrente. */
@Composable
private fun SetupEditorHost(
    overview: FinanceOverview,
    editor: FinanceEditor,
    onAction: (FinanceAction) -> Unit,
) {
    val dismiss = { onAction(FinanceAction.Open(null)) }
    when (editor) {
        is FinanceEditor.EditBudget -> {
            overview.categories.firstOrNull { it.id == editor.categoryId }?.let { category ->
                BudgetSheet(
                    category = category,
                    overview = overview,
                    onDismiss = dismiss,
                    onSave = { onAction(FinanceAction.Save(FinanceForm.Budget(editor.categoryId, it))) },
                )
            }
        }

        is FinanceEditor.EditAccount -> {
            AccountSheet(
                existing = editor.existing,
                overview = overview,
                onDismiss = dismiss,
                onSave = { onAction(FinanceAction.Save(FinanceForm.Account(it, editor.existing))) },
            )
        }

        is FinanceEditor.EditCategory -> {
            CategorySheet(
                existing = editor.existing,
                onDismiss = dismiss,
                onSave = { onAction(FinanceAction.Save(FinanceForm.Category(it, editor.existing))) },
            )
        }

        is FinanceEditor.EditRule -> {
            RuleSheet(
                existing = editor.existing,
                overview = overview,
                onDismiss = dismiss,
                onSave = { onAction(FinanceAction.Save(FinanceForm.Rule(it, editor.existing))) },
                onDelete = editor.existing?.let { rule -> { onAction(FinanceAction.Delete(FinanceItem.Rule(rule))) } },
            )
        }

        is FinanceEditor.EditTransaction, is FinanceEditor.EditTransfer -> {
            Unit
        }
    }
}

@Composable
private fun MonthSwitcher(
    month: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = MinTouchTarget),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onMonthChange(month.minusMonths(1)) }) {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.finance_month_previous),
            )
        }
        Text(
            text = monthTitle(month),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
        )
        IconButton(onClick = { onMonthChange(month.plusMonths(1)) }) {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = stringResource(R.string.finance_month_next),
            )
        }
    }
}

@Composable
private fun defaultCategoryNames(): Map<String, String> =
    DefaultCategory.entries.associate { category -> category.id to stringResource(category.labelRes()) }

private fun FinanceUiState.overviewOrNull(): FinanceOverview? =
    when (this) {
        is FinanceUiState.Success -> overview
        is FinanceUiState.Empty -> overview
        else -> null
    }

private val FAB_CLEARANCE = 88.dp

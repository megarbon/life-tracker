package com.example.lifet.feature.nutrition.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifet.core.designsystem.component.LTChipGroup
import com.example.lifet.core.designsystem.component.LTQuickAddSheet
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.theme.MinTouchTarget
import com.example.lifet.core.model.nutrition.MealType
import com.example.lifet.feature.nutrition.R
import com.example.lifet.feature.nutrition.ui.labelRes
import java.time.LocalDate

/**
 * Hoja "Añadir al diario": recientes (un toque = una ración), selector de comida, buscar alimento, repetir ayer
 * y agua. Registrar un alimento habitual no debería llevar más de dos toques.
 */
@Composable
internal fun QuickAddSheetRoute(
    date: LocalDate,
    actions: QuickAddActions,
    modifier: Modifier = Modifier,
    viewModel: QuickAddViewModel = hiltViewModel(),
) {
    val recents by viewModel.recents.collectAsStateWithLifecycle()
    var mealName by rememberSaveable { mutableStateOf(viewModel.defaultMealType.name) }
    val mealType = MealType.fromName(mealName) ?: viewModel.defaultMealType
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is QuickAddEvent.Added -> {
                    actions.onEntriesAdded(listOf(event.id))
                    actions.onDismiss()
                }

                QuickAddEvent.Failed -> {
                    Unit
                }
            }
        }
    }
    LTQuickAddSheet(
        title = stringResource(R.string.nutrition_quick_add_title),
        onDismiss = actions.onDismiss,
        modifier = modifier,
        recents = recents.map { it.name },
        onRecentPick = { name -> viewModel.onRecentPicked(name, mealType, date) },
    ) {
        QuickAddContent(mealType, { mealName = it.name }, actions)
    }
}

@Composable
private fun QuickAddContent(
    mealType: MealType,
    onMealSelected: (MealType) -> Unit,
    actions: QuickAddActions,
) {
    val labels = MealType.entries.associateWith { stringResource(it.labelRes) }
    LTChipGroup(
        options = MealType.entries,
        selected = mealType,
        label = { labels.getValue(it) },
        onSelect = onMealSelected,
    )
    Button(
        onClick = {
            actions.onDismiss()
            actions.onSearch(mealType)
        },
        modifier = Modifier.fillMaxWidth().heightIn(min = MinTouchTarget),
    ) { Text(stringResource(R.string.nutrition_search_hint)) }
    Row(horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm)) {
        OutlinedButton(
            onClick = {
                actions.onDismiss()
                actions.onRepeatYesterday()
            },
            modifier = Modifier.weight(1f).heightIn(min = MinTouchTarget),
        ) { Text(stringResource(R.string.nutrition_repeat_yesterday)) }
        OutlinedButton(
            onClick = {
                actions.onDismiss()
                actions.onAddWater()
            },
            modifier = Modifier.weight(1f).heightIn(min = MinTouchTarget),
        ) { Text(stringResource(R.string.nutrition_add_water)) }
    }
}

package com.example.lifet.feature.nutrition

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.lifet.core.designsystem.preview.LTPreviews
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.model.nutrition.MealEntry
import com.example.lifet.core.model.nutrition.NutritionDay
import com.example.lifet.core.model.nutrition.NutritionDiary
import com.example.lifet.core.model.nutrition.WaterEntry
import com.example.lifet.feature.nutrition.diary.DiaryNavigation
import com.example.lifet.feature.nutrition.diary.DiaryScreen
import com.example.lifet.feature.nutrition.diary.DiaryUiState
import com.example.lifet.feature.nutrition.diary.toUiState
import com.example.lifet.feature.nutrition.ui.PreviewData

private val previewNavigation = DiaryNavigation(onSearch = { _, _ -> }, onTrends = {}, onGoals = {})

@Composable
private fun DiaryPreview(state: DiaryUiState) {
    LifeTrackerTheme {
        DiaryScreen(
            state = state,
            date = PreviewData.today,
            snackbarHostState = remember { SnackbarHostState() },
            navigation = previewNavigation,
            onAction = {},
        )
    }
}

private fun diaryState(
    meals: List<MealEntry>,
    water: List<WaterEntry>,
): DiaryUiState {
    val day = NutritionDay(PreviewData.today, meals, water, PreviewData.goals)
    return NutritionDiary(day, PreviewData.week).toUiState(PreviewData.today)
}

@LTPreviews
@Composable
private fun DiaryRegularPreview() = DiaryPreview(diaryState(PreviewData.meals, PreviewData.water))

@LTPreviews
@Composable
private fun DiaryEmptyPreview() =
    DiaryPreview(DiaryUiState.Empty(PreviewData.today, isToday = true, goals = PreviewData.goals))

@LTPreviews
@Composable
private fun DiaryEdgeCasesPreview() = DiaryPreview(diaryState(PreviewData.extremeMeals, emptyList()))

@LTPreviews
@Composable
private fun DiaryLoadingPreview() = DiaryPreview(DiaryUiState.Loading)

@LTPreviews
@Composable
private fun DiaryErrorPreview() = DiaryPreview(DiaryUiState.Error(R.string.nutrition_error_generic))

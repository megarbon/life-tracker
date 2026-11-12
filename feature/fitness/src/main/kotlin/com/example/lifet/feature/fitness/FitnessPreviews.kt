@file:Suppress("MagicNumber", "TooManyFunctions") // Datos de ejemplo literales y una función por cada @LTPreviews.

package com.example.lifet.feature.fitness

import androidx.compose.runtime.Composable
import com.example.lifet.core.designsystem.preview.LTPreviews
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.model.fitness.BodyWeight
import com.example.lifet.core.model.fitness.DailySteps
import com.example.lifet.core.model.fitness.ExerciseLog
import com.example.lifet.core.model.fitness.FitnessCalculator
import com.example.lifet.core.model.fitness.FitnessDataSet
import com.example.lifet.core.model.fitness.FitnessOverview
import com.example.lifet.core.model.fitness.StrengthSet
import com.example.lifet.core.model.fitness.TemplateExercise
import com.example.lifet.core.model.fitness.Workout
import com.example.lifet.core.model.fitness.WorkoutTemplate
import com.example.lifet.core.model.fitness.WorkoutType
import kotlinx.coroutines.flow.emptyFlow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

private val PREVIEW_TODAY: LocalDate = LocalDate.of(2026, 3, 11)
private val PREVIEW_ZONE: ZoneId = ZoneId.of("Europe/Madrid")
private const val PREVIEW_WEEKS = 14L
private const val LONG_TEXT_REPEATS = 14

private fun workout(
    id: String,
    type: WorkoutType,
    title: String,
    date: LocalDate,
    minutes: Long,
    distanceM: Long? = null,
    exercises: List<ExerciseLog> = emptyList(),
): Workout =
    Workout(
        id = id,
        type = type,
        title = title,
        start = date.atTime(LocalTime.of(18, 0)).atZone(PREVIEW_ZONE).toInstant(),
        zone = PREVIEW_ZONE,
        durationSec = minutes * 60,
        distanceM = distanceM,
        energyKcal = if (type.isCardio) (minutes * 9).toInt() else null,
        exercises = exercises,
    )

/** Dos sesiones de fuerza y un cardio por semana, con la carga del press subiendo poco a poco. */
private fun regularData(longTexts: Boolean): FitnessDataSet {
    val firstMonday = PREVIEW_TODAY.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(PREVIEW_WEEKS)
    val long =
        if (longTexts) {
            "Press de banca inclinado con agarre cerrado y pausa 🏋️ ".repeat(
                LONG_TEXT_REPEATS,
            )
        } else {
            ""
        }
    val workouts =
        (0..PREVIEW_WEEKS).flatMap { week ->
            val monday = firstMonday.plusWeeks(week)
            listOf(
                workout(
                    "s$week",
                    WorkoutType.STRENGTH,
                    "Empuje",
                    monday,
                    45,
                    exercises =
                        listOf(
                            ExerciseLog("Press banca$long", List(4) { StrengthSet(5, 60_000 + week * 1_250) }),
                            ExerciseLog("Dominadas", List(3) { StrengthSet(5 + (week / 4).toInt(), 0) }),
                        ),
                ),
                workout("p$week", WorkoutType.STRENGTH, "Pierna", monday.plusDays(2), 50),
                workout("r$week", WorkoutType.RUN, "Rodaje", monday.plusDays(5), 40, distanceM = 7_000),
            ).filter { it.startDate <= PREVIEW_TODAY }
        }
    val weights =
        (0L..90L).map { BodyWeight(PREVIEW_TODAY.minusDays(it), 82_400 - (90 - it) * 25 + (it % 3) * 100) }
    val steps = listOf(DailySteps(PREVIEW_TODAY, 6_420), DailySteps(PREVIEW_TODAY.minusDays(1), 9_100))
    val templates =
        listOf(
            WorkoutTemplate(
                "t",
                "Empuje",
                WorkoutType.STRENGTH,
                3_000,
                listOf(TemplateExercise("Press banca", 4, 5, 70_000)),
            ),
        )
    return FitnessDataSet(workouts, weights, steps, templates)
}

private fun overviewOf(data: FitnessDataSet): FitnessOverview = FitnessCalculator(data, PREVIEW_TODAY).overview()

@Composable
private fun PreviewScreen(
    state: FitnessUiState,
    editor: FitnessEditor? = null,
    tab: FitnessTab = FitnessTab.SUMMARY,
) {
    LifeTrackerTheme {
        FitnessScreen(state = state, editor = editor, effects = emptyFlow(), onAction = {}, initialTab = tab)
    }
}

@LTPreviews
@Composable
private fun FitnessSummaryPreview() {
    PreviewScreen(FitnessUiState.Success(overviewOf(regularData(longTexts = false))))
}

@LTPreviews
@Composable
private fun FitnessSummaryEdgeCasesPreview() {
    PreviewScreen(FitnessUiState.Success(overviewOf(regularData(longTexts = true))))
}

@LTPreviews
@Composable
private fun FitnessHistoryPreview() {
    PreviewScreen(FitnessUiState.Success(overviewOf(regularData(longTexts = false))), tab = FitnessTab.HISTORY)
}

@LTPreviews
@Composable
private fun FitnessExercisesPreview() {
    PreviewScreen(FitnessUiState.Success(overviewOf(regularData(longTexts = false))), tab = FitnessTab.EXERCISES)
}

@LTPreviews
@Composable
private fun FitnessBodyPreview() {
    PreviewScreen(FitnessUiState.Success(overviewOf(regularData(longTexts = false))), tab = FitnessTab.BODY)
}

@LTPreviews
@Composable
private fun FitnessEmptyPreview() {
    PreviewScreen(FitnessUiState.Empty(overviewOf(FitnessDataSet())))
}

@LTPreviews
@Composable
private fun FitnessLoadingPreview() {
    PreviewScreen(FitnessUiState.Loading)
}

@LTPreviews
@Composable
private fun FitnessErrorPreview() {
    PreviewScreen(FitnessUiState.Error(R.string.fitness_error_load))
}

@LTPreviews
@Composable
private fun FitnessWorkoutSheetPreview() {
    val overview = overviewOf(regularData(longTexts = false))
    val editor =
        FitnessEditor.EditWorkout(
            existing = null,
            seed = null,
            newId = "preview",
            today = PREVIEW_TODAY,
            zone = PREVIEW_ZONE,
            nowMinute = 19 * 60,
        )
    PreviewScreen(FitnessUiState.Success(overview), editor = editor)
}

@LTPreviews
@Composable
private fun FitnessExerciseDetailPreview() {
    val overview = overviewOf(regularData(longTexts = false))
    PreviewScreen(
        FitnessUiState.Success(overview),
        editor =
            FitnessEditor.ExerciseDetail(
                overview.history.exercises
                    .first()
                    .key,
            ),
        tab = FitnessTab.EXERCISES,
    )
}

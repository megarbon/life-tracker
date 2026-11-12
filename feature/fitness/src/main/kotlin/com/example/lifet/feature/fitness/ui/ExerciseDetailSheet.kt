package com.example.lifet.feature.fitness.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.theme.MinTouchTarget
import com.example.lifet.core.model.fitness.ExerciseProgress
import com.example.lifet.core.model.fitness.ExerciseSession
import com.example.lifet.core.model.fitness.PersonalRecord
import com.example.lifet.core.model.fitness.RecordKind
import com.example.lifet.feature.fitness.FitnessFormatters
import com.example.lifet.feature.fitness.R
import com.example.lifet.feature.fitness.quantity
import com.example.lifet.feature.fitness.rememberFitnessFormatters

/** Últimas sesiones que lista el detalle; el resto se ve en la gráfica. */
private const val SESSIONS_SHOWN = 6

/** Detalle de un ejercicio: gráfica de progresión, mejor marca, últimas sesiones y récords batidos. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExerciseDetailSheet(
    exercise: ExerciseProgress?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = LifeTrackerTheme.spacing.md)
                    .padding(bottom = LifeTrackerTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.md),
        ) {
            if (exercise == null) {
                Text(stringResource(R.string.fitness_exercise_missing), style = MaterialTheme.typography.bodyLarge)
            } else {
                ExerciseDetailContent(exercise)
            }
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                Text(stringResource(R.string.fitness_close))
            }
        }
    }
}

@Composable
private fun ExerciseDetailContent(exercise: ExerciseProgress) {
    val formatters = rememberFitnessFormatters()
    Text(
        text = exercise.name,
        style = MaterialTheme.typography.titleLarge,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.semantics { heading() },
    )
    Text(
        text = bestMark(exercise, formatters),
        style = MaterialTheme.typography.titleMedium,
    )
    ExerciseProgressChart(exercise, formatters)
    Text(
        text = stringResource(R.string.fitness_exercise_sessions),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() },
    )
    exercise.sessions.takeLast(SESSIONS_SHOWN).asReversed().forEach { session ->
        Text(
            text =
                stringResource(
                    R.string.fitness_exercise_session_row,
                    formatters.date(session.date),
                    sessionSets(session, formatters),
                ),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    if (exercise.records.isNotEmpty()) {
        Text(
            text = stringResource(R.string.fitness_exercise_records),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        exercise.records.asReversed().take(SESSIONS_SHOWN).forEach { record ->
            Text(
                text =
                    stringResource(
                        R.string.fitness_exercise_session_row,
                        formatters.date(record.date),
                        recordValue(record, formatters),
                    ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun bestMark(
    exercise: ExerciseProgress,
    formatters: FitnessFormatters,
): String =
    if (exercise.isBodyweightOnly) {
        quantity(R.plurals.fitness_exercise_best_reps, exercise.bestReps ?: 0)
    } else {
        stringResource(R.string.fitness_exercise_best_load, formatters.kilos(exercise.bestOneRepMaxGrams ?: 0L))
    }

/** Series de una sesión en una línea ("5 × 62,5, 5 × 62,5, 3 × 65"); las de peso corporal dicen "peso corporal". */
@Composable
internal fun sessionSets(
    session: ExerciseSession,
    formatters: FitnessFormatters,
): String {
    val withLoad = stringResource(R.string.fitness_exercise_set_load)
    val bodyweight = stringResource(R.string.fitness_exercise_set_bodyweight)
    return session.sets.joinToString(", ") { set ->
        if (set.isBodyweight) {
            bodyweight.format(set.reps)
        } else {
            withLoad.format(set.reps, formatters.load(set.weightGrams))
        }
    }
}

/** Valor legible de un récord: 1RM en kg o repeticiones. */
@Composable
internal fun recordValue(
    record: PersonalRecord,
    formatters: FitnessFormatters,
): String =
    when (record.kind) {
        RecordKind.ONE_REP_MAX -> stringResource(R.string.fitness_record_one_rep_max, formatters.kilos(record.value))
        RecordKind.BODYWEIGHT_REPS -> quantity(R.plurals.fitness_record_reps, record.value.toInt())
    }

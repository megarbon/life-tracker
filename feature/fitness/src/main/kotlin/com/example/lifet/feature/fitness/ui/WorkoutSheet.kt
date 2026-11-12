package com.example.lifet.feature.fitness.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import com.example.lifet.core.designsystem.component.LTChipGroup
import com.example.lifet.core.designsystem.component.LTQuickAddSheet
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.theme.MinTouchTarget
import com.example.lifet.core.model.fitness.FitnessOverview
import com.example.lifet.core.model.fitness.FitnessRules
import com.example.lifet.core.model.fitness.Workout
import com.example.lifet.core.model.fitness.WorkoutTemplate
import com.example.lifet.core.model.fitness.WorkoutType
import com.example.lifet.feature.fitness.DraftIssue
import com.example.lifet.feature.fitness.ExerciseDraft
import com.example.lifet.feature.fitness.FitnessEditor
import com.example.lifet.feature.fitness.R
import com.example.lifet.feature.fitness.SetDraft
import com.example.lifet.feature.fitness.WorkoutDraft
import com.example.lifet.feature.fitness.WorkoutSeed
import com.example.lifet.feature.fitness.blankWorkoutDraft
import com.example.lifet.feature.fitness.issues
import com.example.lifet.feature.fitness.labelRes
import com.example.lifet.feature.fitness.rememberFitnessFormatters
import com.example.lifet.feature.fitness.toDraft
import com.example.lifet.feature.fitness.toRepeatDraft
import com.example.lifet.feature.fitness.validate

private const val NOTE_MAX_LINES = 3

/**
 * Hoja de alta y edición de un entreno. Arriba, los entrenos recientes como chips: un toque rellena tipo, título,
 * duración, distancia y ejercicios para repetirlo en menos de cinco segundos. Las plantillas se eligen aparte.
 */
@Composable
internal fun WorkoutSheet(
    overview: FitnessOverview,
    editor: FitnessEditor.EditWorkout,
    onDismiss: () -> Unit,
    onSave: (WorkoutDraft) -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val zone = editor.existing?.zone ?: editor.zone
    var draft by rememberSaveable(editor.existing?.id, editor.newId) { mutableStateOf(initialDraft(editor)) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val issues = if (submitted) draft.validate(zone).issues() else emptySet()
    val recents = if (editor.existing == null) overview.recentWorkouts else emptyList()
    val recentLabels = recentLabels(recents)
    val knownExercises = remember(overview.history) { overview.history.exercises.map { it.name } }

    LTQuickAddSheet(
        title =
            stringResource(
                if (editor.existing ==
                    null
                ) {
                    R.string.fitness_sheet_add
                } else {
                    R.string.fitness_sheet_edit
                },
            ),
        onDismiss = onDismiss,
        recents = recentLabels.map { it.second },
        onRecentPick = { label ->
            recentLabels.firstOrNull { it.second == label }?.first?.let {
                draft = it.toRepeatDraft(editor.newId, editor.today, editor.nowMinute)
                submitted = false
            }
        },
        modifier = modifier,
    ) {
        SheetColumn {
            if (editor.existing == null && overview.templates.isNotEmpty()) {
                TemplatePicker(overview) { template ->
                    draft = template.toDraft(editor.newId, editor.today, editor.nowMinute)
                    submitted = false
                }
            }
            WorkoutFields(draft, issues, knownExercises) { draft = it }
            SheetActions(
                onSave = {
                    submitted = true
                    if (draft.validate(zone).issues().isEmpty()) onSave(draft)
                },
                onDelete = onDelete,
            )
        }
    }
}

private fun initialDraft(editor: FitnessEditor.EditWorkout): WorkoutDraft =
    when {
        editor.existing != null -> {
            editor.existing.toDraft()
        }

        editor.seed is WorkoutSeed.FromWorkout -> {
            editor.seed.workout.toRepeatDraft(
                editor.newId,
                editor.today,
                editor.nowMinute,
            )
        }

        editor.seed is WorkoutSeed.FromTemplate -> {
            editor.seed.template.toDraft(
                editor.newId,
                editor.today,
                editor.nowMinute,
            )
        }

        else -> {
            blankWorkoutDraft(editor.newId, WorkoutType.STRENGTH, editor.today, editor.nowMinute)
        }
    }

/** Etiqueta de cada entreno reciente: su título o, si no tiene, el tipo. Las repetidas se descartan. */
@Composable
private fun recentLabels(recents: List<Workout>): List<Pair<Workout, String>> {
    val fallback = stringResource(R.string.fitness_workout_default_title)
    val typeNames = WorkoutType.entries.associateWith { stringResource(it.labelRes) }
    return remember(recents, fallback, typeNames) {
        recents
            .map { it to it.title.ifBlank { typeNames[it.type] ?: fallback } }
            .distinctBy { it.second }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TemplatePicker(
    overview: FitnessOverview,
    onPick: (WorkoutTemplate) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.xs)) {
        Text(
            text = stringResource(R.string.fitness_templates),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm)) {
            overview.templates.forEach { template ->
                val name = template.name.ifBlank { stringResource(template.type.labelRes) }
                SuggestionChip(
                    onClick = { onPick(template) },
                    label = { Text(name) },
                    modifier = Modifier.heightIn(min = MinTouchTarget),
                )
            }
        }
    }
}

@Composable
private fun WorkoutFields(
    draft: WorkoutDraft,
    issues: Set<DraftIssue>,
    knownExercises: List<String>,
    onChange: (WorkoutDraft) -> Unit,
) {
    val labels = WorkoutType.entries.associateWith { stringResource(it.labelRes) }
    LTChipGroup(
        options = WorkoutType.entries,
        selected = draft.type,
        label = { labels[it].orEmpty() },
        onSelect = { onChange(draft.withType(it)) },
    )
    TextInput(
        value = draft.title,
        onValueChange = { onChange(draft.copy(title = it)) },
        label = stringResource(R.string.fitness_field_title),
        maxLength = FitnessRules.TITLE_MAX_LENGTH,
        modifier = Modifier.fillMaxWidth(),
    )
    NumberField(
        value = draft.durationText,
        onValueChange = { onChange(draft.withDuration(it)) },
        label = stringResource(R.string.fitness_field_duration),
        modifier = Modifier.fillMaxWidth(),
        error = issueText(issues, DraftIssue.DURATION_MISSING, DraftIssue.DURATION_INVALID),
    )
    if (draft.type.isCardio) {
        NumberField(
            value = draft.distanceText,
            onValueChange = { onChange(draft.copy(distanceText = it)) },
            label = stringResource(R.string.fitness_field_distance),
            modifier = Modifier.fillMaxWidth(),
            maxDecimals = 3,
            error = issueText(issues, DraftIssue.DISTANCE_INVALID),
        )
    }
    NumberField(
        value = draft.energyText,
        onValueChange = { onChange(draft.copy(energyText = it)) },
        label = stringResource(R.string.fitness_field_energy),
        modifier = Modifier.fillMaxWidth(),
        error = issueText(issues, DraftIssue.ENERGY_INVALID),
    )
    if (draft.type.isStrength) ExercisesSection(draft, issues, knownExercises, onChange)
    Row(horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm)) {
        DateField(date = draft.date, onDateChange = { onChange(draft.copy(date = it)) }, modifier = Modifier.weight(1f))
        TimeField(
            minuteOfDay = draft.startMinute,
            onChange = { onChange(draft.withStart(it)) },
            modifier = Modifier.weight(1f),
        )
    }
    TextInput(
        value = draft.note,
        onValueChange = { onChange(draft.copy(note = it)) },
        label = stringResource(R.string.fitness_field_note),
        maxLength = FitnessRules.NOTE_MAX_LENGTH,
        modifier = Modifier.fillMaxWidth(),
        maxLines = NOTE_MAX_LINES,
    )
    SaveAsTemplate(draft, issues, onChange)
}

@Composable
private fun SaveAsTemplate(
    draft: WorkoutDraft,
    issues: Set<DraftIssue>,
    onChange: (WorkoutDraft) -> Unit,
) {
    Column {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = MinTouchTarget)
                    .toggleable(
                        value = draft.saveAsTemplate,
                        role = Role.Checkbox,
                        onValueChange = { onChange(draft.copy(saveAsTemplate = it)) },
                    ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm),
        ) {
            Checkbox(checked = draft.saveAsTemplate, onCheckedChange = null)
            Text(stringResource(R.string.fitness_save_as_template), style = MaterialTheme.typography.bodyLarge)
        }
        issueText(issues, DraftIssue.TEMPLATE_NEEDS_TITLE)?.let { ErrorText(it) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExercisesSection(
    draft: WorkoutDraft,
    issues: Set<DraftIssue>,
    knownExercises: List<String>,
    onChange: (WorkoutDraft) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.md)) {
        draft.exercises.forEachIndexed { index, exercise ->
            ExerciseEditor(index, exercise, issues, draft, onChange)
        }
        val suggestions =
            knownExercises.filter { name ->
                draft.exercises.none { it.name.equals(name, ignoreCase = true) }
            }
        if (draft.canAddExercise && suggestions.isNotEmpty()) {
            Text(
                text = stringResource(R.string.fitness_exercise_suggestions),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm)) {
                suggestions.take(MAX_SUGGESTIONS).forEach { name ->
                    SuggestionChip(
                        onClick = { onChange(draft.addExerciseNamed(name)) },
                        label = { Text(name, maxLines = 1) },
                        modifier = Modifier.heightIn(min = MinTouchTarget),
                    )
                }
            }
        }
        if (draft.canAddExercise) {
            OutlinedButton(
                onClick = { onChange(draft.addExercise()) },
                modifier = Modifier.heightIn(min = MinTouchTarget),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Text(stringResource(R.string.fitness_exercise_add))
            }
        }
    }
}

@Composable
private fun ExerciseEditor(
    index: Int,
    exercise: ExerciseDraft,
    issues: Set<DraftIssue>,
    draft: WorkoutDraft,
    onChange: (WorkoutDraft) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm)) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.xs),
        ) {
            TextInput(
                value = exercise.name,
                onValueChange = { name -> onChange(draft.updateExercise(index) { it.copy(name = name) }) },
                label = stringResource(R.string.fitness_exercise_name, index + 1),
                maxLength = FitnessRules.EXERCISE_NAME_MAX_LENGTH,
                modifier = Modifier.weight(1f),
                error = issueText(issues, DraftIssue.EXERCISE_NAME_MISSING).takeIf { exercise.name.isBlank() },
            )
            IconButton(
                onClick = { onChange(draft.removeExercise(index)) },
                modifier = Modifier.heightIn(min = MinTouchTarget),
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription =
                        stringResource(
                            R.string.fitness_exercise_remove,
                            index + 1,
                        ),
                )
            }
        }
        exercise.sets.forEachIndexed { setIndex, set ->
            SetRow(index, setIndex, set, draft, onChange)
        }
        issueText(issues, DraftIssue.SET_INVALID, DraftIssue.EXERCISE_WITHOUT_SETS)?.let { ErrorText(it) }
        OutlinedButton(
            onClick = { onChange(draft.addSet(index)) },
            enabled = exercise.sets.size < FitnessRules.MAX_SETS_PER_EXERCISE,
            modifier = Modifier.heightIn(min = MinTouchTarget),
        ) { Text(stringResource(R.string.fitness_set_add)) }
    }
}

@Composable
private fun SetRow(
    exerciseIndex: Int,
    setIndex: Int,
    set: SetDraft,
    draft: WorkoutDraft,
    onChange: (WorkoutDraft) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm),
        modifier = Modifier.semantics(mergeDescendants = false) {},
    ) {
        Text(
            text = stringResource(R.string.fitness_set_label, setIndex + 1),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(SET_LABEL_WEIGHT),
        )
        NumberField(
            value = set.repsText,
            onValueChange = { text -> onChange(draft.updateSet(exerciseIndex, setIndex) { it.copy(repsText = text) }) },
            label = stringResource(R.string.fitness_set_reps),
            modifier = Modifier.weight(SET_FIELD_WEIGHT),
        )
        NumberField(
            value = set.weightText,
            onValueChange = { text ->
                onChange(draft.updateSet(exerciseIndex, setIndex) { it.copy(weightText = text) })
            },
            label = stringResource(R.string.fitness_set_weight),
            modifier = Modifier.weight(SET_FIELD_WEIGHT),
            maxDecimals = 3,
            supporting = if (setIndex == 0) stringResource(R.string.fitness_set_weight_hint) else null,
        )
        IconButton(onClick = {
            onChange(draft.removeSet(exerciseIndex, setIndex))
        }, modifier = Modifier.heightIn(min = MinTouchTarget)) {
            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.fitness_set_remove, setIndex + 1))
        }
    }
}

@Composable
internal fun ErrorText(text: String) {
    Text(text, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
}

private const val MAX_SUGGESTIONS = 8
private const val SET_LABEL_WEIGHT = 0.8f
private const val SET_FIELD_WEIGHT = 1f

package com.example.lifet.feature.fitness.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.example.lifet.core.designsystem.component.LTQuickAddSheet
import com.example.lifet.core.model.fitness.BodyWeight
import com.example.lifet.core.model.fitness.DailySteps
import com.example.lifet.core.model.fitness.FitnessRules
import com.example.lifet.feature.fitness.DraftIssue
import com.example.lifet.feature.fitness.R
import com.example.lifet.feature.fitness.StepsDraft
import com.example.lifet.feature.fitness.WeightDraft
import com.example.lifet.feature.fitness.issues
import com.example.lifet.feature.fitness.toDraft
import com.example.lifet.feature.fitness.validate
import java.time.LocalDate

private const val KG_DECIMALS = 3

/**
 * Hoja de peso corporal. Un peso por día: guardar otro el mismo día lo sustituye, así que el aviso de "ya hay un
 * registro" no hace falta; editar uno existente permite cambiar su fecha.
 */
@Composable
internal fun WeightSheet(
    existing: BodyWeight?,
    today: LocalDate,
    suggestedKilos: String,
    onDismiss: () -> Unit,
    onSave: (WeightDraft) -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var draft by rememberSaveable(existing?.date) {
        mutableStateOf(existing?.toDraft() ?: WeightDraft(date = today, weightText = suggestedKilos))
    }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val issues = if (submitted) draft.validate().issues() else emptySet()
    LTQuickAddSheet(
        title =
            stringResource(
                if (existing ==
                    null
                ) {
                    R.string.fitness_weight_sheet_add
                } else {
                    R.string.fitness_weight_sheet_edit
                },
            ),
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        SheetColumn {
            NumberField(
                value = draft.weightText,
                onValueChange = { draft = draft.copy(weightText = it) },
                label = stringResource(R.string.fitness_weight_field),
                modifier = Modifier.fillMaxWidth(),
                maxDecimals = KG_DECIMALS,
                error = issueText(issues, DraftIssue.WEIGHT_MISSING, DraftIssue.WEIGHT_INVALID),
            )
            DateField(
                date = draft.date,
                onDateChange = { draft = draft.copy(date = it) },
                modifier = Modifier.fillMaxWidth(),
            )
            TextInput(
                value = draft.note,
                onValueChange = { draft = draft.copy(note = it) },
                label = stringResource(R.string.fitness_weight_note),
                maxLength = FitnessRules.NOTE_MAX_LENGTH,
                modifier = Modifier.fillMaxWidth(),
            )
            SheetActions(
                onSave = {
                    submitted = true
                    if (draft.validate().issues().isEmpty()) onSave(draft)
                },
                onDelete = onDelete,
            )
        }
    }
}

/** Hoja de pasos manuales de un día. Un registro por día: guardar otro lo sustituye. */
@Composable
internal fun StepsSheet(
    existing: DailySteps?,
    today: LocalDate,
    onDismiss: () -> Unit,
    onSave: (StepsDraft) -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var draft by rememberSaveable(existing?.date) { mutableStateOf(existing?.toDraft() ?: StepsDraft(date = today)) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val issues = if (submitted) draft.validate().issues() else emptySet()
    LTQuickAddSheet(
        title =
            stringResource(
                if (existing ==
                    null
                ) {
                    R.string.fitness_steps_sheet_add
                } else {
                    R.string.fitness_steps_sheet_edit
                },
            ),
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        SheetColumn {
            NumberField(
                value = draft.stepsText,
                onValueChange = { draft = draft.copy(stepsText = it) },
                label = stringResource(R.string.fitness_steps_field),
                modifier = Modifier.fillMaxWidth(),
                error = issueText(issues, DraftIssue.STEPS_MISSING, DraftIssue.STEPS_INVALID),
                imeAction = ImeAction.Done,
            )
            DateField(
                date = draft.date,
                onDateChange = { draft = draft.copy(date = it) },
                modifier = Modifier.fillMaxWidth(),
            )
            SheetActions(
                onSave = {
                    submitted = true
                    if (draft.validate().issues().isEmpty()) onSave(draft)
                },
                onDelete = onDelete,
            )
        }
    }
}

package com.example.lifet.feature.fitness.ui

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.theme.MinTouchTarget
import com.example.lifet.feature.fitness.DraftIssue
import com.example.lifet.feature.fitness.R
import com.example.lifet.feature.fitness.messageRes
import com.example.lifet.feature.fitness.rememberFitnessFormatters
import com.example.lifet.feature.fitness.sanitizeDecimalInput
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import com.example.lifet.core.designsystem.R as DesignR

private const val MINUTES_IN_HOUR = 60

/** Texto de error de un problema de formulario, o `null` si no aplica a este campo. */
@Composable
internal fun issueText(
    issues: Set<DraftIssue>,
    vararg relevant: DraftIssue,
): String? = relevant.firstOrNull { it in issues }?.let { stringResource(it.messageRes) }

/**
 * Campo numérico que sólo deja teclear dígitos y, si [maxDecimals] > 0, un separador decimal. El texto es de la
 * UI (no `Double`) para no perder precisión; se convierte al validar el formulario.
 */
@Composable
internal fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    maxDecimals: Int = 0,
    error: String? = null,
    supporting: String? = null,
    imeAction: ImeAction = ImeAction.Next,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(sanitizeDecimalInput(it, maxDecimals)) },
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
        isError = error != null,
        supportingText = (error ?: supporting)?.let { { Text(it) } },
        keyboardOptions =
            KeyboardOptions(
                keyboardType = if (maxDecimals > 0) KeyboardType.Decimal else KeyboardType.Number,
                imeAction = imeAction,
            ),
    )
}

/** Texto libre de una o varias líneas, con el límite de longitud que acepta el modelo. */
@Composable
internal fun TextInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    maxLength: Int,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    error: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.take(maxLength)) },
        modifier = modifier,
        label = { Text(label) },
        minLines = 1,
        maxLines = maxLines,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(imeAction = if (maxLines > 1) ImeAction.Default else ImeAction.Next),
    )
}

/** Botón que muestra la fecha y abre el selector de fecha de Material. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateField(
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val formatters = rememberFitnessFormatters()
    OutlinedButton(
        onClick = { open = true },
        modifier = modifier.heightIn(min = MinTouchTarget),
    ) {
        Text(stringResource(R.string.fitness_field_date, formatters.date(date)), maxLines = 2)
    }
    if (open) {
        // El selector trabaja en milisegundos UTC a medianoche: se evita la zona local para no correr el día.
        val state =
            rememberDatePickerState(
                initialSelectedDateMillis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis ->
                            onDateChange(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                        }
                        open = false
                    },
                ) { Text(stringResource(R.string.fitness_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) { Text(stringResource(DesignR.string.lt_cancel)) }
            },
        ) { DatePicker(state = state) }
    }
}

/** Botón con la hora de inicio que abre el selector de hora (12 o 24 h según el dispositivo). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimeField(
    minuteOfDay: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val formatters = rememberFitnessFormatters()
    OutlinedButton(onClick = { open = true }, modifier = modifier.heightIn(min = MinTouchTarget)) {
        Text(stringResource(R.string.fitness_field_start, formatters.clock(minuteOfDay)), maxLines = 2)
    }
    if (open) {
        val state =
            rememberTimePickerState(
                initialHour = minuteOfDay / MINUTES_IN_HOUR,
                initialMinute = minuteOfDay % MINUTES_IN_HOUR,
                is24Hour = DateFormat.is24HourFormat(LocalContext.current),
            )
        AlertDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onChange(state.hour * MINUTES_IN_HOUR + state.minute)
                        open = false
                    },
                ) { Text(stringResource(R.string.fitness_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) { Text(stringResource(DesignR.string.lt_cancel)) }
            },
            text = { TimePicker(state = state) },
        )
    }
}

/** Fila de acciones de una hoja: borrar (si se edita) y guardar. */
@Composable
internal fun SheetActions(
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm),
    ) {
        if (onDelete != null) {
            OutlinedButton(onClick = onDelete, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                Text(stringResource(R.string.fitness_delete), color = MaterialTheme.colorScheme.error)
            }
        }
        Button(onClick = onSave, modifier = Modifier.weight(1f).heightIn(min = MinTouchTarget)) {
            Text(stringResource(R.string.fitness_save))
        }
    }
}

@Composable
internal fun SheetColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.md),
    ) { content() }
}

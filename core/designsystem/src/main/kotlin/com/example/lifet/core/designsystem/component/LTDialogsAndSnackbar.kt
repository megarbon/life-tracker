package com.example.lifet.core.designsystem.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.lifet.core.designsystem.R
import com.example.lifet.core.designsystem.preview.LTPreviews
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme

/** Diálogo de confirmación para acciones destructivas o difíciles de revertir. */
@Composable
public fun LTConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String = stringResource(R.string.lt_confirm),
    dismissLabel: String = stringResource(R.string.lt_cancel),
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissLabel) } },
    )
}

/**
 * Muestra un snackbar con "Deshacer" tras borrar. Ejecuta [onUndo] sólo si el usuario pulsa la acción.
 * Es `suspend`: lánzalo desde un `LaunchedEffect` o `scope.launch`; la pila se descarta sola.
 */
public suspend fun SnackbarHostState.showUndo(
    message: String,
    undoLabel: String,
    onUndo: () -> Unit,
) {
    currentSnackbarData?.dismiss()
    val result =
        showSnackbar(
            message = message,
            actionLabel = undoLabel,
            withDismissAction = true,
            duration = SnackbarDuration.Long,
        )
    if (result == SnackbarResult.ActionPerformed) onUndo()
}

@LTPreviews
@Composable
private fun LTConfirmDialogPreview() {
    LifeTrackerTheme {
        LTConfirmDialog(
            title = "¿Borrar movimiento?",
            message = "No se puede deshacer.",
            onConfirm = {},
            onDismiss = {},
        )
    }
}

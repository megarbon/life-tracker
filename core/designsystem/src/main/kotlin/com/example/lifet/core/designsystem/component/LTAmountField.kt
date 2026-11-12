package com.example.lifet.core.designsystem.component

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.example.lifet.core.designsystem.preview.LTPreviews
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.util.parseAmount
import com.example.lifet.core.designsystem.util.sanitizeAmountInput
import com.example.lifet.core.model.CurrencyCode
import com.example.lifet.core.model.Money

/**
 * Campo de importe con teclado numérico. Guarda texto (no `Double`) para no perder precisión;
 * usa [parseAmount] para obtener el [Money]. [text] y [onTextChange] llevan el estado fuera
 * para que el ViewModel o el formulario lo controlen. Los decimales se limitan a los de [currency].
 */
@Composable
public fun LTAmountField(
    text: String,
    onTextChange: (String) -> Unit,
    currency: CurrencyCode,
    label: String,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
) {
    OutlinedTextField(
        value = text,
        onValueChange = { onTextChange(sanitizeAmountInput(it, currency)) },
        modifier = modifier,
        label = { Text(label) },
        suffix = { Text(currency.value) },
        singleLine = true,
        isError = errorMessage != null,
        supportingText = errorMessage?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}

/** Atajo para formularios: el importe tecleado, o `null` si está vacío o es inválido. */
public fun amountOrNull(
    text: String,
    currency: CurrencyCode,
): Money? = parseAmount(text, currency)

@LTPreviews
@Composable
private fun LTAmountFieldPreview() {
    LifeTrackerTheme {
        LTAmountField(text = "12,50", onTextChange = {}, currency = CurrencyCode.EUR, label = "Importe")
    }
}

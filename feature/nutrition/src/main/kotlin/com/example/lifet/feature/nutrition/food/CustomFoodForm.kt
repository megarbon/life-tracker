package com.example.lifet.feature.nutrition.food

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.example.lifet.core.model.nutrition.FoodCategory
import com.example.lifet.core.model.nutrition.FoodItem
import com.example.lifet.core.model.nutrition.Nutrients
import com.example.lifet.feature.nutrition.R
import com.example.lifet.feature.nutrition.ui.parseDecimal

/** Campos del formulario de alimento propio como texto, tal y como los teclea el usuario. */
@Immutable
internal data class CustomFoodForm(
    val name: String = "",
    val kcal: String = "",
    val protein: String = "",
    val carbs: String = "",
    val fat: String = "",
    val fiber: String = "",
    val sugar: String = "",
    val serving: String = DEFAULT_SERVING,
) {
    companion object {
        const val DEFAULT_SERVING = "100"
    }
}

/** Errores de cada campo; `null` significa válido. */
@Immutable
internal data class FieldErrors(
    @param:StringRes val name: Int? = null,
    @param:StringRes val kcal: Int? = null,
    @param:StringRes val protein: Int? = null,
    @param:StringRes val carbs: Int? = null,
    @param:StringRes val fat: Int? = null,
    @param:StringRes val fiber: Int? = null,
    @param:StringRes val sugar: Int? = null,
    @param:StringRes val serving: Int? = null,
) {
    val hasErrors: Boolean
        get() = listOf(name, kcal, protein, carbs, fat, fiber, sugar, serving).any { it != null }
}

/** Resultado de validar un campo numérico: el valor, o el texto de error. */
private data class Parsed(
    val value: Double?,
    @param:StringRes val error: Int?,
)

private val INVALID_NUMBER = Parsed(null, R.string.nutrition_field_invalid)

private fun required(text: String): Parsed {
    val number = parseDecimal(text)?.takeIf { it >= 0.0 }
    return when {
        text.isBlank() -> Parsed(null, R.string.nutrition_field_required)
        number == null -> Parsed(null, R.string.nutrition_field_invalid)
        else -> Parsed(number, null)
    }
}

private fun optional(text: String): Parsed = if (text.isBlank()) Parsed(0.0, null) else required(text)

/** Valida el formulario. Los números aceptan coma o punto; fibra y azúcares pueden quedar vacíos. */
internal fun CustomFoodForm.validate(): FormResult {
    val kcalP = required(kcal)
    val proteinP = required(protein)
    val carbsP = required(carbs)
    val fatP = required(fat)
    val fiberP = optional(fiber)
    val sugarP = optional(sugar)
    val servingP = required(serving).let { if (it.error == null && (it.value ?: 0.0) < 1.0) INVALID_NUMBER else it }
    val errors =
        FieldErrors(
            name = if (name.isBlank()) R.string.nutrition_field_required else null,
            kcal = kcalP.error,
            protein = proteinP.error,
            carbs = carbsP.error,
            fat = fatP.error,
            fiber = fiberP.error,
            sugar = sugarP.error,
            serving = servingP.error,
        )
    if (errors.hasErrors) return FormResult(errors, null)
    val nutrients =
        Nutrients(
            kcal = kcalP.value ?: 0.0,
            proteinG = proteinP.value ?: 0.0,
            carbsG = carbsP.value ?: 0.0,
            fatG = fatP.value ?: 0.0,
            fiberG = fiberP.value ?: 0.0,
            sugarG = sugarP.value ?: 0.0,
        )
    val food =
        FoodItem(
            id = "",
            name = name.trim(),
            category = FoodCategory.OTHER,
            per100g = nutrients,
            servingGrams = (servingP.value ?: 1.0).toInt(),
            isCustom = true,
        )
    return FormResult(errors, food)
}

/** [food] es `null` si hay errores; el `id` lo asigna quien guarda. */
internal data class FormResult(
    val errors: FieldErrors,
    val food: FoodItem?,
)

internal fun FoodItem.toForm(): CustomFoodForm =
    CustomFoodForm(
        name = name,
        kcal = per100g.kcal.asField(),
        protein = per100g.proteinG.asField(),
        carbs = per100g.carbsG.asField(),
        fat = per100g.fatG.asField(),
        fiber =
            per100g.fiberG
                .takeIf { it > 0.0 }
                ?.asField()
                .orEmpty(),
        sugar =
            per100g.sugarG
                .takeIf { it > 0.0 }
                ?.asField()
                .orEmpty(),
        serving = servingGrams.toString(),
    )

/** Sin ceros sobrantes: 52.0 se muestra como "52" y 0.3 como "0.3". */
private fun Double.asField(): String = if (this % 1.0 == 0.0) toLong().toString() else toString()

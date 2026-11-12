package com.example.lifet.feature.nutrition.ui

import androidx.annotation.StringRes
import com.example.lifet.core.model.nutrition.MealType
import com.example.lifet.feature.nutrition.R

/** Nombre localizado de cada comida; único lugar donde se traduce `MealType` a texto. */
@get:StringRes
internal val MealType.labelRes: Int
    get() =
        when (this) {
            MealType.BREAKFAST -> R.string.nutrition_meal_breakfast
            MealType.LUNCH -> R.string.nutrition_meal_lunch
            MealType.DINNER -> R.string.nutrition_meal_dinner
            MealType.SNACK -> R.string.nutrition_meal_snack
        }

package com.example.lifet.core.ui

import androidx.annotation.StringRes
import com.example.lifet.core.model.Area

/** Nombre localizado de cada área; único lugar donde se traduce `Area` a recurso de texto. */
@get:StringRes
public val Area.labelRes: Int
    get() =
        when (this) {
            Area.FINANCE -> R.string.area_finance
            Area.NUTRITION -> R.string.area_nutrition
            Area.FITNESS -> R.string.area_fitness
            Area.QUIT -> R.string.area_quit
            Area.WORK -> R.string.area_work
            Area.MOOD -> R.string.area_mood
        }

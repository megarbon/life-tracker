package com.example.lifet.core.model.nutrition

/** Fracción (0..1) de la energía que aporta cada macro; suma 1 salvo en [ZERO]. */
public data class MacroSplit(
    public val protein: Double,
    public val carbs: Double,
    public val fat: Double,
) {
    public companion object {
        public val ZERO: MacroSplit = MacroSplit(protein = 0.0, carbs = 0.0, fat = 0.0)
    }
}

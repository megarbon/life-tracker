package com.example.lifet.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/** Cifras tabulares para que los números no "bailen" al cambiar y alineen en columnas. */
private const val TABULAR_NUMBERS = "tnum"

private const val LARGE_TITLE_SIZE = 34
private const val LARGE_TITLE_LINE = 41
private const val TITLE1_SIZE = 28
private const val TITLE1_LINE = 34
private const val TITLE2_SIZE = 22
private const val TITLE2_LINE = 28
private const val TITLE3_SIZE = 20
private const val TITLE3_LINE = 25
private const val BODY_SIZE = 17
private const val BODY_LINE = 22
private const val CALLOUT_SIZE = 16
private const val CALLOUT_LINE = 21
private const val SUBHEAD_SIZE = 15
private const val SUBHEAD_LINE = 20
private const val FOOTNOTE_SIZE = 13
private const val FOOTNOTE_LINE = 18
private const val CAPTION_SIZE = 12
private const val CAPTION_LINE = 16
private const val HERO_SIZE = 48
private const val HERO_LINE = 56
private const val LARGE_TITLE_TRACKING = -0.2f

/**
 * Escala inspirada en la de iOS (Dynamic Type), siempre en `sp` para respetar el tamaño de fuente del usuario.
 * Ver docs/DESIGN_GUIDE.md, sección 3. Los pesos se limitan a Regular, Semibold y Bold.
 */
internal val LifeTrackerTypography: Typography =
    Typography().let { base ->
        base.copy(
            displaySmall = base.displaySmall.scale(HERO_SIZE, HERO_LINE, FontWeight.Bold).tabular(),
            headlineLarge =
                base.headlineLarge.scale(LARGE_TITLE_SIZE, LARGE_TITLE_LINE, FontWeight.Bold, LARGE_TITLE_TRACKING.sp),
            headlineMedium = base.headlineMedium.scale(TITLE1_SIZE, TITLE1_LINE, FontWeight.Bold).tabular(),
            headlineSmall = base.headlineSmall.scale(TITLE2_SIZE, TITLE2_LINE, FontWeight.SemiBold),
            titleLarge = base.titleLarge.scale(TITLE2_SIZE, TITLE2_LINE, FontWeight.SemiBold).tabular(),
            titleMedium = base.titleMedium.scale(TITLE3_SIZE, TITLE3_LINE, FontWeight.SemiBold),
            titleSmall = base.titleSmall.scale(BODY_SIZE, BODY_LINE, FontWeight.SemiBold),
            bodyLarge = base.bodyLarge.scale(BODY_SIZE, BODY_LINE, FontWeight.Normal).tabular(),
            bodyMedium = base.bodyMedium.scale(CALLOUT_SIZE, CALLOUT_LINE, FontWeight.Normal).tabular(),
            bodySmall = base.bodySmall.scale(FOOTNOTE_SIZE, FOOTNOTE_LINE, FontWeight.Normal),
            labelLarge = base.labelLarge.scale(SUBHEAD_SIZE, SUBHEAD_LINE, FontWeight.Normal).tabular(),
            labelMedium = base.labelMedium.scale(FOOTNOTE_SIZE, FOOTNOTE_LINE, FontWeight.Normal),
            labelSmall = base.labelSmall.scale(CAPTION_SIZE, CAPTION_LINE, FontWeight.Normal),
        )
    }

private fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = TABULAR_NUMBERS)

private fun TextStyle.scale(
    size: Int,
    line: Int,
    weight: FontWeight,
    tracking: TextUnit = 0.sp,
): TextStyle = copy(fontSize = size.sp, lineHeight = line.sp, fontWeight = weight, letterSpacing = tracking)

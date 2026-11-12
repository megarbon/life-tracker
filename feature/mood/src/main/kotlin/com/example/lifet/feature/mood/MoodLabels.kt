package com.example.lifet.feature.mood

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SentimentDissatisfied
import androidx.compose.material.icons.rounded.SentimentNeutral
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.material.icons.rounded.SentimentVeryDissatisfied
import androidx.compose.material.icons.rounded.SentimentVerySatisfied
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import com.example.lifet.core.model.mood.MoodLevel
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

internal val MoodLevel.icon: ImageVector
    get() =
        when (this) {
            MoodLevel.VERY_LOW -> Icons.Rounded.SentimentVeryDissatisfied
            MoodLevel.LOW -> Icons.Rounded.SentimentDissatisfied
            MoodLevel.NEUTRAL -> Icons.Rounded.SentimentNeutral
            MoodLevel.GOOD -> Icons.Rounded.SentimentSatisfied
            MoodLevel.VERY_GOOD -> Icons.Rounded.SentimentVerySatisfied
        }

@get:StringRes
internal val MoodLevel.labelRes: Int
    get() =
        when (this) {
            MoodLevel.VERY_LOW -> R.string.mood_level_very_low
            MoodLevel.LOW -> R.string.mood_level_low
            MoodLevel.NEUTRAL -> R.string.mood_level_neutral
            MoodLevel.GOOD -> R.string.mood_level_good
            MoodLevel.VERY_GOOD -> R.string.mood_level_very_good
        }

private val EMOTION_LABELS =
    mapOf(
        "joy" to R.string.mood_emotion_joy,
        "calm" to R.string.mood_emotion_calm,
        "gratitude" to R.string.mood_emotion_gratitude,
        "motivation" to R.string.mood_emotion_motivation,
        "love" to R.string.mood_emotion_love,
        "pride" to R.string.mood_emotion_pride,
        "tired" to R.string.mood_emotion_tired,
        "anxiety" to R.string.mood_emotion_anxiety,
        "sadness" to R.string.mood_emotion_sadness,
        "anger" to R.string.mood_emotion_anger,
        "loneliness" to R.string.mood_emotion_loneliness,
        "boredom" to R.string.mood_emotion_boredom,
    )

private val FACTOR_LABELS =
    mapOf(
        "sleep" to R.string.mood_factor_sleep,
        "sport" to R.string.mood_factor_sport,
        "work" to R.string.mood_factor_work,
        "social" to R.string.mood_factor_social,
        "family" to R.string.mood_factor_family,
        "health" to R.string.mood_factor_health,
        "food" to R.string.mood_factor_food,
        "money" to R.string.mood_factor_money,
        "nature" to R.string.mood_factor_nature,
        "hobbies" to R.string.mood_factor_hobbies,
    )

/** Las claves predefinidas se traducen; las etiquetas propias del usuario se muestran tal cual. */
@Composable
internal fun emotionLabel(key: String): String = EMOTION_LABELS[key]?.let { stringResource(it) } ?: key

@Composable
internal fun factorLabel(key: String): String = FACTOR_LABELS[key]?.let { stringResource(it) } ?: key

/** Formateadores dependientes del idioma, creados una vez por composición. */
internal class MoodFormatters(
    val locale: Locale,
) {
    private val decimal =
        NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 1
            maximumFractionDigits = 1
        }
    val fullDate: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)
    val time: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
    val shortDate: DateTimeFormatter = DateTimeFormatter.ofPattern("d/M", locale)

    fun average(value: Double): String = decimal.format(value)

    /** Diferencia con signo explícito ("+0,4", "-0,2"), para que el sentido no dependa sólo del color. */
    fun delta(value: Double): String = (if (value > 0) "+" else "") + decimal.format(value)
}

@Composable
internal fun rememberMoodFormatters(): MoodFormatters {
    val locale = LocalLocale.current.platformLocale
    return remember(locale) { MoodFormatters(locale) }
}

package com.example.lifet.feature.quit.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.designsystem.util.rememberReducedMotion
import com.example.lifet.core.model.quit.HabitMode
import com.example.lifet.core.model.quit.HabitProgress
import com.example.lifet.feature.quit.CleanTime
import com.example.lifet.feature.quit.LiveProgress
import com.example.lifet.feature.quit.R
import com.example.lifet.feature.quit.icon
import com.example.lifet.feature.quit.liveProgress
import com.example.lifet.feature.quit.millisUntilNextTick
import com.example.lifet.feature.quit.quantity
import com.example.lifet.feature.quit.rememberLiveNow
import com.example.lifet.feature.quit.rememberQuitFormatters
import com.example.lifet.feature.quit.spanText
import java.time.Clock
import java.time.Instant

private const val NUMBER_ANIMATION_MS = 200

/**
 * Héroe del resumen: el tiempo limpio en vivo (o, en modo reducción, el consumo de la semana) y el dinero ahorrado.
 * Una recaída nunca borra lo conseguido: debajo de la racha actual se muestran siempre el récord y los totales.
 *
 * El reloj vive en [LiveHeroNumbers]: sólo ese bloque se recompone cada segundo o minuto, no la tarjeta entera.
 */
@Composable
internal fun HeroCard(
    progress: HabitProgress,
    clock: Clock,
    modifier: Modifier = Modifier,
) {
    val accent = LifeTrackerTheme.areaColors.quit
    val reducing = progress.habit.mode == HabitMode.REDUCE
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(LifeTrackerTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.sm),
            ) {
                Icon(progress.habit.kind.icon, contentDescription = null, tint = accent)
                Text(
                    text =
                        stringResource(
                            if (reducing) R.string.quit_hero_title_reduce else R.string.quit_hero_title_quit,
                        ),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (reducing) {
                ReduceHero(progress, clock)
            } else {
                QuitHero(progress, clock)
            }
        }
    }
}

@Composable
private fun QuitHero(
    progress: HabitProgress,
    clock: Clock,
) {
    val formatters = rememberQuitFormatters()
    if (!progress.started) {
        val start =
            progress.habit.startedAt
                .atZone(progress.habit.zone)
                .toLocalDate()
        Text(
            text = stringResource(R.string.quit_hero_not_started, formatters.date(start)),
            style = MaterialTheme.typography.titleMedium,
        )
        return
    }
    val now = rememberLiveNow(clock) { millisUntilNextTick(progress.cleanSince, it) }
    LiveHeroNumbers(progress, now)
    // Estas líneas no cambian con el reloj, así que viven fuera del bloque en vivo.
    RecordLines(progress)
}

/** Lo único que se recompone con el reloj: el tiempo limpio y el dinero ahorrado. */
@Composable
private fun LiveHeroNumbers(
    progress: HabitProgress,
    now: State<Instant>,
) {
    val formatters = rememberQuitFormatters()
    val live = liveProgress(progress, now.value)
    val timeText = cleanTimeText(live.clean)
    val savedText = formatters.money(live.saved)
    val savedLine = stringResource(R.string.quit_hero_saved, savedText)
    val description =
        stringResource(
            R.string.quit_hero_description,
            stringResource(R.string.quit_hero_title_quit),
            timeText,
            savedLine,
        )
    Column(
        verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.xs),
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        AnimatedNumber(text = timeText)
        AnimatedNumber(text = savedLine, small = true)
    }
}

/** La cifra cambia con un deslizamiento corto; con "eliminar animaciones" sólo hay un fundido. */
@Composable
private fun AnimatedNumber(
    text: String,
    small: Boolean = false,
) {
    val reduced = rememberReducedMotion()
    val style = if (small) MaterialTheme.typography.titleLarge else MaterialTheme.typography.displaySmall
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            if (reduced) {
                fadeIn() togetherWith fadeOut()
            } else {
                (slideInVertically(tween()) { it / 2 } + fadeIn()) togetherWith
                    (slideOutVertically(tween()) { -it / 2 } + fadeOut())
            }
        },
        label = "quit-live-number",
    ) { current ->
        Text(current, style = style, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

private fun <T> tween() =
    androidx.compose.animation.core
        .tween<T>(NUMBER_ANIMATION_MS)

/** Texto del tiempo limpio: segundos el primer día, minutos después y días (y años) cuando ya es largo. */
@Composable
internal fun cleanTimeText(time: CleanTime): String =
    when {
        time.totalDays == 0L -> {
            stringResource(R.string.quit_hms, time.hours, time.minutes, time.seconds)
        }

        time.years > 0L -> {
            val years = quantity(R.plurals.quit_span_years, time.years.toInt())
            val days = quantity(R.plurals.quit_span_days, time.days.toInt())
            stringResource(R.string.quit_span_join, years, days)
        }

        else -> {
            val days = quantity(R.plurals.quit_span_days, time.days.toInt())
            stringResource(R.string.quit_span_join, days, stringResource(R.string.quit_hm, time.hours, time.minutes))
        }
    }

/** Récord y totales: lo que una recaída nunca borra, siempre a la vista y en tono de apoyo. */
@Composable
private fun RecordLines(progress: HabitProgress) {
    val supporting = MaterialTheme.colorScheme.onSurfaceVariant
    Column(verticalArrangement = Arrangement.spacedBy(LifeTrackerTheme.spacing.xs)) {
        if (progress.cleanSeconds == 0L && progress.totalCleanDays == 0 && !progress.hasSlipped) {
            Text(
                stringResource(R.string.quit_hero_started_today),
                style = MaterialTheme.typography.bodyMedium,
                color = supporting,
            )
        }
        if (progress.hasSlipped) {
            Text(
                stringResource(R.string.quit_hero_since_last, spanText(progress.cleanSeconds)),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        val isBest = progress.cleanSeconds >= progress.bestStreakSeconds
        Text(
            text =
                if (isBest && !progress.hasSlipped) {
                    quantity(R.plurals.quit_clean_days_total, progress.totalCleanDays)
                } else if (isBest) {
                    stringResource(R.string.quit_hero_record_current)
                } else {
                    stringResource(R.string.quit_hero_record, spanText(progress.bestStreakSeconds))
                },
            style = MaterialTheme.typography.bodyMedium,
            color = supporting,
        )
        if (progress.hasSlipped) {
            Text(
                quantity(R.plurals.quit_clean_days_total, progress.totalCleanDays),
                style = MaterialTheme.typography.bodyMedium,
                color = supporting,
            )
            Text(
                stringResource(R.string.quit_hero_keep_going),
                style = MaterialTheme.typography.bodySmall,
                color = supporting,
            )
        }
    }
}

/** Héroe del modo reducción: lo consumido esta semana frente al límite y lo ahorrado hasta ahora. */
@Composable
private fun ReduceHero(
    progress: HabitProgress,
    clock: Clock,
) {
    val formatters = rememberQuitFormatters()
    val week = progress.thisWeek
    val limit = week?.limit
    val units = week?.units ?: 0
    val value =
        if (limit != null) {
            stringResource(R.string.quit_reduce_week_value, units, limit)
        } else {
            stringResource(R.string.quit_reduce_week_value_no_limit, units)
        }
    val now = rememberLiveNow(clock) { millisUntilNextTick(progress.habit.startedAt, it) }
    val saved = formatters.money(liveProgress(progress, now.value).saved)
    Text(value, style = MaterialTheme.typography.displaySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    Text(
        text = reduceLine(units, limit),
        style = MaterialTheme.typography.bodyLarge,
    )
    Text(
        text = stringResource(R.string.quit_hero_saved, saved),
        style = MaterialTheme.typography.titleLarge,
    )
    Text(
        text =
            if (progress.compliance.isEmpty()) {
                stringResource(R.string.quit_reduce_compliance_none)
            } else {
                stringResource(R.string.quit_reduce_compliance, progress.compliantWeeks, progress.compliance.size)
            },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Mensaje de la semana en curso: dentro del límite, justo en él o pasado; nunca un reproche. */
@Composable
private fun reduceLine(
    units: Int,
    limit: Int?,
): String =
    when {
        limit == null -> stringResource(R.string.quit_reduce_within)
        units > limit -> stringResource(R.string.quit_reduce_over)
        units == limit -> stringResource(R.string.quit_reduce_at_limit)
        else -> quantity(R.plurals.quit_reduce_remaining, limit - units)
    }

/** Usado por las previews para fijar el progreso en vivo sin reloj. */
internal fun previewLive(
    progress: HabitProgress,
    now: Instant,
): LiveProgress = liveProgress(progress, now)

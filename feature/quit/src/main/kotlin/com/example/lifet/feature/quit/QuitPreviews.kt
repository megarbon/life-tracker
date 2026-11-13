@file:Suppress("MagicNumber", "TooManyFunctions") // Datos de ejemplo literales y una función por cada @LTPreviews.

package com.example.lifet.feature.quit

import androidx.compose.runtime.Composable
import com.example.lifet.core.designsystem.preview.LTPreviews
import com.example.lifet.core.designsystem.theme.LifeTrackerTheme
import com.example.lifet.core.model.CurrencyCode
import com.example.lifet.core.model.Money
import com.example.lifet.core.model.quit.BaselinePeriod
import com.example.lifet.core.model.quit.CopingStrategy
import com.example.lifet.core.model.quit.CravingTrigger
import com.example.lifet.core.model.quit.HabitKind
import com.example.lifet.core.model.quit.HabitMode
import com.example.lifet.core.model.quit.QuitCalculator
import com.example.lifet.core.model.quit.QuitConsumption
import com.example.lifet.core.model.quit.QuitCraving
import com.example.lifet.core.model.quit.QuitDataSet
import com.example.lifet.core.model.quit.QuitHabit
import com.example.lifet.core.model.quit.QuitOverview
import com.example.lifet.core.model.quit.QuitRules
import kotlinx.coroutines.flow.emptyFlow
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

private val PREVIEW_ZONE: ZoneId = ZoneId.of("Europe/Madrid")
private val PREVIEW_NOW: Instant = Instant.parse("2026-03-10T15:00:00Z")
private val PREVIEW_CLOCK: Clock = Clock.fixed(PREVIEW_NOW, PREVIEW_ZONE)
private val PREVIEW_TODAY: LocalDate = LocalDate.of(2026, 3, 10)
private const val LONG_TEXT_REPEATS = 14

private fun at(
    daysAgo: Long,
    time: LocalTime,
): Instant =
    PREVIEW_TODAY
        .minusDays(daysAgo)
        .atTime(time)
        .atZone(PREVIEW_ZONE)
        .toInstant()

private fun tobacco(
    daysAgo: Long,
    name: String = "Tabaco",
    price: Long = 540,
): QuitHabit =
    QuitHabit(
        id = "tobacco",
        kind = HabitKind.TOBACCO,
        name = name,
        mode = HabitMode.QUIT,
        startedAt = at(daysAgo, LocalTime.of(8, 30)),
        zone = PREVIEW_ZONE,
        baselineUnits = 20,
        baselinePeriod = BaselinePeriod.DAY,
        unitsPerPack = 20,
        packPrice = Money(price, CurrencyCode.EUR),
    )

private fun alcohol(): QuitHabit =
    QuitHabit(
        id = "alcohol",
        kind = HabitKind.ALCOHOL,
        name = "Alcohol",
        mode = HabitMode.REDUCE,
        startedAt = at(84, LocalTime.of(10, 0)),
        zone = PREVIEW_ZONE,
        baselineUnits = 14,
        baselinePeriod = BaselinePeriod.WEEK,
        unitsPerPack = 1,
        packPrice = Money(450, CurrencyCode.EUR),
        weeklyLimit = 4,
    )

private val TRIGGERS = CravingTrigger.entries

/** Antojos repartidos por horas y disparadores con una intensidad que baja con el tiempo. */
private fun cravings(
    habitId: String,
    days: Int,
): List<QuitCraving> =
    (0 until days).flatMap { day ->
        listOf(9, 14, 20).take(1 + day % 3).mapIndexed { index, hour ->
            QuitCraving(
                id = "c-$habitId-$day-$index",
                habitId = habitId,
                at = at(day.toLong(), LocalTime.of(hour, 10 * index)),
                zone = PREVIEW_ZONE,
                intensity = (4 - day / 15).coerceIn(1, 5),
                trigger = TRIGGERS[(day + index) % TRIGGERS.size],
                strategy = CopingStrategy.entries.getOrNull((day + index) % (CopingStrategy.entries.size + 1)),
            )
        }
    }

private fun slip(
    id: String,
    habitId: String,
    daysAgo: Long,
    units: Int,
    note: String = "",
): QuitConsumption =
    QuitConsumption(id, habitId, at(daysAgo, LocalTime.of(21, 40)), PREVIEW_ZONE, units, CravingTrigger.SOCIAL, note)

private fun regularData(longTexts: Boolean): QuitDataSet {
    val long =
        if (longTexts) {
            "Cena larguísima con amigos, todos fumaban y fue difícil 🚬😮‍💨 ".repeat(
                LONG_TEXT_REPEATS,
            )
        } else {
            ""
        }
    val smoke = tobacco(47, name = if (longTexts) "Dejar de fumar para siempre 🚭 ".repeat(3) else "Tabaco")
    val drink = alcohol()
    return QuitDataSet(
        habits = listOf(smoke, drink),
        cravings = cravings(smoke.id, 47) + cravings(drink.id, 20),
        consumptions =
            listOf(
                slip("s1", smoke.id, 35, 3, long),
                slip("d1", drink.id, 3, 3),
                slip("d2", drink.id, 10, 5),
                slip("d3", drink.id, 17, 2),
            ),
    )
}

private fun overviewOf(
    data: QuitDataSet,
    now: Instant = PREVIEW_NOW,
): QuitOverview = QuitCalculator(data, now, PREVIEW_ZONE).overview()

@Composable
private fun PreviewScreen(
    state: QuitUiState,
    editor: QuitEditor? = null,
    tab: QuitTab = QuitTab.SUMMARY,
    clock: Clock = PREVIEW_CLOCK,
) {
    LifeTrackerTheme {
        QuitScreen(
            state = state,
            editor = editor,
            effects = emptyFlow(),
            clock = clock,
            onAction = {},
            initialTab = tab,
        )
    }
}

private fun eventEditor(
    habitId: String,
    asConsumption: Boolean = false,
) = QuitEditor.Event(
    habitId = habitId,
    existing = null,
    asConsumption = asConsumption,
    seedStrategy = null,
    newId = "preview",
    now = PREVIEW_NOW,
    today = PREVIEW_TODAY,
    zone = PREVIEW_ZONE,
    nowMinute = 16 * 60,
)

@LTPreviews
@Composable
private fun QuitSummaryPreview() {
    PreviewScreen(QuitUiState.Success(overviewOf(regularData(longTexts = false))))
}

@LTPreviews
@Composable
private fun QuitSummaryEdgeCasesPreview() {
    PreviewScreen(QuitUiState.Success(overviewOf(regularData(longTexts = true))), tab = QuitTab.HISTORY)
}

@LTPreviews
@Composable
private fun QuitSummaryJustStartedPreview() {
    val data = QuitDataSet(habits = listOf(tobacco(0)))
    val now = PREVIEW_NOW.plusSeconds(90)
    PreviewScreen(QuitUiState.Success(overviewOf(data, now)), clock = Clock.fixed(now, PREVIEW_ZONE))
}

@LTPreviews
@Composable
private fun QuitSummaryYearsPreview() {
    val data = QuitDataSet(habits = listOf(tobacco(2300, price = QuitRules.MAX_PACK_PRICE_MINOR)))
    PreviewScreen(QuitUiState.Success(overviewOf(data)))
}

@LTPreviews
@Composable
private fun QuitReductionPreview() {
    val drink = alcohol()
    val data = regularData(longTexts = false).copy(habits = listOf(drink))
    PreviewScreen(QuitUiState.Success(overviewOf(data)))
}

@LTPreviews
@Composable
private fun QuitMilestonesPreview() {
    PreviewScreen(QuitUiState.Success(overviewOf(regularData(longTexts = false))), tab = QuitTab.MILESTONES)
}

@LTPreviews
@Composable
private fun QuitHistoryPreview() {
    PreviewScreen(QuitUiState.Success(overviewOf(regularData(longTexts = false))), tab = QuitTab.HISTORY)
}

@LTPreviews
@Composable
private fun QuitEmptyPreview() {
    PreviewScreen(QuitUiState.Empty)
}

@LTPreviews
@Composable
private fun QuitLoadingPreview() {
    PreviewScreen(QuitUiState.Loading)
}

@LTPreviews
@Composable
private fun QuitErrorPreview() {
    PreviewScreen(QuitUiState.Error(R.string.quit_error_load))
}

@LTPreviews
@Composable
private fun QuitCravingSheetPreview() {
    PreviewScreen(QuitUiState.Success(overviewOf(regularData(false))), editor = eventEditor("tobacco"))
}

@LTPreviews
@Composable
private fun QuitConsumptionSheetPreview() {
    PreviewScreen(
        QuitUiState.Success(overviewOf(regularData(false))),
        editor = eventEditor("tobacco", asConsumption = true),
    )
}

@LTPreviews
@Composable
private fun QuitHabitSheetPreview() {
    val editor =
        QuitEditor.Habit(
            existing = null,
            kind = HabitKind.ALCOHOL,
            currency = "EUR",
            newId = "preview",
            now = PREVIEW_NOW,
            today = PREVIEW_TODAY,
            zone = PREVIEW_ZONE,
            nowMinute = 16 * 60,
        )
    PreviewScreen(QuitUiState.Success(overviewOf(regularData(false))), editor = editor)
}

@LTPreviews
@Composable
private fun QuitSosPreview() {
    PreviewScreen(QuitUiState.Success(overviewOf(regularData(false))), editor = QuitEditor.Sos("tobacco"))
}

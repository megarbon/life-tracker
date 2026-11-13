package com.example.lifet.feature.quit

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.NoDrinks
import androidx.compose.material.icons.rounded.SmokeFree
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.lifet.core.model.quit.BaselinePeriod
import com.example.lifet.core.model.quit.CopingStrategy
import com.example.lifet.core.model.quit.CravingTrigger
import com.example.lifet.core.model.quit.HabitKind
import com.example.lifet.core.model.quit.HabitMode
import com.example.lifet.core.model.quit.MilestoneKey
import com.example.lifet.core.model.quit.QuitConsumption
import com.example.lifet.core.model.quit.QuitCraving
import com.example.lifet.core.model.quit.QuitEvent

internal val HabitKind.icon: ImageVector
    get() =
        when (this) {
            HabitKind.TOBACCO -> Icons.Rounded.SmokeFree
            HabitKind.ALCOHOL -> Icons.Rounded.NoDrinks
        }

/** Un antojo es una ola que pasa; un consumo, una marca en el camino. Ninguno lleva un icono de alarma. */
internal val QuitEvent.icon: ImageVector
    get() =
        when (this) {
            is QuitCraving -> Icons.Rounded.Waves
            is QuitConsumption -> Icons.Rounded.Flag
        }

@get:StringRes
internal val HabitKind.labelRes: Int
    get() =
        when (this) {
            HabitKind.TOBACCO -> R.string.quit_kind_tobacco
            HabitKind.ALCOHOL -> R.string.quit_kind_alcohol
        }

@get:StringRes
internal val HabitMode.labelRes: Int
    get() =
        when (this) {
            HabitMode.QUIT -> R.string.quit_mode_quit
            HabitMode.REDUCE -> R.string.quit_mode_reduce
        }

@get:StringRes
internal val BaselinePeriod.labelRes: Int
    get() =
        when (this) {
            BaselinePeriod.DAY -> R.string.quit_period_day
            BaselinePeriod.WEEK -> R.string.quit_period_week
        }

@get:StringRes
internal val CravingTrigger.labelRes: Int
    get() =
        when (this) {
            CravingTrigger.STRESS -> R.string.quit_trigger_stress
            CravingTrigger.EMOTIONS -> R.string.quit_trigger_emotions
            CravingTrigger.SOCIAL -> R.string.quit_trigger_social
            CravingTrigger.BOREDOM -> R.string.quit_trigger_boredom
            CravingTrigger.AFTER_MEAL -> R.string.quit_trigger_after_meal
            CravingTrigger.COFFEE -> R.string.quit_trigger_coffee
            CravingTrigger.WORK_BREAK -> R.string.quit_trigger_work_break
            CravingTrigger.TIREDNESS -> R.string.quit_trigger_tiredness
            CravingTrigger.ROUTINE -> R.string.quit_trigger_routine
            CravingTrigger.OTHER -> R.string.quit_trigger_other
        }

@get:StringRes
internal val CopingStrategy.labelRes: Int
    get() =
        when (this) {
            CopingStrategy.BREATHING -> R.string.quit_strategy_breathing
            CopingStrategy.WALK -> R.string.quit_strategy_walk
            CopingStrategy.WATER -> R.string.quit_strategy_water
            CopingStrategy.CALL_SOMEONE -> R.string.quit_strategy_call_someone
            CopingStrategy.DISTRACTION -> R.string.quit_strategy_distraction
            CopingStrategy.EXERCISE -> R.string.quit_strategy_exercise
            CopingStrategy.WAIT -> R.string.quit_strategy_wait
            CopingStrategy.SNACK -> R.string.quit_strategy_snack
            CopingStrategy.OTHER -> R.string.quit_strategy_other
        }

@get:StringRes
internal val MilestoneKey.labelRes: Int
    get() =
        when (this) {
            MilestoneKey.HOURS_24 -> R.string.quit_milestone_24h
            MilestoneKey.HOURS_48 -> R.string.quit_milestone_48h
            MilestoneKey.HOURS_72 -> R.string.quit_milestone_72h
            MilestoneKey.WEEK_1 -> R.string.quit_milestone_1w
            MilestoneKey.WEEKS_2 -> R.string.quit_milestone_2w
            MilestoneKey.MONTH_1 -> R.string.quit_milestone_1m
            MilestoneKey.MONTHS_3 -> R.string.quit_milestone_3m
            MilestoneKey.MONTHS_6 -> R.string.quit_milestone_6m
            MilestoneKey.YEAR_1 -> R.string.quit_milestone_1y
            MilestoneKey.YEARS_2 -> R.string.quit_milestone_2y
            MilestoneKey.YEARS_5 -> R.string.quit_milestone_5y
        }

@get:StringRes
internal val MilestoneKey.noteRes: Int
    get() =
        when (this) {
            MilestoneKey.HOURS_24 -> R.string.quit_milestone_24h_note
            MilestoneKey.HOURS_48 -> R.string.quit_milestone_48h_note
            MilestoneKey.HOURS_72 -> R.string.quit_milestone_72h_note
            MilestoneKey.WEEK_1 -> R.string.quit_milestone_1w_note
            MilestoneKey.WEEKS_2 -> R.string.quit_milestone_2w_note
            MilestoneKey.MONTH_1 -> R.string.quit_milestone_1m_note
            MilestoneKey.MONTHS_3 -> R.string.quit_milestone_3m_note
            MilestoneKey.MONTHS_6 -> R.string.quit_milestone_6m_note
            MilestoneKey.YEAR_1 -> R.string.quit_milestone_1y_note
            MilestoneKey.YEARS_2 -> R.string.quit_milestone_2y_note
            MilestoneKey.YEARS_5 -> R.string.quit_milestone_5y_note
        }

@get:StringRes
internal val DraftIssue.messageRes: Int
    get() =
        when (this) {
            DraftIssue.BASELINE_MISSING -> R.string.quit_issue_baseline_missing
            DraftIssue.BASELINE_INVALID -> R.string.quit_issue_baseline_invalid
            DraftIssue.PACK_SIZE_MISSING -> R.string.quit_issue_pack_size_missing
            DraftIssue.PACK_SIZE_INVALID -> R.string.quit_issue_pack_size_invalid
            DraftIssue.PRICE_MISSING -> R.string.quit_issue_price_missing
            DraftIssue.PRICE_INVALID -> R.string.quit_issue_price_invalid
            DraftIssue.CURRENCY_INVALID -> R.string.quit_issue_currency_invalid
            DraftIssue.LIMIT_MISSING -> R.string.quit_issue_limit_missing
            DraftIssue.LIMIT_INVALID -> R.string.quit_issue_limit_invalid
            DraftIssue.START_IN_FUTURE -> R.string.quit_issue_start_in_future
            DraftIssue.UNITS_MISSING -> R.string.quit_issue_units_missing
            DraftIssue.UNITS_INVALID -> R.string.quit_issue_units_invalid
            DraftIssue.EVENT_IN_FUTURE -> R.string.quit_issue_event_in_future
            DraftIssue.EVENT_BEFORE_START -> R.string.quit_issue_event_before_start
        }

/** Intensidad de 1 a 5 como palabra ("muy suave"... "muy fuerte"). */
@StringRes
internal fun intensityLabelRes(intensity: Int): Int =
    when (intensity) {
        1 -> R.string.quit_intensity_1
        2 -> R.string.quit_intensity_2
        3 -> R.string.quit_intensity_3
        4 -> R.string.quit_intensity_4
        else -> R.string.quit_intensity_5
    }

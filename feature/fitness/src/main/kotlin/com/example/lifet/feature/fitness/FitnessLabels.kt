package com.example.lifet.feature.fitness

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Hiking
import androidx.compose.material.icons.rounded.Pool
import androidx.compose.material.icons.rounded.Rowing
import androidx.compose.material.icons.rounded.SportsGymnastics
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.lifet.core.model.fitness.WorkoutType

internal val WorkoutType.icon: ImageVector
    get() =
        when (this) {
            WorkoutType.STRENGTH -> Icons.Rounded.FitnessCenter
            WorkoutType.RUN -> Icons.AutoMirrored.Rounded.DirectionsRun
            WorkoutType.WALK -> Icons.AutoMirrored.Rounded.DirectionsWalk
            WorkoutType.CYCLING -> Icons.AutoMirrored.Rounded.DirectionsBike
            WorkoutType.SWIM -> Icons.Rounded.Pool
            WorkoutType.HIKE -> Icons.Rounded.Hiking
            WorkoutType.ROW -> Icons.Rounded.Rowing
            WorkoutType.OTHER -> Icons.Rounded.SportsGymnastics
        }

@get:StringRes
internal val WorkoutType.labelRes: Int
    get() =
        when (this) {
            WorkoutType.STRENGTH -> R.string.fitness_type_strength
            WorkoutType.RUN -> R.string.fitness_type_run
            WorkoutType.WALK -> R.string.fitness_type_walk
            WorkoutType.CYCLING -> R.string.fitness_type_cycling
            WorkoutType.SWIM -> R.string.fitness_type_swim
            WorkoutType.HIKE -> R.string.fitness_type_hike
            WorkoutType.ROW -> R.string.fitness_type_row
            WorkoutType.OTHER -> R.string.fitness_type_other
        }

@get:StringRes
internal val DraftIssue.messageRes: Int
    get() =
        when (this) {
            DraftIssue.DURATION_MISSING -> R.string.fitness_issue_duration_missing
            DraftIssue.DURATION_INVALID -> R.string.fitness_issue_duration_invalid
            DraftIssue.DISTANCE_INVALID -> R.string.fitness_issue_distance_invalid
            DraftIssue.ENERGY_INVALID -> R.string.fitness_issue_energy_invalid
            DraftIssue.EXERCISE_NAME_MISSING -> R.string.fitness_issue_exercise_name_missing
            DraftIssue.EXERCISE_WITHOUT_SETS -> R.string.fitness_issue_exercise_without_sets
            DraftIssue.SET_INVALID -> R.string.fitness_issue_set_invalid
            DraftIssue.WEIGHT_MISSING -> R.string.fitness_issue_weight_missing
            DraftIssue.WEIGHT_INVALID -> R.string.fitness_issue_weight_invalid
            DraftIssue.STEPS_MISSING -> R.string.fitness_issue_steps_missing
            DraftIssue.STEPS_INVALID -> R.string.fitness_issue_steps_invalid
            DraftIssue.TEMPLATE_NEEDS_TITLE -> R.string.fitness_issue_template_needs_title
        }

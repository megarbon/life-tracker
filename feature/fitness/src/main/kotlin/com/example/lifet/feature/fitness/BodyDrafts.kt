package com.example.lifet.feature.fitness

import com.example.lifet.core.model.fitness.BodyWeight
import com.example.lifet.core.model.fitness.DailySteps
import com.example.lifet.core.model.fitness.FitnessRules
import java.io.Serializable
import java.time.LocalDate

private const val KG_SCALE = 3

/** Peso corporal tecleado en kg (hasta tres decimales: se guarda en gramos). */
internal data class WeightDraft(
    val date: LocalDate,
    val weightText: String = "",
    val note: String = "",
) : Serializable {
    private companion object {
        private const val serialVersionUID = 1L
    }
}

internal data class StepsDraft(
    val date: LocalDate,
    val stepsText: String = "",
) : Serializable {
    private companion object {
        private const val serialVersionUID = 1L
    }
}

internal fun WeightDraft.validate(): DraftResult<BodyWeight> {
    val collector = IssueCollector()
    val grams = parseScaled(weightText, KG_SCALE)
    val valid = grams != null && grams in FitnessRules.MIN_BODY_WEIGHT_GRAMS..FitnessRules.MAX_BODY_WEIGHT_GRAMS
    when {
        weightText.isBlank() -> collector.add(DraftIssue.WEIGHT_MISSING)
        !valid -> collector.add(DraftIssue.WEIGHT_INVALID)
    }
    return collector.result {
        grams?.takeIf { valid }?.let { BodyWeight(date, it, note.trim().take(FitnessRules.NOTE_MAX_LENGTH)) }
    }
}

internal fun StepsDraft.validate(): DraftResult<DailySteps> {
    val collector = IssueCollector()
    val steps =
        collector.whole(
            stepsText,
            0..FitnessRules.MAX_STEPS,
            DraftIssue.STEPS_MISSING,
            DraftIssue.STEPS_INVALID,
        )
    return collector.result { steps?.let { DailySteps(date, it) } }
}

internal fun BodyWeight.toDraft(): WeightDraft = WeightDraft(date, weightGrams.toEditableText(KG_SCALE), note)

internal fun DailySteps.toDraft(): StepsDraft = StepsDraft(date, steps.toString())

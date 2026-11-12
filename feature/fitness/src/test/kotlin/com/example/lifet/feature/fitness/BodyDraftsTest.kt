package com.example.lifet.feature.fitness

import com.example.lifet.core.model.fitness.BodyWeight
import com.example.lifet.core.model.fitness.DailySteps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BodyDraftsTest {
    private val today = LocalDate.of(2026, 3, 10)

    private fun weight(text: String) = WeightDraft(today, text).validate()

    @Test
    fun `weight in kilos converts to grams`() {
        val result = weight("81,35") as DraftResult.Valid

        assertEquals(BodyWeight(today, 81_350), result.value)
    }

    @Test
    fun `weight is required and must be a plausible body weight`() {
        assertEquals(setOf(DraftIssue.WEIGHT_MISSING), weight("").issues())
        assertEquals(setOf(DraftIssue.WEIGHT_INVALID), weight("abc").issues())
        assertEquals(setOf(DraftIssue.WEIGHT_INVALID), weight("5").issues())
        assertEquals(setOf(DraftIssue.WEIGHT_INVALID), weight("900").issues())
        assertTrue(weight("20").issues().isEmpty())
        assertTrue(weight("500").issues().isEmpty())
    }

    @Test
    fun `weight keeps a trimmed note and limits grams precision to three decimals`() {
        val result = WeightDraft(today, "80", "  tras correr ").validate() as DraftResult.Valid

        assertEquals("tras correr", result.value.note)
        assertEquals(setOf(DraftIssue.WEIGHT_INVALID), weight("80,1234").issues())
    }

    @Test
    fun `steps accept zero up to the limit`() {
        assertEquals(DailySteps(today, 0), (StepsDraft(today, "0").validate() as DraftResult.Valid).value)
        assertEquals(setOf(DraftIssue.STEPS_MISSING), StepsDraft(today, "").validate().issues())
        assertEquals(setOf(DraftIssue.STEPS_INVALID), StepsDraft(today, "-1").validate().issues())
        assertEquals(setOf(DraftIssue.STEPS_INVALID), StepsDraft(today, "200001").validate().issues())
        assertEquals(setOf(DraftIssue.STEPS_INVALID), StepsDraft(today, "8,5").validate().issues())
    }

    @Test
    fun `stored values return to their editable text`() {
        assertEquals(WeightDraft(today, "81.35", "x"), BodyWeight(today, 81_350, "x").toDraft())
        assertEquals(StepsDraft(today, "8000"), DailySteps(today, 8_000).toDraft())
    }
}

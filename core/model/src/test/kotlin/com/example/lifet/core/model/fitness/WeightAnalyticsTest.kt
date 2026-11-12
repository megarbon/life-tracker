package com.example.lifet.core.model.fitness

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeightAnalyticsTest {
    private fun weight(
        daysAgo: Long,
        grams: Long,
    ) = BodyWeight(TODAY.minusDays(daysAgo), grams)

    @Test
    fun `no weights gives an empty progress`() {
        val progress = WeightAnalytics.progress(emptyList())

        assertTrue(progress.points.isEmpty())
        assertNull(progress.latest)
        assertNull(progress.changeGrams)
    }

    @Test
    fun `moving average covers the seven days ending on each date`() {
        val progress =
            WeightAnalytics.progress(
                listOf(weight(10, 80_000), weight(2, 79_000), weight(1, 78_000), weight(0, 77_000)),
            )

        // Hace 10 días queda fuera de la ventana de 7 días de hoy.
        assertEquals(80_000L, progress.points.first().movingAverageGrams)
        assertEquals(78_000L, progress.latest?.movingAverageGrams)
    }

    @Test
    fun `points are sorted by date`() {
        val progress = WeightAnalytics.progress(listOf(weight(0, 77_000), weight(5, 78_000)))

        assertEquals(listOf(TODAY.minusDays(5), TODAY), progress.points.map { it.date })
    }

    @Test
    fun `change compares with the record closest to thirty days ago`() {
        val progress =
            WeightAnalytics.progress(listOf(weight(31, 82_000), weight(0, 80_000)))

        assertEquals(-2_000L, progress.changeGrams)
    }

    @Test
    fun `change is unknown when nothing was recorded around thirty days ago`() {
        val progress = WeightAnalytics.progress(listOf(weight(10, 82_000), weight(0, 80_000)))

        assertNull(progress.changeGrams)
    }

    @Test
    fun `a single record has no change`() {
        assertNull(WeightAnalytics.progress(listOf(weight(0, 80_000))).changeGrams)
    }
}

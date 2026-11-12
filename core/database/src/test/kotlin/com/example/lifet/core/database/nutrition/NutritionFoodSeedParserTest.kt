package com.example.lifet.core.database.nutrition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionFoodSeedParserTest {
    private val header = "id;name;category;kcal;protein;carbs;fat;fiber;sugar;serving_g"

    @Test
    fun `parses a valid row by column name`() {
        val foods = parseFoodSeedCsv("$header\nseed_x;Pan;GRAIN;265;9;49;3.2;2.7;5;40")

        assertEquals(1, foods.size)
        val food = foods.single()
        assertEquals("seed_x", food.id)
        assertEquals(265.0, food.kcalPer100g, 0.0)
        assertEquals(40, food.servingGrams)
        assertEquals(false, food.isCustom)
    }

    @Test
    fun `skips malformed rows without dropping the rest`() {
        val csv =
            """
            $header
            ok_1;Bueno;FRUIT;50;1;10;0.5;1;5;100
            bad_number;Malo;FRUIT;abc;1;10;0.5;1;5;100
            short;Corto;FRUIT;50
            ;SinId;FRUIT;50;1;10;0.5;1;5;100
            ok_2;Otro;FRUIT;60;1;10;0.5;1;5;100
            """.trimIndent()

        assertEquals(listOf("ok_1", "ok_2"), parseFoodSeedCsv(csv).map { it.id })
    }

    @Test
    fun `empty input gives no foods`() {
        assertTrue(parseFoodSeedCsv("").isEmpty())
    }
}

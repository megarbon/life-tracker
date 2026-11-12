package com.example.lifet.feature.nutrition

import app.cash.turbine.ReceiveTurbine
import com.example.lifet.core.domain.GetTodayUseCase
import com.example.lifet.core.domain.nutrition.AddWater
import com.example.lifet.core.domain.nutrition.LogFood
import com.example.lifet.core.domain.nutrition.ManageDiaryEntries
import com.example.lifet.core.domain.nutrition.ObserveNutritionDay
import com.example.lifet.core.domain.nutrition.ObserveNutritionDiary
import com.example.lifet.core.domain.nutrition.ObserveNutritionSeries
import com.example.lifet.core.domain.nutrition.RepeatYesterdayMeals
import com.example.lifet.core.testing.TestClock
import com.example.lifet.core.testing.nutrition.FakeFoodRepository
import com.example.lifet.core.testing.nutrition.FakeNutritionGoalsRepository
import com.example.lifet.core.testing.nutrition.FakeNutritionLogRepository
import java.time.Clock
import java.time.LocalDate

/** Reloj fijo: 2026-03-10 10:00 en Madrid. */
internal val testClock: Clock = TestClock.fixed("2026-03-10T09:00:00Z")
internal val today: LocalDate = LocalDate.of(2026, 3, 10)

/** Repositorios falsos y casos de uso reales montados encima, como en producción. */
internal class NutritionTestEnv {
    val log = FakeNutritionLogRepository()
    val goals = FakeNutritionGoalsRepository()
    val foods = FakeFoodRepository()
    val getToday = GetTodayUseCase(testClock)
    val logFood = LogFood(log, testClock)
    val addWater = AddWater(log, testClock)
    val repeatYesterday = RepeatYesterdayMeals(log)
    val manage = ManageDiaryEntries(log)
    val observeSeries = ObserveNutritionSeries(log, goals)
    val observeDiary = ObserveNutritionDiary(ObserveNutritionDay(log, goals), observeSeries)
}

/** Descarta emisiones hasta encontrar una de tipo [T] (p. ej. saltar `Loading`). */
internal suspend inline fun <reified T> ReceiveTurbine<*>.awaitInstance(): T {
    while (true) {
        val item = awaitItem()
        if (item is T) return item
    }
}

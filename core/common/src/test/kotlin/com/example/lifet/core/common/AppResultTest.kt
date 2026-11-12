package com.example.lifet.core.common

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class AppResultTest {
    @Test
    fun `runCatchingApp wraps a returned value in Success`() {
        assertEquals(AppResult.Success(42), runCatchingApp { 42 })
    }

    @Test
    fun `runCatchingApp maps exceptions with the provided mapper`() {
        val result = runCatchingApp(onError = { AppError.Storage }) { error("boom") }

        assertEquals(AppResult.Failure(AppError.Storage), result)
    }

    @Test
    fun `runCatchingApp falls back to Unknown without a mapper`() {
        val result = runCatchingApp { error("boom") }

        assertEquals(AppResult.Failure(AppError.Unknown), result)
    }

    @Test
    fun `runCatchingApp never swallows cancellation`() {
        assertThrows(CancellationException::class.java) {
            runCatchingApp { throw CancellationException("cancelled") }
        }
    }

    @Test
    fun `map transforms Success and leaves Failure untouched`() {
        assertEquals(AppResult.Success(3), AppResult.Success(2).map { it + 1 })
        val failure: AppResult<Int> = AppResult.Failure(AppError.NotFound)
        assertEquals(failure, failure.map { it + 1 })
    }

    @Test
    fun `getOrNull returns null on failure`() {
        val failure: AppResult<Int> = AppResult.Failure(AppError.Network)

        assertNull(failure.getOrNull())
        assertEquals(7, AppResult.Success(7).getOrNull())
    }
}

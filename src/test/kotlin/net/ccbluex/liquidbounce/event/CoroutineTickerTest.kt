/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import java.util.concurrent.Executor
import java.util.function.IntPredicate
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CoroutineTickerTest {

    private fun assertFailure(expected: Throwable, actual: Throwable?) {
        // Coroutine debug mode may copy an exception to add its suspension stack, retaining the original as cause.
        assertTrue(actual === expected || actual?.cause === expected, "The original failure was not delivered")
    }

    private fun ticker(): Ticker = CoroutineTickerImpl(Executor { it.run() })

    private fun CoroutineScope.waiter(ticker: Ticker, stopAt: IntPredicate): Deferred<Result<Int>> =
        async(start = CoroutineStart.UNDISPATCHED) {
            runCatching { ticker.tickUntil(stopAt) }
        }

    @Test
    fun `predicate failures complete the waiter with the original exception`() = runTest {
        val ticker = ticker()
        val failure = IllegalArgumentException("predicate failed")
        val result = waiter(ticker) { throw failure }

        ticker.tick()

        assertFailure(failure, result.await().exceptionOrNull())
    }

    @Test
    fun `predicate failures after earlier ticks do not leave the waiter suspended`() = runTest {
        val ticker = ticker()
        val failure = IllegalStateException("second tick failed")
        val ticks = mutableListOf<Int>()
        val result = waiter(ticker) {
            ticks += it
            if (it == 2) throw failure
            false
        }

        ticker.tick()
        assertFalse(result.isCompleted)
        ticker.tick()

        assertFailure(failure, result.await().exceptionOrNull())
        assertEquals(listOf(1, 2), ticks)
    }

    @Test
    fun `a failed predicate is removed from the ticker`() = runTest {
        val ticker = ticker()
        var calls = 0
        val result = waiter(ticker) {
            calls++
            error("failed")
        }

        ticker.tick()
        result.await()
        ticker.tick()
        assertEquals(1, calls)
    }

    @Test
    fun `cancellation thrown by the predicate reaches the waiter`() = runTest {
        val ticker = ticker()
        val failure = CancellationException("predicate cancelled")
        val result = waiter(ticker) { throw failure }

        ticker.tick()

        assertFailure(failure, result.await().exceptionOrNull())
    }

    @Test
    fun `successful predicates report the elapsed ticks once`() = runTest {
        val ticker = ticker()
        var calls = 0
        val result = waiter(ticker) {
            calls++
            it >= 3
        }

        ticker.tick()
        ticker.tick()
        assertFalse(result.isCompleted)
        ticker.tick()

        assertEquals(3, result.await().getOrThrow())
        ticker.tick()
        assertEquals(3, calls)
    }

    @Test
    fun `cancelled waiters are removed without evaluating the predicate`() = runTest {
        val ticker = ticker()
        var calls = 0
        val result = waiter(ticker) {
            calls++
            false
        }

        result.cancel()
        ticker.tick()
        ticker.tick()

        assertEquals(0, calls)
    }

    @Test
    fun `predicate errors run the waiting coroutines cleanup`() = runTest {
        val ticker = ticker()
        var cleanedUp = false
        val failure = IllegalStateException("failed")
        val result = async(start = CoroutineStart.UNDISPATCHED) {
            runCatching {
                try {
                    ticker.tickUntil { throw failure }
                } finally {
                    cleanedUp = true
                }
            }
        }

        ticker.tick()

        assertFailure(failure, result.await().exceptionOrNull())
        assertTrue(cleanedUp)
    }

    @Test
    fun `tasks registered during a tick start on the next tick`() {
        val ticker = ticker()
        val calls = mutableListOf<String>()
        ticker.register {
            calls += "first"
            ticker.register {
                calls += "second"
                true
            }
            true
        }

        ticker.tick()
        assertEquals(listOf("first"), calls)
        ticker.tick()
        assertEquals(listOf("first", "second"), calls)
        ticker.tick()
        assertEquals(listOf("first", "second"), calls)
    }
}

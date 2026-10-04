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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.runTest
import java.util.function.IntPredicate
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TickUntilCallbackTest {

    private fun assertFailure(expected: Throwable, actual: Throwable?) {
        // Coroutine debug mode may copy an exception to add its suspension stack, retaining the original as cause.
        assertTrue(actual === expected || actual?.cause === expected, "The original failure was not delivered")
    }

    // Drive the actual callback with a real cancellable continuation, without a Minecraft tick loop.
    private fun CoroutineScope.waiter(stopAt: IntPredicate): Pair<Deferred<Result<Int>>, TickUntilCallback> {
        lateinit var callback: TickUntilCallback
        val result = async(start = CoroutineStart.UNDISPATCHED) {
            runCatching {
                suspendCancellableCoroutine<Int> { continuation ->
                    callback = TickUntilCallback(continuation, stopAt)
                }
            }
        }
        return result to callback
    }

    @Test
    fun `predicate failures complete the waiter with the original exception`() = runTest {
        val failure = IllegalArgumentException("predicate failed")
        val (result, callback) = waiter { throw failure }

        assertTrue(callback.asBoolean)

        assertFailure(failure, result.await().exceptionOrNull())
    }

    @Test
    fun `predicate failures after earlier ticks do not leave the waiter suspended`() = runTest {
        val failure = IllegalStateException("second tick failed")
        val ticks = mutableListOf<Int>()
        val (result, callback) = waiter {
            ticks += it
            if (it == 2) throw failure
            false
        }

        assertFalse(callback.asBoolean)
        assertTrue(callback.asBoolean)

        assertFailure(failure, result.await().exceptionOrNull())
        assertEquals(listOf(1, 2), ticks)
    }

    @Test
    fun `a failed predicate is never called again`() = runTest {
        var calls = 0
        val (result, callback) = waiter {
            calls++
            error("failed")
        }

        assertTrue(callback.asBoolean)
        result.await()
        assertTrue(callback.asBoolean)
        assertEquals(1, calls)
    }

    @Test
    fun `cancellation thrown by the predicate reaches the waiter`() = runTest {
        val failure = CancellationException("predicate cancelled")
        val (result, callback) = waiter { throw failure }

        assertTrue(callback.asBoolean)

        assertFailure(failure, result.await().exceptionOrNull())
    }

    @Test
    fun `successful predicates report the elapsed ticks once`() = runTest {
        var calls = 0
        val (result, callback) = waiter {
            calls++
            it >= 3
        }

        assertFalse(callback.asBoolean)
        assertFalse(callback.asBoolean)
        assertTrue(callback.asBoolean)

        assertEquals(3, result.await().getOrThrow())
        assertTrue(callback.asBoolean)
        assertEquals(3, calls)
    }

    @Test
    fun `cancelled waiters are removed without evaluating the predicate`() = runTest {
        var calls = 0
        val (result, callback) = waiter {
            calls++
            false
        }

        result.cancel()

        assertTrue(callback.asBoolean)
        assertEquals(0, calls)
    }

    @Test
    fun `predicate errors run the waiting coroutines cleanup`() = runTest {
        lateinit var callback: TickUntilCallback
        var cleanedUp = false
        val failure = IllegalStateException("failed")
        val result = async(start = CoroutineStart.UNDISPATCHED) {
            runCatching {
                try {
                    suspendCancellableCoroutine<Int> { continuation ->
                        callback = TickUntilCallback(continuation) { throw failure }
                    }
                } finally {
                    cleanedUp = true
                }
            }
        }

        assertTrue(callback.asBoolean)

        assertFailure(failure, result.await().exceptionOrNull())
        assertTrue(cleanedUp)
    }
}

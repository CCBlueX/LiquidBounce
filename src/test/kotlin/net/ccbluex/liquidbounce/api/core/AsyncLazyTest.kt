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
package net.ccbluex.liquidbounce.api.core

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertSame

@OptIn(ExperimentalCoroutinesApi::class)
class AsyncLazyTest {

    @Test
    fun `caches a successful value`() = runTest {
        val value = Any()
        var attempts = 0
        val lazy = AsyncLazy {
            attempts++
            value
        }

        assertSame(value, lazy.get())
        assertSame(value, lazy.get())
        assertEquals(1, attempts)
    }

    @Test
    fun `caches a successful null value`() = runTest {
        var attempts = 0
        val lazy = AsyncLazy<String?> {
            attempts++
            null
        }

        assertNull(lazy.get())
        assertNull(lazy.get())
        assertEquals(1, attempts)
    }

    @Test
    fun `retries after failure and caches the recovered value`() = runTest {
        val failure = IllegalStateException("Temporary failure")
        var attempts = 0
        val lazy = AsyncLazy {
            if (++attempts == 1) throw failure
            "Recovered"
        }

        assertFailure(failure, assertFailsWith<IllegalStateException> { lazy.get() })
        assertEquals("Recovered", lazy.get())
        assertEquals("Recovered", lazy.get())
        assertEquals(2, attempts)
    }

    @Test
    fun `retries each failed attempt`() = runTest {
        var attempts = 0
        val lazy = AsyncLazy {
            if (++attempts < 3) throw IllegalArgumentException("Attempt $attempts")
            "Recovered"
        }

        assertEquals("Attempt 1", assertFailsWith<IllegalArgumentException> { lazy.get() }.message)
        assertEquals("Attempt 2", assertFailsWith<IllegalArgumentException> { lazy.get() }.message)
        assertEquals("Recovered", lazy.get())
        assertEquals(3, attempts)
    }

    @Test
    fun `concurrent callers share one successful attempt`() = runTest {
        val release = CompletableDeferred<Unit>()
        val value = Any()
        var attempts = 0
        val lazy = AsyncLazy {
            attempts++
            release.await()
            value
        }
        val callers = List(8) { async { lazy.get() } }

        runCurrent()
        assertEquals(1, attempts)
        release.complete(Unit)

        callers.forEach { assertSame(value, it.await()) }
        assertSame(value, lazy.get())
        assertEquals(1, attempts)
    }

    @Test
    fun `callers on different threads initialize only once`() = runTest {
        val start = CompletableDeferred<Unit>()
        val initialized = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val attempts = AtomicInteger()
        val value = Any()
        val lazy = AsyncLazy {
            attempts.incrementAndGet()
            initialized.complete(Unit)
            release.await()
            value
        }
        val callers = List(16) {
            async(Dispatchers.Default) {
                start.await()
                lazy.get()
            }
        }

        start.complete(Unit)
        initialized.await()
        release.complete(Unit)

        callers.forEach { assertSame(value, it.await()) }
        assertEquals(1, attempts.get())
    }

    @Test
    fun `concurrent callers share failure before a fresh attempt`() = runTest {
        val release = CompletableDeferred<Unit>()
        val failure = IllegalArgumentException("Temporary failure")
        var attempts = 0
        val lazy = AsyncLazy {
            if (++attempts == 1) {
                release.await()
                throw failure
            }
            "Recovered"
        }
        val callers = List(8) { async { runCatching { lazy.get() } } }

        runCurrent()
        assertEquals(1, attempts)
        release.complete(Unit)

        callers.forEach { assertFailure(failure, it.await().exceptionOrNull()) }
        assertEquals("Recovered", lazy.get())
        assertEquals(2, attempts)
    }

    @Test
    fun `a resumed waiter can immediately retry a failed attempt`() = runTest {
        val release = CompletableDeferred<Unit>()
        val failure = IllegalStateException("Temporary failure")
        var attempts = 0
        val lazy = AsyncLazy {
            if (++attempts == 1) {
                release.await()
                throw failure
            }
            "Recovered"
        }
        val owner = async { runCatching { lazy.get() } }
        runCurrent()
        val waiter = async(Dispatchers.Unconfined) {
            assertFailure(failure, assertFailsWith<IllegalStateException> { lazy.get() })
            lazy.get()
        }

        release.complete(Unit)

        assertFailure(failure, owner.await().exceptionOrNull())
        assertEquals("Recovered", waiter.await())
        assertEquals(2, attempts)
    }

    @Test
    fun `cancelled initializer releases waiters and allows retry`() = runTest {
        val release = CompletableDeferred<Unit>()
        var attempts = 0
        val lazy = AsyncLazy {
            if (++attempts == 1) release.await()
            "Recovered"
        }
        val owner = launch { lazy.get() }
        val waiter = async { runCatching { lazy.get() } }

        runCurrent()
        assertEquals(1, attempts)
        owner.cancelAndJoin()

        assertIs<CancellationException>(waiter.await().exceptionOrNull())
        assertEquals("Recovered", lazy.get())
        assertEquals(2, attempts)
    }

    @Test
    fun `cancelling a waiter does not cancel initialization`() = runTest {
        val release = CompletableDeferred<Unit>()
        var attempts = 0
        val lazy = AsyncLazy {
            attempts++
            release.await()
            "Value"
        }
        val owner = async { lazy.get() }
        val waiter = launch { lazy.get() }

        runCurrent()
        waiter.cancelAndJoin()
        release.complete(Unit)

        assertEquals("Value", owner.await())
        assertEquals("Value", lazy.get())
        assertEquals(1, attempts)
    }

    @Test
    fun `delegated property retries after failure`() {
        var attempts = 0
        val value by AsyncLazy {
            if (++attempts == 1) throw IllegalStateException("Temporary failure")
            "Recovered"
        }

        assertFailsWith<IllegalStateException> { value }
        assertEquals("Recovered", value)
        assertEquals("Recovered", value)
        assertEquals(2, attempts)
    }

    private fun assertFailure(expected: Throwable, actual: Throwable?) {
        val failure = assertNotNull(actual)
        assertEquals(expected::class, failure::class)
        assertEquals(expected.message, failure.message)
    }

}

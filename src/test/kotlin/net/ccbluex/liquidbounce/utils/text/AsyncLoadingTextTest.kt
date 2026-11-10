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

package net.ccbluex.liquidbounce.utils.text

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlin.coroutines.cancellation.CancellationException

class AsyncLoadingTextTest {

    @Test
    fun `lazy deferred displays loading until explicitly started`() = runTest {
        var calls = 0
        val deferred = async(start = CoroutineStart.LAZY) {
            calls++
            PlainText.NEW_LINE
        }
        val loading = PlainText.of("Waiting")
        val text = AsyncLoadingText(deferred, { loading }, AsyncLoadingText.DEFAULT_ON_EXCEPTION)

        assertSame(loading, text.get())
        assertEquals(0, calls)
        assertFalse(deferred.isActive)
        assertFalse(deferred.isCompleted)

        deferred.start()
        deferred.join()
        assertSame(PlainText.NEW_LINE, text.get())
        assertEquals(1, calls)
    }

    @Test
    fun `cancelling deferred displays loading while cleanup is still running`() = runTest {
        val cleanup = CompletableDeferred<Unit>()
        val deferred = async<Component>(start = CoroutineStart.UNDISPATCHED) {
            try {
                awaitCancellation()
            } finally {
                withContext(NonCancellable) { cleanup.await() }
            }
        }
        val loading = PlainText.of("Loading")
        val failed = PlainText.of("Cancelled")
        var failures = 0
        val text = AsyncLoadingText(deferred, { loading }, {
            failures++
            failed
        })

        deferred.cancel()
        try {
            assertFalse(deferred.isActive)
            assertFalse(deferred.isCompleted)
            assertSame(loading, text.get())
            assertEquals(0, failures)
        } finally {
            cleanup.complete(Unit)
        }
        deferred.join()

        assertSame(failed, text.get())
        assertEquals(1, failures)
    }

    @Test
    fun `completed cancellation is passed to the error renderer`() {
        val deferred = CompletableDeferred<Component>()
        val cancellation = CancellationException("cancelled")
        val failed = PlainText.of("Failed")
        var observed: Throwable? = null
        val text = AsyncLoadingText(deferred, AsyncLoadingText.DEFAULT_ON_LOADING, {
            observed = it
            failed
        })

        deferred.cancel(cancellation)

        assertSame(failed, text.get())
        assertSame(cancellation, observed)
        assertTrue(deferred.isCompleted)
    }

    @Test
    fun `loading renderer is evaluated for every unfinished read`() {
        val deferred = CompletableDeferred<Component>()
        var reads = 0
        val text = AsyncLoadingText(deferred, {
            PlainText.of("Loading ${++reads}")
        }, AsyncLoadingText.DEFAULT_ON_EXCEPTION)

        assertEquals("Loading 1", text.get().string)
        assertEquals("Loading 2", text.get().string)
        deferred.complete(PlainText.NEW_LINE)
        assertSame(PlainText.NEW_LINE, text.get())
        assertEquals(2, reads)
    }

    @Test
    fun `test normal completion`() {
        val deferred = CompletableDeferred<Component>()
        val onLoading = PlainText.of("Loading now...", Style.EMPTY)
        val text = AsyncLoadingText(deferred, { onLoading }, AsyncLoadingText.DEFAULT_ON_EXCEPTION)
        assertEquals(onLoading, text.get())

        deferred.complete(PlainText.NEW_LINE)
        assertEquals(PlainText.NEW_LINE, text.get())
    }

    @Test
    fun `test exceptional completion`() {
        val deferred = CompletableDeferred<Component>()
        val text = AsyncLoadingText(deferred)
        assertEquals(AsyncLoadingText.DEFAULT_ON_LOADING.get(), text.get())

        val exception = Exception()
        deferred.completeExceptionally(exception)
        assertEquals(AsyncLoadingText.DEFAULT_ON_EXCEPTION.apply(exception), text.get())
    }

}

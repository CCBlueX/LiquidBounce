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

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KProperty

/**
 * Shares an initialization attempt between callers and caches a successful result.
 * Failed or cancelled attempts are reported to their callers; a later call can retry.
 */
class AsyncLazy<T>(
    private val initializer: suspend () -> T
) {
    private val deferred = atomic<CompletableDeferred<T>?>(null)

    suspend fun get(): T {
        while (true) {
            val current = deferred.value
            if (current != null) {
                return current.await()
            }

            val attempt = CompletableDeferred<T>()
            if (!deferred.compareAndSet(expect = null, update = attempt)) {
                continue
            }

            try {
                attempt.complete(initializer())
            } catch (e: Throwable) {
                // Detach before waking waiters, so a caller handling this failure can retry.
                deferred.compareAndSet(expect = attempt, update = null)
                attempt.completeExceptionally(e)
            }

            return attempt.await()
        }
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        // Block on current thread if not called within a coroutine
        return runBlocking { get() }
    }
}

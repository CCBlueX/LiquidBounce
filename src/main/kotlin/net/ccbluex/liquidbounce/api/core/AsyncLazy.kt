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

class AsyncLazy<T>(
    private val initializer: suspend () -> T
) {
    private val deferred: CompletableDeferred<T> = CompletableDeferred()
    private val initialized = atomic(false)

    private suspend fun initialize() {
        if (initialized.compareAndSet(expect = false, update = true)) {
            try {
                val result = initializer()
                deferred.complete(result)
            } catch (e: Throwable) {
                deferred.completeExceptionally(e)
                // Reset initialized flag if initialization fails
                initialized.value = false
            }
        }
    }

    suspend fun get(): T {
        initialize()
        return deferred.await()
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        // Block on current thread if not called within a coroutine
        return runBlocking { get() }
    }
}

/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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
package net.ccbluex.liquidbounce.features.module

import com.google.common.collect.Lists
import kotlinx.coroutines.*
import net.ccbluex.liquidbounce.event.Event
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

// Running sequences
internal val sequences = Lists.newCopyOnWriteArrayList<Sequence<*>>()

typealias SuspendableHandler<T> = suspend Sequence<T>.(T) -> Unit

class Sequence<T : Event>(val handler: SuspendableHandler<T>, val event: T, private var loop: Boolean = false) {

    private var coroutine = GlobalScope.launch(Dispatchers.Unconfined) {
        sequences += this@Sequence
        if (!loop) {
            handler(event)
        } else {
            sync()
            while (loop) {
                handler(event)
                sync()
            }
        }
        sequences -= this@Sequence
    }

    private var continuation: Continuation<Unit>? = null
    private var elapsedTicks = 0
    private var totalTicks: () -> Int = { 0 }

    fun tick() {
        if (this.elapsedTicks < this.totalTicks()) {
            this.elapsedTicks++
        } else {
            this.continuation?.resume(Unit)
        }
    }

    suspend fun wait(ticks: Int) {
        this.wait { ticks }
    }

    /**
     * Waits for the amount of ticks that is retrieved via [ticksToWait]
     */
    suspend fun wait(ticksToWait: () -> Int) {
        elapsedTicks = 0
        totalTicks = ticksToWait

        suspendCoroutine<Unit> { continuation = it }
    }

    private suspend fun sync() = wait(0)

    suspend fun waitUntil(case: () -> Boolean) {
        while (!case()) {
            sync()
        }
    }

    fun cancel() {
        loop = false
    }

}

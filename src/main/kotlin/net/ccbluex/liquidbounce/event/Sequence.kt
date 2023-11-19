/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

import com.google.common.collect.Lists
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.ccbluex.liquidbounce.event.events.PlayerMovementTickEvent
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

typealias SuspendableHandler<T> = suspend Sequence<T>.(T) -> Unit

object SequenceManager : Listenable {

    // Running sequences
    internal val sequences = Lists.newCopyOnWriteArrayList<Sequence<*>>()

    /**
     * Tick sequences
     */
    val entityTickHandler = handler<PlayerMovementTickEvent> {
        for (sequence in sequences) {
            sequence.tick()
        }
    }

}

open class Sequence<T : Event>(val handler: SuspendableHandler<T>, protected val event: T) {

    private var coroutine = GlobalScope.launch(Dispatchers.Unconfined) {
        SequenceManager.sequences += this@Sequence
        coroutineRun()
        SequenceManager.sequences -= this@Sequence
    }

    private var continuation: Continuation<Unit>? = null
    private var elapsedTicks = 0
    private var totalTicks: () -> Int = { 0 }

    internal open suspend fun coroutineRun() {
        runCatching {
            handler(event)
        }.onFailure {
            logger.error("Exception occurred during subroutine", it)
        }
    }

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

    suspend fun waitSeconds(ticks: Int) {
        this.wait { ticks * 20 }
    }

    /**
     * TODO: Remove this once waiting ticks on a non repeatable sequence is fixed
     */
    suspend fun waitTicks(ticks: Int) {
        var elapsedTicks = 0

        while (elapsedTicks != ticks) {
            elapsedTicks++
            sync()
        }
    }

    /**
     * Waits for the amount of ticks that is retrieved via [ticksToWait]
     */
    suspend fun wait(ticksToWait: () -> Int) {
        elapsedTicks = 0
        totalTicks = ticksToWait

        suspendCoroutine { continuation = it }
    }

    internal suspend fun sync() = wait(0)

    suspend fun waitUntil(case: () -> Boolean) {
        while (!case()) {
            sync()
        }
    }

    /**
     * Waits for the requested [ticks]
     *
     * @param breakLoop In case it must exit early
     */

    // Somehow implement this only with the help of this class
    suspend fun wait(ticks: Int, breakLoop: () -> Boolean = { false }) {
        val player = mc.player ?: return
        val ticksToWait = player.age + ticks

        while (player.age < ticksToWait) {
            if (breakLoop()) {
                break
            }

            sync()
        }
    }

}

class DummyEvent : Event()

class RepeatingSequence(handler: SuspendableHandler<DummyEvent>) : Sequence<DummyEvent>(handler, DummyEvent()) {

    private var repeat = true

    override suspend fun coroutineRun() {
        sync()

        while (repeat) {
            runCatching {
                handler(event)
            }.onFailure {
                logger.error("Exception occurred during subroutine", it)
            }

            sync()
        }

    }

    fun cancel() {
        repeat = false
    }

}

/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.utils.client.logger
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

typealias SuspendableHandler<T> = suspend Sequence<T>.(T) -> Unit

object SequenceManager : Listenable {

    // Running sequences
    internal val sequences = Lists.newCopyOnWriteArrayList<Sequence<*>>()

    /**
     * Tick sequences
     *
     * We want it to run before everything else, so we set the priority to 1000
     * This is because we want to tick the existing sequences before new ones are added and might be ticked
     * in the same tick
     */
    @Suppress("unused")
    val tickSequences = handler<GameTickEvent>(priority = 1000) {
        for (sequence in sequences) {
            // Prevent modules handling events when not supposed to
            if (!sequence.owner.handleEvents()) {
                sequence.cancel()
                continue
            }

            sequence.tick()
        }
    }

}

open class Sequence<T : Event>(val owner: Listenable, val handler: SuspendableHandler<T>, protected val event: T) {

    private var coroutine: Job

    open fun cancel() {
        coroutine.cancel()
        SequenceManager.sequences -= this@Sequence
    }

    private var continuation: Continuation<Unit>? = null
    private var elapsedTicks = 0
    private var totalTicks: () -> Int = { 0 }

    init {
        // Note: It is important that this is in the constructor and NOT in the variable declaration, because
        // otherwise there is an edge case where the first time a time-dependent suspension occurs it will be
        // overwritten by the initialization of the `totalTicks` field which results in one or less ticks of actual wait
        // time.
        this.coroutine = GlobalScope.launch(Dispatchers.Unconfined) {
            SequenceManager.sequences += this@Sequence
            coroutineRun()
            SequenceManager.sequences -= this@Sequence
        }
    }

    internal open suspend fun coroutineRun() {
        if (owner.handleEvents()) {
            runCatching {
                handler(event)
            }.onFailure {
                logger.error("Exception occurred during subroutine", it)
            }
        }
    }

    internal fun tick() {
        if (++this.elapsedTicks >= this.totalTicks()) {
            this.continuation?.resume(Unit)
        }
    }

    /**
     * Waits until the [case] is true, then continues. Checks every tick.
     */
    suspend fun waitUntil(case: () -> Boolean) {
        while (!case()) {
            sync()
        }
    }

    /**
     * Waits until the fixed amount of ticks ran out or the [breakLoop] says to continue.
     */
    suspend fun waitConditional(ticks: Int, breakLoop: () -> Boolean = { false }): Boolean {
        // Don't wait if ticks is 0
        if (ticks == 0) {
            return true
        }

        wait { if (breakLoop()) 0 else ticks }

        return elapsedTicks >= ticks
    }

    /**
     * Waits a fixed amount of ticks before continuing.
     * Re-entry at the game tick.
     */
    suspend fun waitTicks(ticks: Int) {
        // Don't wait if ticks is 0
        if (ticks == 0) {
            return
        }

        this.wait { ticks }
    }

    /**
     * Waits a fixed amount of seconds on tick level before continuing.
     * Re-entry at the game tick.
     */
    suspend fun waitSeconds(seconds: Int) {
        if (seconds == 0) {
            return
        }

        this.wait { seconds * 20 }
    }

    /**
     * Waits for the amount of ticks that is retrieved via [ticksToWait]
     */
    private suspend fun wait(ticksToWait: () -> Int) {
        elapsedTicks = 0
        totalTicks = ticksToWait

        suspendCoroutine { continuation = it }
    }

    /**
     * Syncs the coroutine to the game tick.
     * It does not matter if we wait 0 or 1 ticks, it will always sync to the next tick.
     */
    internal suspend fun sync() = wait { 0 }

}

class DummyEvent : Event()

class RepeatingSequence(owner: Listenable, handler: SuspendableHandler<DummyEvent>)
    : Sequence<DummyEvent>(owner, handler, DummyEvent()) {

    private var continueLoop = true

    override suspend fun coroutineRun() {
        sync()

        while (continueLoop && owner.handleEvents()) {
            super.coroutineRun()
            sync()
        }
    }

    override fun cancel() {
        continueLoop = false
    }

}

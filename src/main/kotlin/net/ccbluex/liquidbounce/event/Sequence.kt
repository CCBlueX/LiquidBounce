/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

import kotlinx.coroutines.*
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.BooleanSupplier
import java.util.function.IntSupplier
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

typealias SuspendableEventHandler<T> = suspend Sequence.(T) -> Unit
typealias SuspendableHandler = suspend Sequence.() -> Unit

object SequenceManager : EventListener {

    // Running sequences
    internal val sequences = CopyOnWriteArrayList<Sequence>()

    /**
     * Tick sequences
     *
     * We want it to run before everything else, so we set the priority to 1000
     * This is because we want to tick the existing sequences before new ones are added and might be ticked
     * in the same tick
     */
    @Suppress("unused")
    val tickSequences = handler<GameTickEvent>(priority = FIRST_PRIORITY) {
        for (sequence in sequences) {
            // Prevent modules handling events when not supposed to
            if (!sequence.owner.running) {
                sequence.cancel()
                continue
            }

            sequence.tick()
        }
    }

    /**
     * Cancels all sequences associated with an event listener.
     * This is called when a module is disabled to ensure no sequences continue running.
     */
    fun cancelAllSequences(owner: EventListener) {
        sequences.removeIf { sequence ->
            if (sequence.owner == owner) {
                sequence.cancel()
                true
            } else {
                false
            }
        }
    }

}

@Suppress("TooManyFunctions")
open class Sequence(val owner: EventListener, val handler: SuspendableHandler) {
    var coroutine: Job
        private set

    open fun cancel() {
        coroutine.cancel()
        cancellationTask?.run()
        SequenceManager.sequences -= this@Sequence
    }

    private var continuation: Continuation<Unit>? = null
    private var elapsedTicks = 0
    private var totalTicks = IntSupplier { 0 }
    private var cancellationTask: Runnable? = null

    init {
        // Note: It is important that this is in the constructor and NOT in the variable declaration, because
        // otherwise there is an edge case where the first time a time-dependent suspension occurs it will be
        // overwritten by the initialization of the `totalTicks` field
        // which results in one or fewer ticks of actual wait time.
        @OptIn(DelicateCoroutinesApi::class)
        this.coroutine = GlobalScope.launch(Dispatchers.Unconfined) {
            SequenceManager.sequences += this@Sequence
            coroutineRun()
            SequenceManager.sequences -= this@Sequence
        }
    }

    internal open suspend fun coroutineRun() {
        if (owner.running) {
            runCatching {
                handler()
            }.onFailure {
                logger.error("Exception occurred during subroutine", it)
            }
        }
    }

    internal fun tick() {
        if (++this.elapsedTicks >= this.totalTicks.asInt) {
            val continuation = this.continuation ?: return
            this.continuation = null
            continuation.resume(Unit)
        }
    }

    /**
     * Adds a task to be executed when the sequence is cancelled.
     */
    fun onCancellation(task: Runnable) {
        cancellationTask = task
    }

    /**
     * Waits until the [case] is true, then continues. Checks every tick.
     */
    suspend fun waitUntil(case: BooleanSupplier): Int {
        var ticks = 0
        while (!case.asBoolean) {
            sync()
            ticks++
        }
        return ticks
    }

    /**
     * Waits until the fixed amount of ticks ran out or the [breakLoop] says to continue.
     * Returns true when we passed the time of [ticks] without breaking the loop.
     */
    suspend fun waitConditional(ticks: Int, breakLoop: BooleanSupplier = BooleanSupplier { false }): Boolean {
        // Don't wait if ticks is 0
        if (ticks == 0) {
            return !breakLoop.asBoolean
        }

        wait { if (breakLoop.asBoolean) 0 else ticks }

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
    private suspend fun wait(ticksToWait: IntSupplier) {
        elapsedTicks = 0
        totalTicks = ticksToWait

        suspendCoroutine { continuation = it }
    }

    /**
     * Syncs the coroutine to the game tick.
     * It does not matter if we wait 0 or 1 ticks, it will always sync to the next tick.
     */
    internal suspend fun sync() = wait { 0 }

    /**
     * Private utility function for waiting external [deferred].
     * Resume without changing [CoroutineContext].
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun <T> waitFor(deferred: Deferred<T>): T {
        // Use `waitUntil` to avoid duplicated resumption
        waitUntil(deferred::isCompleted)
        return deferred.getCompleted()
    }

    /**
     * Start a task with given context, and wait for its completion.
     * @see withContext
     */
    suspend fun <T> waitFor(
        context: CoroutineContext,
        block: suspend CoroutineScope.() -> T
    ): T = waitFor(CoroutineScope(coroutine + context).async(context, block = block))

    /**
     * Wait for next event with type of [E],
     * handle it with [handler] and [priority],
     * and get the instance.
     */
    suspend inline fun <reified E : Event> waitNext(
        priority: Short = 0,
        crossinline handler: Handler<E>
    ): E {
        val deferred = CompletableDeferred<E>(parent = coroutine)
        owner.once<E>(priority) {
            handler(it)
            deferred.complete(it)
        }
        return waitFor(deferred)
    }

}

class TickSequence(owner: EventListener, handler: SuspendableHandler) : Sequence(owner, handler) {
    private var continueLoop = true

    override suspend fun coroutineRun() {
        sync()

        while (continueLoop && owner.running) {
            super.coroutineRun()
            sync()
        }
    }

    override fun cancel() {
        continueLoop = false
    }

}

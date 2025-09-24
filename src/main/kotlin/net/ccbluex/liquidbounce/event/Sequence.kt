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

import it.unimi.dsi.fastutil.objects.ReferenceArrayList
import kotlinx.coroutines.*
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
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
    private val runningList = ReferenceArrayList<Sequence>()

    // Next tick sequences
    private val pendingList = ReferenceArrayList<Sequence>()

    /**
     * Registers a sequence to be ticked.
     *
     * @param sequence The [Sequence] to be ticked from next tick.
     */
    fun register(sequence: Sequence) {
        mc.execute { pendingList.add(sequence) }
    }

    /**
     * Tick sequences
     *
     * We want it to run before everything else, so we set the priority to [FIRST_PRIORITY]
     * This is because we want to tick the existing sequences before new ones are added and might be ticked
     * in the same tick
     */
    @Suppress("unused")
    private val tickSequences = handler<GameTickEvent>(priority = FIRST_PRIORITY) {
        runningList.addAll(pendingList)
        pendingList.clear()
        runningList.removeIf {
            if (it.isJobInActive) {
                true
            } else {
                it.tick()
                false
            }
        }
    }

}

@Suppress("TooManyFunctions")
class Sequence(val owner: EventListener, val handler: SuspendableHandler) {

    private var continuation: Continuation<Unit>? = null
    private var elapsedTicks = 0
    private var totalTicks = IntSupplier { 0 }

    /**
     * Use [owner]'s [kotlin.coroutines.ContinuationInterceptor] and [CoroutineScope] to handle:
     * - Exception
     * - [EventListener.running] aware cancellation
     */
    private val coroutine: Job = owner.eventListenerScope.launch(
        context = owner.continuationInterceptor() + CoroutineName("Sequence-${owner}"),
        start = CoroutineStart.UNDISPATCHED
    ) {
        SequenceManager.register(this@Sequence)
        if (owner.running) {
            handler()
        }
    }

    val isJobInActive: Boolean
        get() = !coroutine.isActive

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
        coroutine.invokeOnCompletion {
            if (it is CancellationException) {
                task.run()
            }
        }
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
    private suspend fun sync() = wait { 0 }

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

}

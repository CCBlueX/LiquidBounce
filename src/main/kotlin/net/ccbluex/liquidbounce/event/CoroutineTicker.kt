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
import java.util.function.IntPredicate
import java.util.function.Predicate
import kotlin.coroutines.resume

typealias SuspendableEventHandler<T> = suspend CoroutineScope.(T) -> Unit

object CoroutineTicker : EventListener {

    // Running callbacks
    private val runningList = ReferenceArrayList<BooleanSupplier>()

    // Next tick callbacks
    private val pendingList = ReferenceArrayList<BooleanSupplier>()

    /**
     * Registers a task to be ticked.
     *
     * @param task The callback to be run from next tick. It will be removed once returns true.
     */
    fun register(task: BooleanSupplier) {
        mc.execute { pendingList.add(task) }
    }

    /**
     * We want it to run before everything else, so we set the priority to [FIRST_PRIORITY]
     * This is because we want to tick the existing tasks before new ones are added and might be ticked
     * in the same tick
     */
    @Suppress("unused")
    private val taskTicker = handler<GameTickEvent>(priority = FIRST_PRIORITY) {
        runningList.addAll(pendingList)
        pendingList.clear()
        runningList.removeIf(Predicate(BooleanSupplier::getAsBoolean))
    }

}

/**
 * Ticks with [stopAt] until it returns true.
 * The elapsed ticks (starting from 1) will be passed to [stopAt].
 *
 * Resumes on Render thread.
 *
 * Example:
 * - `tickUntil { true }` --> `1`
 * - `tickUntil { it >= 2 }` --> `2`
 *
 * @param stopAt the callback of elapsed ticks. Will be called on game tick.
 * @return the times of [stopAt] to be executed (equals to elapsed ticks)
 */
suspend fun tickUntil(
    stopAt: IntPredicate,
): Int = suspendCancellableCoroutine { continuation ->
    var elapsedTicks = 0
    CoroutineTicker.register {
        when {
            !continuation.isActive -> true
            stopAt.test(++elapsedTicks) -> {
                continuation.resume(elapsedTicks)
                true
            }

            else -> false
        }
    }
}

/**
 * Ticks until the fixed amount of ticks ran out or the [breakLoop] says to continue.
 *
 * @returns if we passed the time of [ticks] without breaking the loop.
 */
suspend fun tickConditional(ticks: Int, breakLoop: BooleanSupplier): Boolean {
    // Don't wait if ticks is 0
    if (ticks == 0) {
        return !breakLoop.asBoolean
    }

    return tickUntil { breakLoop.asBoolean || it >= ticks } >= ticks
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

    tickUntil { it >= ticks }
}

/**
 * Waits a fixed amount of seconds on tick level before continuing.
 * Re-entry at the game tick.
 *
 * Note: When TPS is not 20, this won't be actual `seconds`.
 */
suspend fun waitSeconds(seconds: Int) = waitTicks(seconds * 20)

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
package net.ccbluex.liquidbounce.event

import it.unimi.dsi.fastutil.objects.ReferenceArrayList
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.ReportedException
import org.slf4j.LoggerFactory
import java.util.function.BooleanSupplier
import java.util.function.IntPredicate
import java.util.function.Predicate
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration

/**
 * A queue of callbacks advanced once per tick.
 *
 * Suspension helpers use the ticker in their coroutine context, or [CoroutineTicker] when there is none.
 */
interface Ticker : CoroutineContext.Element {

    /**
     * Registers a task to be ticked.
     *
     * @param task The callback to be run from next tick. It will be removed once returns true.
     */
    fun register(task: BooleanSupplier)

    fun tick()

    override val key: CoroutineContext.Key<*>
        get() = Key

    companion object Key : CoroutineContext.Key<Ticker>
}

internal val CoroutineContext.ticker: Ticker
    get() = this[Ticker] ?: CoroutineTicker

object CoroutineTicker : Ticker {

    private val logger = LoggerFactory.getLogger("$CLIENT_NAME/CoroutineTicker")

    // Tracks nested Minecraft.tick() calls. Only the outermost tick may advance coroutine waiters.
    private var minecraftTickDepth = 0

    // Running callbacks
    private val runningList = ReferenceArrayList<BooleanSupplier>()

    // Next tick callbacks
    private val pendingList = ReferenceArrayList<BooleanSupplier>()

    fun beginMinecraftTick() {
        minecraftTickDepth++
    }

    fun endMinecraftTick() {
        if (minecraftTickDepth <= 0) {
            logger.warn("CoroutineTicker minecraftTickDepth underflow")
            minecraftTickDepth = 0
            return
        }

        minecraftTickDepth--
    }

    /**
     * Registers a task to be ticked.
     *
     * @param task The callback to be run from next tick. It will be removed once returns true.
     */
    override fun register(task: BooleanSupplier) {
        mc.execute { pendingList.add(task) }
    }

    /**
     * We want it to run before everything else, this is because we want to tick the existing tasks before
     * new ones are added and might be ticked in the same tick
     */
    override fun tick() {
        if (minecraftTickDepth > 1) {
            return
        }

        runningList.addAll(pendingList)
        pendingList.clear()
        runningList.removeIf(Predicate {
            try {
                it.asBoolean
            } catch (e: ReportedException) {
                throw e
            } catch (e: Throwable) {
                logger.error("Unhandled exception thrown by callback", e)
                false
            }
        })
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
 * Exceptions thrown by [stopAt] are delivered to the waiting coroutine.
 *
 * What the run [requires] is checked each tick before [stopAt] and once more before returning; when it fails,
 * [stopAt] is not called again and the run ends here.
 *
 * @param stopAt the callback of elapsed ticks. Will be called on game tick.
 * @return the times of [stopAt] to be executed (equals to elapsed ticks)
 */
suspend fun tickUntil(
    stopAt: IntPredicate,
): Int {
    val context = currentCoroutineContext()
    val guards = context.activeGuards()
    val elapsedTicks = suspendCancellableCoroutine { continuation ->
        context.ticker.register(TickUntilCallback(continuation, guards, stopAt))
    }
    guards?.ensure()
    return elapsedTicks
}

private class TickUntilCallback(
    private val continuation: CancellableContinuation<Int>,
    private val guards: Guards?,
    private val stopAt: IntPredicate,
) : BooleanSupplier {
    private var elapsedTicks = 0

    override fun getAsBoolean(): Boolean {
        if (!continuation.isActive) {
            return true
        }

        val finished = try {
            val broken = guards?.broken()
            if (broken != null) {
                continuation.resumeWithException(broken)
                return true
            }
            stopAt.test(++elapsedTicks)
        } catch (e: Throwable) {
            continuation.resumeWithException(e)
            return true
        }

        if (finished) {
            continuation.resume(elapsedTicks)
        }
        return finished
    }

    override fun toString(): String =
        "TickUntilCallback(elapsedTicks=$elapsedTicks, continuation=$continuation, stopAt=$stopAt)"
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

/**
 * Waits [duration] of wall-clock time, for pauses shorter than a tick.
 *
 * What the run [requires] is checked once the time is up, on the thread that continues. Unlike
 * [kotlinx.coroutines.delay] alone, nothing after this runs when it failed meanwhile.
 */
suspend fun waitFor(duration: Duration) {
    delay(duration)
    currentCoroutineContext().activeGuards()?.ensure()
}

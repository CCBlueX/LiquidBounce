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
package net.ccbluex.liquidbounce.utils.aiming

import it.unimi.dsi.fastutil.objects.ReferenceArrayList
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.suspendCancellableCoroutine
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.GuardScope
import net.ccbluex.liquidbounce.event.guarded
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import kotlin.coroutines.CoroutineContext

/**
 * Asks for the rotation [target] plans, on every tick until the rotation going out meets it, and returns what [hit]
 * makes of that rotation; [hit] returns null while it misses.
 *
 * Resumes within the rotation update of that tick, before modules tick and before the movement packet carries the
 * rotation to the server, so the code after this acts with it the way vanilla acts on a look. That holds for
 * coroutines that are not dispatched, such as the ones of [net.ccbluex.liquidbounce.event.tickHandler].
 *
 * Inside a [guarded] scope the conditions are checked every tick before [target] plans and once more before this
 * returns. When one fails, the module stops asking for the rotation and nothing after this runs.
 */
suspend fun <T : Any> EventListener.lookAt(
    priority: Priority,
    target: () -> RotationTarget,
    hit: RotationTarget.(going: Rotation) -> T?,
): T {
    val context = currentCoroutineContext()
    val guard = context[GuardScope]
    val result = suspendCancellableCoroutine { continuation ->
        val look = Look(continuation, guard, this, priority.priority, target, hit)
        val looks = context[Looks]
        when {
            looks != null -> looks.add(look)
            // Within the tick, so a look started before the rotation update asks on that update already
            mc.isSameThread -> RotationManager.looks.add(look)
            else -> mc.execute { RotationManager.looks.add(look) }
        }
    }
    guard?.ensure()
    return result
}

/**
 * [lookAt] until the rotation going out meets the target itself: its surface, or else its angles within the tolerance.
 */
suspend fun EventListener.lookAt(priority: Priority, target: () -> RotationTarget): Rotation =
    lookAt(priority, target) { going -> going.takeIf { isMetBy(it) } }

/**
 * The modules waiting in [lookAt]. The rotation update serves them each tick: [request] before it turns the view,
 * [offer] with the rotation it turned to, [settle] once it is done.
 *
 * [lookAt] uses the one in its coroutine context, or the one of [RotationManager] when there is none.
 */
class Looks : CoroutineContext.Element {

    override val key: CoroutineContext.Key<*>
        get() = Key

    companion object Key : CoroutineContext.Key<Looks>

    private val waiting = ReferenceArrayList<Look<*>>()

    val size: Int
        get() = waiting.size

    internal fun add(look: Look<*>) {
        waiting.add(look)
    }

    /**
     * Checks the guards and hands the target each look plans for this tick to [submit].
     */
    fun request(submit: (RotationTarget, priority: Int, provider: EventListener) -> Unit) {
        for (i in 0 until waiting.size) {
            waiting[i].request(submit)
        }
    }

    /**
     * Tells the looks whose request is among [requests] that [going] goes out this tick.
     */
    fun offer(going: Rotation, requests: List<RotationTarget>) {
        for (i in 0 until waiting.size) {
            waiting[i].offer(going, requests)
        }
    }

    /**
     * Resumes the looks that were met or failed. Their code runs right here and may start new looks.
     */
    fun settle() {
        var i = 0
        while (i < waiting.size) {
            if (waiting[i].settle()) {
                waiting.removeAt(i)
            } else {
                i++
            }
        }
    }

}

internal class Look<T : Any>(
    private val continuation: CancellableContinuation<T>,
    private val guard: GuardScope?,
    private val provider: EventListener,
    private val priority: Int,
    private val target: () -> RotationTarget,
    private val hit: RotationTarget.(Rotation) -> T?,
) {

    private var request: RotationTarget? = null
    private var outcome: Result<T>? = null

    fun request(submit: (RotationTarget, Int, EventListener) -> Unit) {
        if (outcome != null || !continuation.isActive) {
            return
        }

        outcome = runCatching {
            guard?.broken()?.let { throw it }
            val planned = target()
            request = planned
            submit(planned, priority, provider)
            return
        }
    }

    fun offer(going: Rotation, requests: List<RotationTarget>) {
        val request = request ?: return
        if (outcome != null || requests.none { it === request }) {
            return
        }

        outcome = runCatching { request.hit(going) ?: return }
    }

    /**
     * @return true once this look is done
     */
    fun settle(): Boolean {
        if (!continuation.isActive) {
            return true
        }

        val outcome = outcome ?: return false
        continuation.resumeWith(outcome)
        return true
    }

}

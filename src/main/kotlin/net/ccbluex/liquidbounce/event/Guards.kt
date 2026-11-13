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
@file:OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)

package net.ccbluex.liquidbounce.event

import it.unimi.dsi.fastutil.objects.ReferenceArrayList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CopyableThreadContextElement
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlin.coroutines.CoroutineContext
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Requires what [read] returns for the rest of this run of the handler: `false` or `null` ends it. Every suspension
 * helper ([tickUntil], [waitTicks], [tickConditional], [waitSeconds], [waitMatches]) reads it again on each tick it
 * waits and once more right before the code after it runs, [waitFor] only the latter. Once it fails, the run ends there
 * quietly: nothing after that suspension runs, cleanup in `finally` and the handler's `onCancellation` still do, and
 * the next run starts without it. If it fails right now, the run ends here.
 *
 * A condition and a value are written the same way; a delegated value is always the one read last:
 * ```kotlin
 * requires { !InventoryManager.isHandledScreenOpen }
 * val pearl by requires { Slots.OffhandWithHotbar.findSlot(Items.ENDER_PEARL) }
 * ```
 *
 * Close the result to stop requiring it earlier, e.g. with `use`. Works in every coroutine LiquidBounce starts for a
 * listener; a coroutine launched from one is a run of its own.
 */
suspend fun <T : Any> requires(read: () -> T?): Required<T> {
    val guards = currentGuards()
    return Required(read, guards).also { guards.add(it.check) }
}

/**
 * Requires that [read] returns the same instance it returns now, for example the world or the open menu.
 */
suspend fun unchanged(read: () -> Any?): AutoCloseable {
    val initial = read()
    return requires { read() === initial }
}

class Required<T : Any> internal constructor(
    read: () -> T?,
    private val guards: Guards,
) : ReadOnlyProperty<Any?, T>, AutoCloseable {

    private var current: T? = null

    internal val check = { read().also { current = it }.let { it != null && it != false } }

    val value: T
        get() = checkNotNull(current) { "Required value read before it was required" }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = value

    override fun close() = guards.remove(check)

}

/**
 * What one run of a handler [requires], carried in its coroutine context so that suspension helpers find it.
 *
 * Every coroutine started on a listener's scope gets one of its own, since each launch copies it empty; `withContext`
 * keeps it, as the code it runs is still part of the run.
 */
class Guards internal constructor() : CopyableThreadContextElement<Unit> {

    companion object Key : CoroutineContext.Key<Guards>

    override val key: CoroutineContext.Key<*>
        get() = Key

    private var conditions: ReferenceArrayList<() -> Boolean>? = null

    internal val isEmpty: Boolean
        get() = conditions.isNullOrEmpty()

    internal fun add(condition: () -> Boolean) {
        (conditions ?: ReferenceArrayList<() -> Boolean>(4).also { conditions = it }).add(condition)
        if (!condition()) {
            throw GuardInterruption(condition)
        }
    }

    internal fun remove(condition: () -> Boolean) {
        conditions?.remove(condition)
    }

    internal fun clear() {
        conditions?.clear()
    }

    /**
     * The interruption for the first condition that no longer holds.
     */
    internal fun broken(): GuardInterruption? {
        val conditions = conditions ?: return null
        for (i in 0 until conditions.size) {
            val condition = conditions[i]
            if (!condition()) {
                return GuardInterruption(condition)
            }
        }
        return null
    }

    internal fun ensure() {
        broken()?.let { throw it }
    }

    override fun copyForChild() = Guards()

    override fun mergeForChild(overwritingElement: CoroutineContext.Element): CoroutineContext = Guards()

    override fun updateThreadContext(context: CoroutineContext) = Unit

    override fun restoreThreadContext(context: CoroutineContext, oldState: Unit) = Unit

}

/**
 * The guards of the coroutine this suspends in, or null outside of a listener's coroutines. Empty ones are null too,
 * so waits that require nothing skip the checks.
 */
internal fun CoroutineContext.activeGuards(): Guards? = this[Guards]?.takeUnless { it.isEmpty }

private suspend fun currentGuards() = checkNotNull(currentCoroutineContext()[Guards]) {
    "requires only works in coroutines started for a listener"
}

/**
 * Runs [block] as a run of its own in a coroutine that handles one event after the other: it starts requiring
 * nothing, and a failing requirement ends only it.
 */
internal suspend inline fun asRun(block: () -> Unit) {
    val guards = currentCoroutineContext()[Guards]
    try {
        block()
    } catch (_: GuardInterruption) {
    } finally {
        guards?.clear()
    }
}

/**
 * Ends a run at the suspension where [condition] failed. Never reaches module code.
 */
internal class GuardInterruption(
    @JvmField val condition: () -> Boolean,
) : CancellationException("A requirement of the run no longer holds") {
    override fun fillInStackTrace(): Throwable = this
}

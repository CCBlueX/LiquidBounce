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
 * Something a handler's work depends on, such as "the target is alive".
 */
fun interface Condition {
    fun holds(): Boolean
}

/**
 * Keeps [condition] for the rest of this run of the handler. Every suspension helper ([tickUntil], [waitTicks],
 * [tickConditional], [waitSeconds], [waitMatches]) checks it on each tick it waits and once more right before the
 * code after it runs, [waitFor] only the latter. Once it fails, the run ends there quietly: nothing after that
 * suspension runs, cleanup in `finally` and the handler's `onCancellation` still do, and the next run starts
 * without it.
 *
 * If it does not hold right now, the run ends here. Close the result to stop guarding earlier, e.g. with `use`.
 *
 * Works in every coroutine LiquidBounce starts for a listener; a coroutine launched from one is a run of its own.
 */
suspend fun guard(condition: Condition): AutoCloseable {
    val guards = currentGuards()
    guards.add(condition)
    return AutoCloseable { guards.remove(condition) }
}

/**
 * A value read again on every check of this run, see [guard]. Reading `null` ends the run, so the value a delegated
 * property returns after a suspension is always the current one.
 *
 * ```kotlin
 * val target by tracked { targetTracker.target?.takeIf { it.isAlive } }
 * ```
 *
 * Close it, e.g. with `use`, to stop guarding it before the run ends.
 */
suspend fun <T : Any> tracked(read: () -> T?): Tracked<T> {
    val guards = currentGuards()
    return Tracked(read, guards).also(guards::add)
}

/**
 * Guards that [read] returns the same instance it returns now, for example the world or the open menu.
 */
suspend fun same(read: () -> Any?): AutoCloseable {
    val initial = read()
    return guard { read() === initial }
}

class Tracked<T : Any> internal constructor(
    private val read: () -> T?,
    private val guards: Guards,
) : Condition, ReadOnlyProperty<Any?, T>, AutoCloseable {

    private var current: T? = null

    val value: T
        get() = checkNotNull(current) { "Tracked value read before it was guarded" }

    override fun holds(): Boolean {
        current = read()
        return current != null
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = value

    override fun close() = guards.remove(this)

}

/**
 * The conditions of one run of a handler, carried in its coroutine context so that suspension helpers find them.
 *
 * Every coroutine started on a listener's scope gets one of its own, since each launch copies it empty; `withContext`
 * keeps it, as the code it runs is still part of the run.
 */
class Guards internal constructor() : CopyableThreadContextElement<Unit> {

    companion object Key : CoroutineContext.Key<Guards>

    override val key: CoroutineContext.Key<*>
        get() = Key

    private var conditions: ReferenceArrayList<Condition>? = null

    internal val isEmpty: Boolean
        get() = conditions.isNullOrEmpty()

    internal fun add(condition: Condition) {
        (conditions ?: ReferenceArrayList<Condition>(4).also { conditions = it }).add(condition)
        if (!condition.holds()) {
            throw GuardInterruption(condition)
        }
    }

    internal fun remove(condition: Condition) {
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
            if (!condition.holds()) {
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
 * so waits without guards skip the checks.
 */
internal fun CoroutineContext.activeGuards(): Guards? = this[Guards]?.takeUnless { it.isEmpty }

private suspend fun currentGuards() = checkNotNull(currentCoroutineContext()[Guards]) {
    "Guards only work in coroutines started for a listener"
}

/**
 * Runs [block] as a run of its own in a coroutine that handles one event after the other: it starts without guards,
 * and a failing guard ends only it.
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
    @JvmField val condition: Condition,
) : CancellationException("Guard condition $condition no longer holds") {
    override fun fillInStackTrace(): Throwable = this
}

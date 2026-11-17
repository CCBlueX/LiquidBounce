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

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CopyableThreadContextElement
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlin.coroutines.CoroutineContext
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Requires what [read] returns for the rest of this run of the handler: `false` or `null` ends it. It is read again on
 * every tick a tick-based helper waits ([tickUntil], [waitTicks], [tickConditional], [waitSeconds]) and once more right
 * before the code after any suspension runs. Once it fails, the run ends there quietly: nothing after that suspension
 * runs, cleanup in `finally` and the handler's `onCancellation` still do, and the next run starts without it. If it
 * fails right now, the run ends here.
 *
 * A condition and a value are written the same way; a delegated value is always the one read last:
 * ```kotlin
 * requires { !InventoryManager.isHandledScreenOpen }
 * val pearl by requires { Slots.OffhandWithHotbar.findSlot(Items.ENDER_PEARL) }
 * ```
 *
 * Works in every handler run LiquidBounce starts for a listener; a coroutine launched from one is not part of the run.
 */
suspend fun <T : Any> requires(read: () -> T?): Required<T> {
    val guards = checkNotNull(currentCoroutineContext()[Guards]) {
        "requires only works in coroutines started for a listener"
    }
    return Required(read).also { guards.add(it.check) }
}

/**
 * Requires that [read] returns the same instance it returns now, for example the open menu.
 */
suspend fun unchanged(read: () -> Any?) {
    val initial = read()
    requires { read() === initial }
}

class Required<T : Any> internal constructor(read: () -> T?) : ReadOnlyProperty<Any?, T> {

    private var current: T? = null

    internal val check = { read().also { current = it }.let { it != null && it != false } }

    val value: T
        get() = checkNotNull(current) { "Required value read before it was required" }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = value

}

/**
 * What one run of a handler [requires], carried in its coroutine context so that suspension helpers find it.
 *
 * Every coroutine started on a listener's scope gets one of its own, since each launch copies it empty; `withContext`
 * keeps it, as the code it runs is still part of the run.
 */
class Guards internal constructor() : CopyableThreadContextElement<Unit> {

    companion object Key : CoroutineContext.Key<Guards> {
        private val NONE: Array<() -> Boolean> = emptyArray()
    }

    override val key: CoroutineContext.Key<*>
        get() = Key

    /** Replaced instead of changed, so that the threads checking it read a whole one. */
    @Volatile
    private var conditions: Array<() -> Boolean> = NONE

    internal val isIdle: Boolean
        get() = conditions.isEmpty()

    internal fun add(condition: () -> Boolean) {
        conditions += condition
        if (!condition()) {
            throw GuardInterruption(condition)
        }
    }

    /**
     * Starts a run: it requires nothing yet, and is bound to the world it starts in.
     */
    internal fun start() {
        conditions = NONE
    }

    /**
     * The interruption for the first condition that no longer holds.
     */
    internal fun broken(): GuardInterruption? {
        for (condition in conditions) {
            if (!condition()) {
                return GuardInterruption(condition)
            }
        }
        return null
    }

    override fun copyForChild() = Guards()

    override fun mergeForChild(overwritingElement: CoroutineContext.Element): CoroutineContext = Guards()

    override fun updateThreadContext(context: CoroutineContext) = Unit

    override fun restoreThreadContext(context: CoroutineContext, oldState: Unit) = Unit

}

/**
 * The guards of the coroutine this suspends in, or null outside of a listener's coroutines. Idle ones are null too,
 * so waits that require nothing skip the checks.
 */
internal fun CoroutineContext.activeGuards(): Guards? = this[Guards]?.takeUnless { it.isIdle }

/**
 * Starts a handler run in the current coroutine, see [Guards.start].
 */
internal suspend fun beginRun() {
    currentCoroutineContext()[Guards]?.start()
}

/**
 * Runs [block] as a run of its own in a coroutine that handles one event after the other: a failing requirement ends
 * only it.
 */
internal suspend inline fun asRun(block: () -> Unit) {
    currentCoroutineContext()[Guards]?.start()
    try {
        block()
    } catch (_: GuardInterruption) {
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

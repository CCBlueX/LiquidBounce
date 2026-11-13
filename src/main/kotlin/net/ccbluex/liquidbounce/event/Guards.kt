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
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
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
 * A run started in a world also ends once that world is left, without asking for it. Works in every handler run
 * LiquidBounce starts for a listener; a coroutine launched from one is not part of the run.
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
        private const val UNBOUND = -1
        private val worldLeft = { false }
    }

    override val key: CoroutineContext.Key<*>
        get() = Key

    private var conditions: ReferenceArrayList<() -> Boolean>? = null

    /** The [Worlds.generation] the run started in, or [UNBOUND] outside of a world. */
    private var world = UNBOUND

    internal val isIdle: Boolean
        get() = world == UNBOUND && conditions.isNullOrEmpty()

    internal fun add(condition: () -> Boolean) {
        (conditions ?: ReferenceArrayList<() -> Boolean>(4).also { conditions = it }).add(condition)
        if (!condition()) {
            throw GuardInterruption(condition)
        }
    }

    /**
     * Starts a run: it requires nothing yet, and is bound to the world it starts in.
     */
    internal fun start() {
        conditions?.clear()
        world = if (Worlds.inWorld) Worlds.generation else UNBOUND
    }

    internal fun clear() {
        conditions?.clear()
        world = UNBOUND
    }

    /**
     * The interruption for the world being left or the first condition that no longer holds.
     */
    internal fun broken(): GuardInterruption? {
        if (world != UNBOUND && world != Worlds.generation) {
            return GuardInterruption(worldLeft)
        }

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
 * Counts world changes, so that a run knows when the world it started in is gone.
 */
internal object Worlds : EventListener {

    @Volatile
    internal var generation = 0
        private set

    @Volatile
    internal var inWorld = false
        private set

    internal fun changed(inWorld: Boolean) {
        generation++
        this.inWorld = inWorld
    }

    // Before every other handler, so that runs started for the change belong to the new world
    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent>(priority = Short.MAX_VALUE) { event ->
        changed(event.world != null)
    }

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
 * Runs [block] as a run of its own in a coroutine that handles one event after the other: a failing requirement or
 * the world being left ends only it.
 */
internal suspend inline fun asRun(block: () -> Unit) {
    val guards = currentCoroutineContext()[Guards]
    guards?.start()
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

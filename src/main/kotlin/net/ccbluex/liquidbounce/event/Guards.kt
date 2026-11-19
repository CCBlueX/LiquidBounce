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
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

private val logger = LoggerFactory.getLogger("$CLIENT_NAME/Guards")

/**
 * The name of a requirement while it no longer holds, `null` while it does.
 */
internal typealias Requirement = () -> String?

/**
 * Requires what [read] returns for the rest of this run of the handler: `false` or `null` ends it. It is read again on
 * every tick a tick-based helper waits ([tickUntil], [waitTicks], [tickConditional], [waitMillis]) and
 * once more right
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
 * A delegated value goes by its property name in the log of a run it ended; a condition without a value has none.
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

class Required<T : Any> internal constructor(
    private val read: () -> T?,
) : ReadOnlyProperty<Nothing?, T> {

    private var current: T? = null

    /** The property this was delegated to, for naming it in the log. */
    private var label = "requirement"

    internal val check: Requirement = {
        val value = read()
        current = value
        when (value) {
            null -> label
            is Boolean -> if (value) null else label
            else -> null
        }
    }

    val value: T
        get() = checkNotNull(current) { "Required value read before it was required" }

    override fun getValue(thisRef: Nothing?, property: KProperty<*>): T = value

    operator fun provideDelegate(thisRef: Nothing?, property: KProperty<*>): Required<T> = apply {
        label = property.name
    }

}

/**
 * What one run of a handler [requires], carried in its coroutine context so that suspension helpers find it.
 *
 * A run is one coroutine, and every coroutine started on a listener's scope gets its own copy of this: empty, like a
 * run that has not required anything yet. `withContext` keeps it, as the code it runs is still part of the run.
 */
class Guards internal constructor(
    private val listener: EventListener? = null,
) : CopyableThreadContextElement<Unit> {

    companion object Key : CoroutineContext.Key<Guards>

    override val key: CoroutineContext.Key<*>
        get() = Key

    /** Replaced instead of changed, so that the threads checking it read a whole one. */
    @Volatile
    private var requirements: Array<Requirement> = emptyArray()

    internal val isIdle: Boolean
        get() = requirements.isEmpty()

    internal fun add(requirement: Requirement) {
        requirements += requirement
        requirement()?.let { throw interrupt(it) }
    }

    /**
     * The interruption for the first requirement that no longer holds.
     */
    internal fun broken(): GuardInterruption? {
        for (requirement in requirements) {
            requirement()?.let { return interrupt(it) }
        }
        return null
    }

    private fun interrupt(requirement: String) =
        GuardInterruption(requirement).also { logger.debug("Run of {} ended: {}", listener, requirement) }

    override fun copyForChild() = Guards(listener)

    override fun mergeForChild(overwritingElement: CoroutineContext.Element): CoroutineContext = Guards(listener)

    override fun updateThreadContext(context: CoroutineContext) = Unit

    override fun restoreThreadContext(context: CoroutineContext, oldState: Unit) = Unit

}

/**
 * The guards of the coroutine this suspends in, or null outside of a listener's coroutines. Idle ones are null too,
 * so waits that require nothing skip the checks.
 */
internal fun CoroutineContext.activeGuards(): Guards? = this[Guards]?.takeUnless { it.isIdle }

/**
 * Ends a run at the suspension where [requirement] stopped holding. Never reaches module code.
 */
internal class GuardInterruption(
    @JvmField val requirement: String,
) : CancellationException("The run ended: $requirement") {
    override fun fillInStackTrace(): Throwable = this
}

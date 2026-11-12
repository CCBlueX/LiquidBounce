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
@file:OptIn(ExperimentalContracts::class)

package net.ccbluex.liquidbounce.event

import it.unimi.dsi.fastutil.objects.ReferenceArrayList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Something the work inside a [guarded] scope depends on, such as "the target is alive".
 */
fun interface Condition {
    fun holds(): Boolean
}

/**
 * How a [guarded] scope ended.
 */
sealed interface Guarded<out T> {

    data class Completed<out T>(val value: T) : Guarded<T>

    /**
     * [condition] stopped holding while the scope waited, so nothing after that suspension ran.
     */
    data class Interrupted(val condition: Condition) : Guarded<Nothing>
}

fun <T> Guarded<T>.getOrNull(): T? = (this as? Guarded.Completed<T>)?.value

fun <T> Guarded<T>.isInterrupted(): Boolean {
    contract {
        returns(true) implies (this@isInterrupted is Guarded.Interrupted)
        returns(false) implies (this@isInterrupted is Guarded.Completed<T>)
    }
    return this is Guarded.Interrupted
}

inline fun <T> Guarded<T>.onInterrupted(action: (Condition) -> Unit): Guarded<T> {
    contract { callsInPlace(action, InvocationKind.AT_MOST_ONCE) }
    if (this is Guarded.Interrupted) {
        action(condition)
    }
    return this
}

/**
 * Runs [block] while [conditions] hold.
 *
 * The suspension helpers ([tickUntil], [waitTicks], [tickConditional], [waitSeconds] and [waitMatches])
 * called inside check the conditions of this scope and of every enclosing one on each tick they wait and
 * once more right before the code after them would run ([waitFor] only does the latter). The first
 * condition that fails ends the scope at that suspension, and the result is [Guarded.Interrupted] with it.
 * Conditions added from within [block] apply from then on. Other suspensions, such as
 * [kotlinx.coroutines.delay], are only covered by the next helper.
 *
 * A condition failing in an enclosing scope ends that scope instead, so an inner scope never swallows
 * it. Exceptions from [block] or from a condition, and cancellation (for example the module being
 * disabled), pass through unchanged.
 */
suspend fun <T> guarded(vararg conditions: Condition, block: suspend GuardScope.() -> T): Guarded<T> {
    contract { callsInPlace(block, InvocationKind.AT_MOST_ONCE) }

    val scope = GuardScope(currentCoroutineContext()[GuardScope])
    return try {
        withContext(scope) {
            for (condition in conditions) {
                scope.guard(condition)
            }
            Guarded.Completed(scope.block())
        }
    } catch (interruption: GuardInterruption) {
        if (interruption.scope !== scope) {
            throw interruption
        }
        Guarded.Interrupted(interruption.condition)
    }
}

/**
 * The conditions of one [guarded] scope, carried in the coroutine context so that suspension helpers find them.
 */
class GuardScope internal constructor(
    private val parent: GuardScope?,
) : AbstractCoroutineContextElement(GuardScope) {

    companion object Key : CoroutineContext.Key<GuardScope>

    private val conditions = ReferenceArrayList<Condition>(4)

    /**
     * Adds [condition] to this scope. If it does not hold right now, the scope ends here.
     */
    fun guard(condition: Condition): Condition {
        conditions.add(condition)
        if (!condition.holds()) {
            throw GuardInterruption(this, condition)
        }
        return condition
    }

    /**
     * A value that is read again on every check of this scope. Reading `null` fails the scope, so the value
     * a delegated property returns after a suspension is always the current one.
     *
     * ```kotlin
     * val target by tracked { targetTracker.target?.takeIf { it.isAlive } }
     * ```
     */
    fun <T : Any> tracked(read: () -> T?): Tracked<T> = Tracked(read).also(::guard)

    /**
     * Holds while [read] returns the same instance it returns now, for example the world or the open screen.
     */
    fun same(read: () -> Any?): Condition {
        val initial = read()
        return guard { read() === initial }
    }

    /**
     * The first condition, outermost scope first, that no longer holds.
     */
    internal fun broken(): GuardInterruption? {
        parent?.broken()?.let { return it }

        for (i in 0 until conditions.size) {
            val condition = conditions[i]
            if (!condition.holds()) {
                return GuardInterruption(this, condition)
            }
        }
        return null
    }

    internal fun ensure() {
        broken()?.let { throw it }
    }

}

class Tracked<T : Any> internal constructor(private val read: () -> T?) : Condition, ReadOnlyProperty<Any?, T> {

    private var current: T? = null

    val value: T
        get() = checkNotNull(current) { "Tracked value read outside of its guarded scope" }

    override fun holds(): Boolean {
        current = read()
        return current != null
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = value

}

/**
 * Unwinds a [guarded] scope from the suspension where [condition] failed. Never reaches module code.
 */
internal class GuardInterruption(
    @JvmField val scope: GuardScope,
    @JvmField val condition: Condition,
) : CancellationException("Guard condition $condition no longer holds") {
    override fun fillInStackTrace(): Throwable = this
}

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
package net.ccbluex.liquidbounce.features.session

import net.ccbluex.liquidbounce.features.addon.AddonApi
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * State a module or manager keeps once per [ClientSession] instead of once per client.
 *
 * ```
 * private var ticksSinceHit by sessionState { 0 }
 * private val queue by sessionState { ArrayDeque<Packet<*>>() }
 * ```
 *
 * [initial] runs the first time a session reads the state.
 */
@AddonApi
fun <T> sessionState(initial: () -> T) = SessionState(initial)

@AddonApi
class SessionState<T>(private val initial: () -> T) : ReadWriteProperty<Any?, T> {

    /**
     * The main session gets a plain field, which keeps the cost of a client with one session at a field read.
     */
    @Volatile
    private var main: Any? = Unset

    @Suppress("UNCHECKED_CAST")
    fun get(): T {
        val session = SessionContext.current
        if (session.isMain) {
            if (main === Unset) {
                main = initial()
            }
            return main as T
        }

        val value = session.states.computeIfAbsent(this) { initial() ?: Null }
        return (if (value === Null) null else value) as T
    }

    fun set(value: T) {
        val session = SessionContext.current
        if (session.isMain) {
            main = value
        } else {
            session.states[this] = value ?: Null
        }
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = get()

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = set(value)

    private object Unset

    private object Null

}

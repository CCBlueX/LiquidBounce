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
package net.ccbluex.liquidbounce.features.client

import net.ccbluex.liquidbounce.features.addon.AddonApi
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * State a module or manager keeps once per [Client] instead of once per client.
 *
 * ```
 * private var ticksSinceHit by clientState { 0 }
 * private val queue by clientState { ArrayDeque<Packet<*>>() }
 * ```
 *
 * [initial] runs the first time a client reads the state.
 */
@AddonApi
fun <T> clientState(initial: () -> T) = ClientState(initial)

@AddonApi
class ClientState<T>(private val initial: () -> T) : ReadWriteProperty<Any?, T> {

    /**
     * The main client gets a plain field, which keeps the cost of a client with one client at a field read.
     */
    @Volatile
    private var main: Any? = Unset

    @Suppress("UNCHECKED_CAST")
    fun get(): T {
        val client = ClientContext.current
        if (client.isMain) {
            if (main === Unset) {
                main = initial()
            }
            return main as T
        }

        val value = client.states.computeIfAbsent(this) { initial() ?: Null }
        return (if (value === Null) null else value) as T
    }

    fun set(value: T) {
        val client = ClientContext.current
        if (client.isMain) {
            main = value
        } else {
            client.states[this] = value ?: Null
        }
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = get()

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = set(value)

    private object Unset

    private object Null

}

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
import net.ccbluex.liquidbounce.features.module.ClientScope
import net.ccbluex.liquidbounce.utils.kotlin.copyIfMutable
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * State a module or manager keeps once per [Client] instead of once for the whole game. A module needs it for all of
 * its state once it runs in [every][ClientScope.EVERY] client.
 *
 * ```
 * private var ticksSinceHit by clientState { 0 }
 * private val queue by clientState { ArrayDeque<Packet<*>>() }
 * ```
 *
 * [initial] runs the first time a client reads the state. A simulated client starts from a copy of the
 * value its parent has, see [copyIfMutable].
 */
@AddonApi
fun <T> clientState(initial: () -> T) = ClientState(initial)

/**
 * Like [clientState], but a simulated client starts from [initial] too. For state that describes the
 * player of the client, which a simulated client replaces with the one it simulates.
 */
internal fun <T> ownClientState(initial: () -> T) = ClientState(initial, inherited = false)

@AddonApi
class ClientState<T> internal constructor(
    private val initial: () -> T,
    private val inherited: Boolean,
) : ReadWriteProperty<Any?, T> {

    constructor(initial: () -> T) : this(initial, inherited = true)

    /**
     * The main client gets a plain field, which keeps the cost of a client with one client at a field read.
     */
    @Volatile
    private var main: Any? = Unset

    fun get(): T = getIn(ClientContext.current)

    @Suppress("UNCHECKED_CAST")
    private fun getIn(client: Client): T {
        if (client.isMain) {
            if (main === Unset) {
                main = initial()
            }
            return main as T
        }

        val value = if (client.isSimulated) {
            client.states.getOrPut(this) { startIn(client) ?: Null }
        } else {
            client.states.computeIfAbsent(this) { initial() ?: Null }
        }
        return (if (value === Null) null else value) as T
    }

    private fun startIn(client: Client): Any? {
        val parent = client.parent
        return if (inherited && parent != null) copyIfMutable(getIn(parent)) else initial()
    }

    fun set(value: T) {
        val client = ClientContext.current
        if (client.isMain) {
            main = value
        } else {
            client.states[this] = value ?: Null
        }
    }

    /**
     * Whether [client] has read or written this state yet.
     */
    internal fun isSetIn(client: Client) = if (client.isMain) main !== Unset else client.states.containsKey(this)

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = get()

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = set(value)

    private object Unset

    private object Null

}

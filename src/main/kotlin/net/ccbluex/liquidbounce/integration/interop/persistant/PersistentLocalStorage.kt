/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
 *
 */
package net.ccbluex.liquidbounce.integration.interop.persistant

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.nesting.Configurable

object PersistentLocalStorage : Configurable("storage"), MutableMap<String, String> {

    private val map by value("map", mutableMapOf<String, String>())

    init {
        ConfigSystem.root(this)
    }

    operator fun set(name: String, value: Boolean) {
        map[name] = value.toString()
    }

    operator fun set(name: String, value: Int) {
        map[name] = value.toString()
    }

    override val size: Int
        get() = map.size

    override fun containsKey(key: String): Boolean = map.containsKey(key)

    override fun containsValue(value: String): Boolean = map.containsValue(value)

    override fun get(key: String): String? = map[key]

    override fun isEmpty(): Boolean = map.isEmpty()

    override val entries: MutableSet<MutableMap.MutableEntry<String, String>>
        get() = map.entries

    override val keys: MutableSet<String>
        get() = map.keys

    override val values: MutableCollection<String>
        get() = map.values

    override fun clear() = map.clear()

    override fun put(key: String, value: String): String? = map.put(key, value)

    override fun putAll(from: Map<out String, String>) = map.putAll(from)

    override fun remove(key: String): String? = map.remove(key)

}

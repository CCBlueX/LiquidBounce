/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.web.persistant

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.Configurable

object PersistentLocalStorage : Configurable("storage") {

    val map by value("map", mutableMapOf<String, String>())

    init {
        ConfigSystem.root(this)
    }

    fun setItem(name: String, value: Boolean) {
        setItem(name, value.toString())
    }

    fun setItem(name: String, value: Int) {
        setItem(name, value.toString())
    }

    fun setItem(name: String, value: String) {
        map[name] = value
    }

    fun getItem(name: String): String? = map[name]

    fun removeItem(name: String) {
        map.remove(name)
    }

    fun clear() {
        map.clear()
    }

}

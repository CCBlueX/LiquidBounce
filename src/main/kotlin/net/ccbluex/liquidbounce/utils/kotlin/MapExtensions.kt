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
 */
package net.ccbluex.liquidbounce.utils.kotlin

/**
 * Sums the values for matching keys from another map to this map.
 * Modifies the current collection.
 */
fun <K> MutableMap<K, Int>.sumValues(anotherMap: Map<K, Int>): MutableMap<K, Int> {
    anotherMap.forEach { (key, amount) ->
        this[key] = (this[key] ?: 0) + amount
    }
    return this
}

fun <K> MutableMap<K, Int>.incrementOrSet(key: K, amount: Int) {
    this[key] = (this[key] ?: 0) + amount
}

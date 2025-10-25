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

package net.ccbluex.liquidbounce.config.types

import net.ccbluex.liquidbounce.config.gson.stategies.Exclude

/**
 * Ranged value adds support for closed ranges
 */
class RangedValue<T : Any>(
    name: String,
    aliases: List<String> = emptyList(),
    defaultValue: T,
    @Exclude val range: ClosedRange<*>,
    @Exclude val suffix: String,
    valueType: ValueType
) : Value<T>(name, aliases, defaultValue, valueType) {

    @Suppress("UNCHECKED_CAST")
    override fun setByString(string: String) {
        if (this.inner is ClosedRange<*>) {
            val split = string.split("..")

            require(split.size == 2)

            val closedRange = this.inner as ClosedRange<*>

            val newValue = when (closedRange.start) {
                is Int -> split[0].toInt()..split[1].toInt()
                is Long -> split[0].toLong()..split[1].toLong()
                is Float -> split[0].toFloat()..split[1].toFloat()
                is Double -> split[0].toDouble()..split[1].toDouble()
                else -> error("unrecognised range value type")
            }

            set(newValue as T)
        } else {
            val translationFunction: (String) -> Any = when (this.inner) {
                is Int -> String::toInt
                is Long -> String::toLong
                is Float -> String::toFloat
                is Double -> String::toDouble
                else -> error("unrecognised value type")
            }

            set(translationFunction(string) as T)
        }
    }

}

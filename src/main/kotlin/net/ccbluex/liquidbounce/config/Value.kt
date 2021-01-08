/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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
package net.ccbluex.liquidbounce.config

import com.google.gson.annotations.SerializedName
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import java.awt.Color
import kotlin.reflect.KProperty

/**
 * Value data class with generics and support for readable names and description
 */
data class Value<T>(@SerializedName("name")
                    val name: String,
                    @Exclude
                    var description: String = "",
                    @SerializedName("value")
                    var value: T
) {

    /**
     * Support for delegated properties
     * example:
     *  var autoaim by boolean(name = "autoaim", default = true)
     *  if(!autoaim)
     *    autoaim = true
     *
     * Important: To use values a class has to be configurable
     *
     * @docs https://kotlinlang.org/docs/reference/delegated-properties.html
     */

    operator fun getValue(configurable: Configurable, property: KProperty<*>) = value

    operator fun setValue(configurable: Configurable, property: KProperty<*>, t: T) {
        value = t
    }

}

fun Configurable.boolean(name: String, default: Boolean = false) = Value(name, value = default)
    .apply { values.add(this) }

fun Configurable.float(name: String, default: Float = 1.0f,
                       range: ClosedFloatingPointRange<Float> = 0.0f..default) = Value(name, value = default).apply { values.add(this) }

fun Configurable.floatRange(name: String, default: ClosedFloatingPointRange<Float> = 0.0f..1.0f,
                       range: ClosedFloatingPointRange<Float> = default) = Value(name, value = default).apply { values.add(this) }

fun Configurable.int(name: String, default: Int = 1,
                     range: IntRange = 0..default) = Value(name, value = default).apply { values.add(this) }

fun Configurable.intRange(name: String, default: IntRange = 0..1,
                          range: IntRange = default) = Value(name, value = default).apply { values.add(this) }

fun Configurable.text(name: String, default: String = "") = Value(name, value = default).apply { values.add(this) }

fun Configurable.color(name: String, color: Color = Color.WHITE) = Value(name, value = color).apply { values.add(this) }

fun Configurable.block(name: String, default: Block = Blocks.AIR) = Value(name, value = default)
    .apply { values.add(this) }

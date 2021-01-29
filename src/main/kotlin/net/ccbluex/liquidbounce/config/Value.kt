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
import net.ccbluex.liquidbounce.features.module.Mode
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import java.awt.Color
import kotlin.reflect.KProperty

/**
 * Value based on generics and support for readable names and description
 */
open class Value<T>(@SerializedName("name")
                    val name: String,
                    @Exclude
                    var description: String = "",
                    @SerializedName("value")
                    var value: T,
                    @Exclude
                    val change: (T, T) -> Unit,
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
        // Just throw out a error to keep the old value
        runCatching {
            change(value, t)
        }.onSuccess {
            value = t
        }
    }

}

/**
 * Ranged value adds support for closed ranges
 */
class RangedValue<T>(name: String, description: String = "", value: T, val range: ClosedRange<*>, change: (T, T) -> Unit = { _, _ -> })
    : Value<T>(name, description, value, change)

/**
 * Ranged value adds support for closed ranges
 */
class ModeValue(name: String, description: String = "", @Exclude val module: Module, mode: String,
                @Exclude val modes: MutableList<Mode>, change: (String, String) -> Unit = { _, _ -> })
    : Value<String>(name, description, mode, { old, new ->
        change(old, new)

        // disable old mode
        modes.find { it.name.equals(old, true) }?.state = false
        // enable new mode
        modes.find { it.name.equals(new, true) }?.state = true
    }) {
        init {
            val currMode = modes.find { it.name.equals(value, true) }

            if (currMode == null) {
                modes.firstOrNull()?.name?.let { value = it }
            } else {
                currMode.state = true
            }
        }
    }

/**
 * Extensions
 */

fun Configurable.boolean(name: String, default: Boolean = false, change: (Boolean, Boolean) -> Unit = { _, _ -> })
    = Value(name, value = default, change = change).apply { values.add(this) }

fun Configurable.float(name: String, default: Float = 1.0f,  range: ClosedFloatingPointRange<Float> = 0.0f..default, change: (Float, Float) -> Unit = { _, _ -> })
    = RangedValue(name, value = default, range = range, change = change).apply { values.add(this) }

fun Configurable.floatRange(name: String, default: ClosedFloatingPointRange<Float> = 0.0f..1.0f, range: ClosedFloatingPointRange<Float> = default, change: (ClosedFloatingPointRange<Float>, ClosedFloatingPointRange<Float>) -> Unit = { _, _ -> })
    = RangedValue(name, value = default, range = range, change = change).apply { values.add(this) }

fun Configurable.int(name: String, default: Int = 1, range: IntRange = 0..default, change: (Int, Int) -> Unit = { _, _ -> })
    = RangedValue(name, value = default, range = range, change = change).apply { values.add(this) }

fun Configurable.intRange(name: String, default: IntRange = 0..1, range: IntRange = default, change: (IntRange, IntRange) -> Unit = { _, _ -> })
    = RangedValue(name, value = default, range = range, change = change).apply { values.add(this) }

fun Configurable.text(name: String, default: String = "", change: (String, String) -> Unit = { _, _ -> })
    = Value(name, value = default, change = change).apply { values.add(this) }

fun Module.mode(name: String, default: String, modes: MutableList<Mode>, change: (String, String) -> Unit = { _, _ -> })
    = ModeValue(name, mode = default, module = this, modes = modes, change = change).apply { values.add(this) }

fun Configurable.color(name: String, color: Color = Color.WHITE, change: (Color, Color) -> Unit = { _, _ -> })
    = Value(name, value = color, change = change).apply { values.add(this) }

fun Configurable.block(name: String, default: Block = Blocks.AIR, change: (Block, Block) -> Unit = { _, _ -> })
    = Value(name, value = default, change = change).apply { values.add(this) }

fun Configurable.blocks(name: String, default: MutableList<Block> = mutableListOf(), change: (MutableList<Block>, MutableList<Block>) -> Unit = { _, _ -> })
    = Value(name, value = default, change = change).apply { values.add(this) }

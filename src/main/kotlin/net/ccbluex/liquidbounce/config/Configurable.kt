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
import net.ccbluex.liquidbounce.features.module.ChoiceConfigurable
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.Fonts
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.logger
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.item.Item
import net.minecraft.item.Items
import java.lang.reflect.Modifier
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.isAccessible

/**
 * Value based on generics and support for readable names and description
 */
open class Value<T>(@SerializedName("name")
                    open val name: String,
                    @SerializedName("value") internal var value: T,
                    @Exclude
                    private val change: (T, T) -> Unit = { _, _ -> },
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

    operator fun getValue(u: Any?, property: KProperty<*>) = value

    operator fun setValue(u: Any?, property: KProperty<*>, t: T) {
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
class RangedValue<T>(name: String, value: T, @Exclude val range: ClosedRange<*>, change: (T, T) -> Unit = { _, _ -> })
    : Value<T>(name, value, change)

class ChooseListValue(name: String, selected: String, @Exclude val selectables: Array<String>, change: (String, String) -> Unit = { _, _ -> })
    : Value<String>(name, selected, change)

open class Configurable(name: String, value: MutableList<Value<*>> = mutableListOf()): Value<MutableList<Value<*>>>(name, value = value) {

    init {
        for (field in javaClass.declaredFields) {
            if (Modifier.isStatic(field.modifiers) || field.isAnnotationPresent(Exclude::class.java) || !Value::class.java.isAssignableFrom(
                    field.type
                )
            )
                continue

            if (!field.isAccessible)
                field.isAccessible = true

            val v = field.get(this) ?: continue

            this.value.add(v as Value<*>)
        }
    }

    fun initConfigurable() {
        value.filterIsInstance<ChoiceConfigurable>().forEach {
            it.initialize(it)
        }
    }

    protected fun <T : Configurable> tree(configurable: T): T {
        value.add(configurable)
        return configurable
    }

    protected fun <T> value(name: String, default: T, change: (T, T) -> Unit = { _, _ -> })
        = Value(name, value = default, change = change).apply { this@Configurable.value.add(this) }

    protected fun boolean(name: String, default: Boolean = false, change: (Boolean, Boolean) -> Unit = { _, _ -> })
        = Value(name, value = default, change = change).apply { this@Configurable.value.add(this) }

    protected fun float(name: String, default: Float = 1.0f,  range: ClosedFloatingPointRange<Float> = 0.0f..default, change: (Float, Float) -> Unit = { _, _ -> })
        = RangedValue(name, value = default, range = range, change = change).apply { this@Configurable.value.add(this) }

    protected fun floatRange(name: String, default: ClosedFloatingPointRange<Float> = 0.0f..1.0f, range: ClosedFloatingPointRange<Float> = default, change: (ClosedFloatingPointRange<Float>, ClosedFloatingPointRange<Float>) -> Unit = { _, _ -> })
        = RangedValue(name, value = default, range = range, change = change).apply { this@Configurable.value.add(this) }

    protected fun int(name: String, default: Int = 1, range: IntRange = 0..default, change: (Int, Int) -> Unit = { _, _ -> })
        = RangedValue(name, value = default, range = range, change = change).apply { this@Configurable.value.add(this) }

    protected fun intRange(name: String, default: IntRange = 0..1, range: IntRange = default, change: (IntRange, IntRange) -> Unit = { _, _ -> })
        = RangedValue(name, value = default, range = range, change = change).apply { this@Configurable.value.add(this) }

    protected fun text(name: String, default: String = "", change: (String, String) -> Unit = { _, _ -> })
        = Value(name, value = default, change = change).apply { this@Configurable.value.add(this) }

    protected fun textArray(name: String, default: MutableList<String> = mutableListOf(), change: (MutableList<String>, MutableList<String>) -> Unit = { _, _ -> })
        = Value(name, value = default, change = change).apply { this@Configurable.value.add(this) }

    protected fun chooseList(name: String, default: String, array: Array<String>, change: (String, String) -> Unit = { _, _ -> })
        = ChooseListValue(name, selected = default, selectables = array, change = change).apply { this@Configurable.value.add(this) }

    protected fun curve(name: String, default: Array<Float>, change: (Array<Float>, Array<Float>) -> Unit = { _, _ -> }) =
        Value(name, value = default, change = change).apply { this@Configurable.value.add(this) }

    protected fun color(
        name: String,
        color: Color4b = Color4b(255, 255, 255, 255),
        change: (Color4b, Color4b) -> Unit = { _, _ -> }
    ) = Value(name, value = color, change = change).apply { this@Configurable.value.add(this) }

    protected fun block(name: String, default: Block = Blocks.AIR, change: (Block, Block) -> Unit = { _, _ -> }) =
        Value(name, value = default, change = change).apply { this@Configurable.value.add(this) }

    protected fun blocks(
        name: String,
        default: MutableList<Block> = mutableListOf(),
        change: (MutableList<Block>, MutableList<Block>) -> Unit = { _, _ -> }
    ) = Value(name, value = default, change = change).apply { this@Configurable.value.add(this) }

    protected fun item(name: String, default: Item = Items.AIR, change: (Item, Item) -> Unit = { _, _ -> }) =
        Value(name, value = default, change = change).apply { this@Configurable.value.add(this) }

    protected fun items(
        name: String,
        default: MutableList<Item> = mutableListOf(),
        change: (MutableList<Item>, MutableList<Item>) -> Unit = { _, _ -> }
    ) = Value(name, value = default, change = change).apply { this@Configurable.value.add(this) }

    protected fun fonts(
        name: String,
        default: MutableList<Fonts.FontDetail> = mutableListOf(),
        change: (MutableList<Fonts.FontDetail>, MutableList<Fonts.FontDetail>) -> Unit = { _, _ -> }
    ) = Value(name, value = default, change = change).apply { this@Configurable.value.add(this) }

    protected fun Module.choices(name: String, active: String, initialize: (ChoiceConfigurable) -> Unit) =
        ChoiceConfigurable(this, name, active, initialize).apply { this@Configurable.value.add(this) }

    /**
     * Overwrite current configurable and their existing values from [configurable].
     * [skipNew] allows to skip unknown new values and configurables.
     */
    fun overwrite(configurable: Configurable, skipNew: Boolean = true) {
        if (!skipNew || value.isNotEmpty()) {
            for (nev in configurable.value) {
                val oev = value.find { it.name == nev.name } ?: continue

                runCatching {
                    val ref = oev::value
                    if (!ref.isAccessible)
                        ref.isAccessible = true
                    ref.set(nev.value)
                }.onFailure {
                    logger.error("Unable to overwrite value ${oev.name}:value:${oev.value} to ${nev.value}", it)
                }
            }
        }
    }

}

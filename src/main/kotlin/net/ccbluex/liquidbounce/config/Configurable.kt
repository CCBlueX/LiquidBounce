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

import net.ccbluex.liquidbounce.config.util.Exclude
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.Fonts
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.minecraft.block.Block
import net.minecraft.item.Item
import java.lang.reflect.Modifier

open class Configurable(name: String, value: MutableList<Value<*>> = mutableListOf()) :
    Value<MutableList<Value<*>>>(name, value = value) {

    init {
        for (field in javaClass.declaredFields) {
            if (Modifier.isStatic(field.modifiers) || field.isAnnotationPresent(Exclude::class.java) ||
                !Value::class.java.isAssignableFrom(field.type)
            ) {
                continue
            }

            if (!field.isAccessible) {
                field.isAccessible = true
            }

            val v = field.get(this) ?: continue

            this.value.add(v as Value<*>)
        }
    }

    fun initConfigurable() {
        value.filterIsInstance<ChoiceConfigurable>().forEach {
            it.initialize(it)
        }
    }

    // Common value types

    protected fun <T : Configurable> tree(configurable: T): T {
        value.add(configurable)
        return configurable
    }

    protected fun <T : Any> value(name: String, default: T, listType: ListValueType = ListValueType.None) =
        Value(name, default, listType).apply { this@Configurable.value.add(this) }

    protected fun <T : Any> rangedValue(name: String, default: T, range: ClosedRange<*>) =
        RangedValue(name, default, range).apply { this@Configurable.value.add(this) }

    // Fixed data types

    protected fun boolean(name: String, default: Boolean) =
        value(name, default)

    protected fun float(name: String, default: Float, range: ClosedFloatingPointRange<Float>) =
        rangedValue(name, default, range)

    protected fun floatRange(name: String, default: ClosedFloatingPointRange<Float>, range: ClosedFloatingPointRange<Float>) =
        rangedValue(name, default, range)

    protected fun int(name: String, default: Int, range: IntRange) =
        rangedValue(name, default, range)

    protected fun intRange(name: String, default: IntRange, range: IntRange) =
        rangedValue(name, default, range)

    protected fun text(name: String, default: String) =
        value(name, default)

    protected fun textArray(name: String, default: MutableList<String>) =
        value(name, default, ListValueType.String)

    protected fun curve(name: String, default: Array<Float>) =
        value(name, default)

    protected fun color(name: String, default: Color4b) =
        value(name, default)

    protected fun block(name: String, default: Block) =
        value(name, default)

    protected fun blocks(name: String, default: MutableList<Block>) =
        value(name, default, ListValueType.Block)

    protected fun item(name: String, default: Item) =
        value(name, default)

    protected fun items(name: String, default: MutableList<Item>) =
        value(name, default, ListValueType.Item)

    protected fun fonts(name: String, default: MutableList<Fonts.FontDetail>) =
        value(name, default, ListValueType.FontDetail)

    protected fun <T : NamedChoice> enumChoice(name: String, default: T, choices: Array<T>) =
        ChooseListValue(name, default, choices).apply { this@Configurable.value.add(this) }

    protected fun Module.choices(name: String, active: String, initialize: (ChoiceConfigurable) -> Unit) =
        ChoiceConfigurable(this, name, active, initialize).apply { this@Configurable.value.add(this) }

}

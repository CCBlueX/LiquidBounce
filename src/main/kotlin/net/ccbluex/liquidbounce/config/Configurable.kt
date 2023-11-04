/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.Fonts
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.client.Curves
import net.minecraft.block.Block
import net.minecraft.item.Item

open class Configurable(
    name: String,
    value: MutableList<Value<*>> = mutableListOf(),
    valueType: ValueType = ValueType.CONFIGURABLE
) :
    Value<MutableList<Value<*>>>(name, value = value, valueType) {

    open fun initConfigurable() {
        value.filterIsInstance<Configurable>().forEach {
            it.initConfigurable()
        }
    }

    @get:JvmName("getContainedValues")
    val containedValues: Array<Value<*>>
        get() = this.value.toTypedArray()

    fun getContainedValuesRecursively(): Array<Value<*>> {
        val output = mutableListOf<Value<*>>()

        this.getContainedValuesRecursivelyInternal(output)

        return output.toTypedArray()
    }

    fun getContainedValuesRecursivelyInternal(output: MutableList<Value<*>>) {
        for (currentValue in this.value) {
            if (currentValue is ToggleableConfigurable) {
                output.add(currentValue)
                output.addAll(currentValue.value.filter { it.name.equals("Enabled", true) })
            } else {
                if (currentValue is Configurable) {
                    currentValue.getContainedValuesRecursivelyInternal(output)
                } else {
                    output.add(currentValue)
                }
            }

            if (currentValue is ChoiceConfigurable) {
                output.add(currentValue)

                currentValue.choices.filter { it.isActive }.forEach {
                    it.getContainedValuesRecursivelyInternal(output)
                }
            }
        }
    }

    // Common value types

    protected fun <T : Configurable> tree(configurable: T): T {
        value.add(configurable)
        return configurable
    }

    protected fun <T : Any> value(
        name: String,
        default: T,
        valueType: ValueType = ValueType.INVALID,
        listType: ListValueType = ListValueType.None
    ) =
        Value(name, default, valueType, listType).apply { this@Configurable.value.add(this) }

    protected fun <T : Any> rangedValue(name: String, default: T, range: ClosedRange<*>, valueType: ValueType) =
        RangedValue(name, default, range, valueType).apply { this@Configurable.value.add(this) }

    // Fixed data types

    protected fun boolean(name: String, default: Boolean) = value(name, default, ValueType.BOOLEAN)

    protected fun float(name: String, default: Float, range: ClosedFloatingPointRange<Float>) =
        rangedValue(name, default, range, ValueType.FLOAT)

    protected fun floatRange(
        name: String,
        default: ClosedFloatingPointRange<Float>,
        range: ClosedFloatingPointRange<Float>
    ) = rangedValue(name, default, range, ValueType.FLOAT_RANGE)

    protected fun int(name: String, default: Int, range: IntRange) = rangedValue(name, default, range, ValueType.INT)

    protected fun intRange(name: String, default: IntRange, range: IntRange) =
        rangedValue(name, default, range, ValueType.INT_RANGE)

    protected fun text(name: String, default: String) = value(name, default, ValueType.TEXT)

    protected fun textArray(name: String, default: MutableList<String>) =
        value(name, default, ValueType.TEXT_ARRAY, ListValueType.String)

    protected fun curve(name: String, default: Curves) =
        ChooseListValue(name, default, Curves.values()).apply { this@Configurable.value.add(this) }


    protected fun color(name: String, default: Color4b) = value(name, default, ValueType.COLOR)

    protected fun block(name: String, default: Block) = value(name, default, ValueType.BLOCK)

    protected fun blocks(name: String, default: MutableSet<Block>) =
        value(name, default, ValueType.BLOCKS, ListValueType.Block)

    protected fun item(name: String, default: Item) = value(name, default, ValueType.ITEM)

    protected fun items(name: String, default: MutableList<Item>) =
        value(name, default, ValueType.ITEMS, ListValueType.Item)

    protected fun fonts(name: String, default: MutableList<Fonts.FontDetail>) =
        value(name, default, ValueType.INVALID, ListValueType.FontDetail)

    protected fun <T : NamedChoice> enumChoice(name: String, default: T, choices: Array<T>) =
        ChooseListValue(name, default, choices).apply { this@Configurable.value.add(this) }

    protected fun Module.choices(name: String, active: Choice, choices: Array<Choice>) =
        ChoiceConfigurable(this, name, active) { choices }.apply { this@Configurable.value.add(this) }

    protected fun Module.choices(name: String, active: Choice, choicesCallback: (ChoiceConfigurable) -> Array<Choice>) =
        ChoiceConfigurable(this, name, active, choicesCallback).apply { this@Configurable.value.add(this) }

}

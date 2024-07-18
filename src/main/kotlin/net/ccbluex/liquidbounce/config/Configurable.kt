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
 */
package net.ccbluex.liquidbounce.config

import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.client.Curves
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.toLowerCamelCase
import net.minecraft.block.Block
import net.minecraft.item.Item

open class Configurable(
    name: String,
    value: MutableList<Value<*>> = mutableListOf(),
    valueType: ValueType = ValueType.CONFIGURABLE,

    /**
     * Signalizes that the [Configurable]'s translation key
     * should not depend on another [Configurable].
     * This means the [translationBaseKey] will be directly used.
     *
     * The options should be used in common options, so that
     * descriptions don't have to be written twice.
     */
    independentDescription: Boolean = false
) : Value<MutableList<Value<*>>>(
    name,
    inner = value,
    valueType,
    independentDescription = independentDescription
) {

    /**
     * Stores the [Configurable] in which
     * the [Configurable] is included, can be null.
     */
    var base: Configurable? = null

    /**
     * The translation base used when [base] is null,
     * otherwise the [translationBaseKey] from [base]
     * is used when its base is null and so on.
     */
    open val translationBaseKey: String
        get() = "liquidbounce.option.${name.toLowerCamelCase()}"

    open fun initConfigurable() {
        inner.filterIsInstance<Configurable>().forEach {
            it.initConfigurable()
        }
    }

    /**
     * Creates the description keys for this [Configurable] and all values.
     */
    fun loadDescriptionKeys(previousBaseKey: String? = null) {
        val baseKey = if (independentDescription) {
            translationBaseKey
        } else if (previousBaseKey != null) {
            "$previousBaseKey.${name.toLowerCamelCase()}"
        } else {
            constructTranslationBaseKey()
        }

        this.descriptionKey = "$baseKey.description"
        // TODO remove debug logger
        logger.info(descriptionKey)

        for (currentValue in this.inner) {
            if (currentValue is Configurable) {
                currentValue.loadDescriptionKeys(baseKey)
            } else {
                currentValue.descriptionKey = if (currentValue.independentDescription) {
                    "liquidbounce.common.value.${currentValue.name.toLowerCamelCase()}.description"
                } else {
                    "$baseKey.value.${currentValue.name.toLowerCamelCase()}.description"
                }
                logger.info(currentValue.descriptionKey)
            }

            if (currentValue is ChoiceConfigurable<*>) {
                val suffix = ".description"
                val currentKey = currentValue.descriptionKey!!.substringBeforeLast(suffix)

                currentValue.choices.forEach { it.loadDescriptionKeys(currentKey) }
            }
        }
    }

    /**
     * Joins the names of all bases and this and the [translationBaseKey] of the lowest
     * base together to create a translation base key.
     */
    private fun constructTranslationBaseKey(): String {
        val values = mutableListOf<String>()
        var current: Configurable? = this
        while (current != null) {
            val base1 = current.base
            if (base1 == null) {
                values.add(current.translationBaseKey)
            } else {
                values.add(current.name.toLowerCamelCase())
            }
            current = base1
        }
        values.reverse()
        return values.joinToString(".")
    }

    @get:JvmName("getContainedValues")
    val containedValues: Array<Value<*>>
        get() = this.inner.toTypedArray()

    fun getContainedValuesRecursively(): Array<Value<*>> {
        val output = mutableListOf<Value<*>>()

        this.getContainedValuesRecursivelyInternal(output)

        return output.toTypedArray()
    }

    fun getContainedValuesRecursivelyInternal(output: MutableList<Value<*>>) {
        for (currentValue in this.inner) {
            if (currentValue is ToggleableConfigurable) {
                output.add(currentValue)
                output.addAll(currentValue.inner.filter { it.name.equals("Enabled", true) })
            } else {
                if (currentValue is Configurable) {
                    currentValue.getContainedValuesRecursivelyInternal(output)
                } else {
                    output.add(currentValue)
                }
            }

            if (currentValue is ChoiceConfigurable<*>) {
                output.add(currentValue)

                currentValue.choices.filter { it.isActive }.forEach {
                    it.getContainedValuesRecursivelyInternal(output)
                }
            }
        }
    }

    // Common value types

    protected fun <T : Configurable> tree(configurable: T): T {
        inner.add(configurable)
        configurable.base = this
        return configurable
    }

    protected fun <T : Any> value(
        name: String, default: T, valueType: ValueType = ValueType.INVALID, listType: ListValueType = ListValueType.None
    ) = Value(name, default, valueType, listType).apply { this@Configurable.inner.add(this) }

    private fun <T : Any> rangedValue(
        name: String, default: T, range: ClosedRange<*>, suffix: String, valueType: ValueType
    ) = RangedValue(name, default, range, suffix, valueType).apply { this@Configurable.inner.add(this) }

    // Fixed data types

    protected fun boolean(name: String, default: Boolean) = value(name, default, ValueType.BOOLEAN)

    protected fun float(name: String, default: Float, range: ClosedFloatingPointRange<Float>, suffix: String = "") =
        rangedValue(name, default, range, suffix, ValueType.FLOAT)

    protected fun floatRange(
        name: String,
        default: ClosedFloatingPointRange<Float>,
        range: ClosedFloatingPointRange<Float>,
        suffix: String = ""
    ) = rangedValue(name, default, range, suffix, ValueType.FLOAT_RANGE)

    protected fun int(name: String, default: Int, range: IntRange, suffix: String = "") =
        rangedValue(name, default, range, suffix, ValueType.INT)

    protected fun key(name: String, default: Int) = value(name, default, ValueType.KEY)

    protected fun intRange(name: String, default: IntRange, range: IntRange, suffix: String = "") =
        rangedValue(name, default, range, suffix, ValueType.INT_RANGE)

    protected fun text(name: String, default: String) = value(name, default, ValueType.TEXT)

    protected fun textArray(name: String, default: MutableList<String>) =
        value(name, default, ValueType.TEXT_ARRAY, ListValueType.String)

    protected fun curve(name: String, default: Curves) = enumChoice(name, default)

    protected fun color(name: String, default: Color4b) = value(name, default, ValueType.COLOR)

    protected fun block(name: String, default: Block) = value(name, default, ValueType.BLOCK)

    protected fun blocks(name: String, default: MutableSet<Block>) =
        value(name, default, ValueType.BLOCKS, ListValueType.Block)

    protected fun item(name: String, default: Item) = value(name, default, ValueType.ITEM)

    protected fun items(name: String, default: MutableList<Item>) =
        value(name, default, ValueType.ITEMS, ListValueType.Item)

    internal inline fun <reified T> enumChoice(name: String, default: T): ChooseListValue<T>
        where T : Enum<T>, T : NamedChoice = enumChoice(name, default, enumValues<T>())

    protected fun <T> enumChoice(name: String, default: T, choices: Array<T>): ChooseListValue<T>
        where T : Enum<T>, T : NamedChoice =
        ChooseListValue(name, default, choices).apply { this@Configurable.inner.add(this) }

    protected fun <T : Choice> choices(
        listenable: Listenable,
        name: String,
        active: T,
        choices: Array<T>
    ): ChoiceConfigurable<T> {
        return ChoiceConfigurable<T>(listenable, name, { active }) { choices }.apply {
            this@Configurable.inner.add(this)
            this.base = this@Configurable
        }
    }

    protected fun <T : Choice> choices(
        listenable: Listenable,
        name: String,
        activeCallback: (ChoiceConfigurable<T>) -> T,
        choicesCallback: (ChoiceConfigurable<T>) -> Array<T>
    ): ChoiceConfigurable<T> {
        return ChoiceConfigurable<T>(listenable, name, activeCallback, choicesCallback).apply {
            this@Configurable.inner.add(this)
            this.base = this@Configurable
        }
    }

    protected fun value(value: Value<*>) = value.apply { this@Configurable.inner.add(this) }

}

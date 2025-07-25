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
package net.ccbluex.liquidbounce.config.types.nesting

import net.ccbluex.liquidbounce.config.types.*
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.toLowerCamelCase
import net.ccbluex.liquidbounce.utils.input.InputBind
import net.ccbluex.liquidbounce.utils.kotlin.emptyEnumSet
import net.ccbluex.liquidbounce.utils.kotlin.toEnumSet
import net.ccbluex.liquidbounce.utils.math.Easing
import net.minecraft.block.Block
import net.minecraft.client.util.InputUtil
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.item.Item
import net.minecraft.sound.SoundEvent
import net.minecraft.util.Identifier
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import org.lwjgl.glfw.GLFW
import java.util.*
import kotlin.enums.EnumEntries

@Suppress("TooManyFunctions")
open class Configurable(
    name: String,
    value: MutableList<Value<*>> = mutableListOf(),
    valueType: ValueType = ValueType.CONFIGURABLE,

    /**
     * Signalizes that the [Configurable]'s translation key
     * should not depend on another [Configurable].
     * This means the [baseKey] will be directly used.
     *
     * The options should be used in common options, so that
     * descriptions don't have to be written twice.
     */
    independentDescription: Boolean = false,
    /**
     * Used for backwards compatibility when renaming.
     */
    aliases: Array<out String> = emptyArray(),
) : Value<MutableList<Value<*>>>(
    name,
    aliases,
    defaultValue = value,
    valueType,
    independentDescription = independentDescription
) {

    /**
     * Stores the [Configurable] in which
     * the [Configurable] is included, can be null.
     */
    var base: Configurable? = null

    /**
     * The base key used when [base] is null,
     * otherwise the [baseKey] from [base]
     * is used when its base is null and so on.
     */
    open val baseKey: String
        get() = "liquidbounce.option.${name.toLowerCamelCase()}"

    open fun initConfigurable() {
        inner.filterIsInstance<Configurable>().forEach {
            it.initConfigurable()
        }
    }

    /**
     * Walks the path of the [Configurable] and its children
     */
    fun walkKeyPath(previousBaseKey: String? = null) {
        this.key = if (previousBaseKey != null) {
            "$previousBaseKey.${name.toLowerCamelCase()}"
        } else {
            constructBaseKey()
        }

        // Update children
        for (currentValue in this.inner) {
            if (currentValue is Configurable) {
                currentValue.walkKeyPath(this.key)
            } else {
                currentValue.key = "${this.key}.value.${currentValue.name.toLowerCamelCase()}"
            }

            if (currentValue is ChoiceConfigurable<*>) {
                val currentKey = currentValue.key

                currentValue.choices.forEach { choice -> choice.walkKeyPath(currentKey) }
            }
        }
    }

    /**
     * Joins the names of all bases and this and the [baseKey] of the lowest
     * base together to create a translation base key.
     */
    private fun constructBaseKey(): String {
        val values = mutableListOf<String>()
        var current: Configurable? = this
        while (current != null) {
            val base1 = current.base
            if (base1 == null) {
                values.add(current.baseKey)
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
                currentValue.inner.filterTo(output) { it.name.equals("Enabled", true) }
            } else {
                if (currentValue is Configurable) {
                    currentValue.getContainedValuesRecursivelyInternal(output)
                } else {
                    output.add(currentValue)
                }
            }

            if (currentValue is ChoiceConfigurable<*>) {
                output.add(currentValue)

                currentValue.choices.filter { it.isSelected }.forEach {
                    it.getContainedValuesRecursivelyInternal(output)
                }
            }
        }
    }

    /**
     * Restore all values to their default values
     */
    override fun restore() {
        inner.forEach(Value<*>::restore)
    }

    // Common value types

    fun <T : Configurable> tree(configurable: T): T {
        inner.add(configurable)
        configurable.base = this
        return configurable
    }

    fun <T : Configurable> treeAll(vararg configurable: T) {
        configurable.forEach(this::tree)
    }

    fun <T : Any> value(
        name: String,
        defaultValue: T,
        valueType: ValueType = ValueType.INVALID,
    ) = Value(name, defaultValue = defaultValue, valueType = valueType).apply {
        this@Configurable.inner.add(this)
    }

    internal inline fun <T : MutableCollection<E>, reified E> list(
        name: String,
        defaultValue: T,
        valueType: ValueType
    ) = ListValue(name, defaultValue, innerValueType = valueType, innerType = E::class.java).apply {
        this@Configurable.inner.add(this)
    }

    internal inline fun <T : MutableCollection<E>, reified E> mutableList(
        name: String,
        defaultValue: T,
        valueType: ValueType
    ) = MutableListValue(name, defaultValue, valueType, E::class.java).apply {
        this@Configurable.inner.add(this)
    }

    internal inline fun <T : MutableSet<E>, reified E> itemList(
        name: String,
        defaultValue: T,
        items: Set<ItemListValue.NamedItem<E>>,
        valueType: ValueType
    ) = ItemListValue(name, defaultValue, items, valueType, E::class.java).apply {
        this@Configurable.inner.add(this)
    }

    internal inline fun <T : MutableSet<E>, reified E> registryList(
        name: String,
        defaultValue: T,
        valueType: ValueType
    ) = RegistryListValue(name, defaultValue, valueType, E::class.java).apply {
        this@Configurable.inner.add(this)
    }

    fun <T : Any> rangedValue(
        name: String,
        defaultValue: T,
        range: ClosedRange<*>,
        suffix: String,
        valueType: ValueType
    ) = RangedValue(name, defaultValue = defaultValue, range = range, suffix = suffix, valueType = valueType).apply {
        this@Configurable.inner.add(this)
    }

    // Fixed data types

    fun boolean(name: String, default: Boolean) = value(name, default, ValueType.BOOLEAN)

    fun float(name: String, default: Float, range: ClosedFloatingPointRange<Float>, suffix: String = "") =
        rangedValue(name, default, range, suffix, ValueType.FLOAT)

    fun floatRange(
        name: String,
        default: ClosedFloatingPointRange<Float>,
        range: ClosedFloatingPointRange<Float>,
        suffix: String = ""
    ) = rangedValue(name, default, range, suffix, ValueType.FLOAT_RANGE)

    fun int(name: String, default: Int, range: IntRange, suffix: String = "") =
        rangedValue(name, default, range, suffix, ValueType.INT)

    fun bind(name: String, default: Int = GLFW.GLFW_KEY_UNKNOWN) = bind(
        name,
        InputBind(InputUtil.Type.KEYSYM, default, InputBind.BindAction.TOGGLE)
    )

    fun bind(name: String, default: InputBind) = BindValue(name, defaultValue = default).apply {
        this@Configurable.inner.add(this)
    }

    fun key(name: String, default: Int) = key(name, InputUtil.Type.KEYSYM.createFromCode(default))

    fun key(name: String, default: InputUtil.Key = InputUtil.UNKNOWN_KEY) =
        value(name, default, ValueType.KEY)

    fun intRange(name: String, default: IntRange, range: IntRange, suffix: String = "") =
        rangedValue(name, default, range, suffix, ValueType.INT_RANGE)

    fun text(name: String, default: String) = value(name, default, ValueType.TEXT)

    fun <C : MutableCollection<String>> textList(name: String, default: C) =
        mutableList<C, String>(name, default, ValueType.TEXT)

    fun curve(name: String, default: Easing) = enumChoice(name, default)

    fun color(name: String, default: Color4b) = value(name, default, ValueType.COLOR)

    fun block(name: String, default: Block) = value(name, default, ValueType.BLOCK)

    fun vec3i(name: String, default: Vec3i) = value(name, default, ValueType.VECTOR_I)

    fun vec3d(name: String, default: Vec3d) = value(name, default, ValueType.VECTOR_D)

    fun <C : MutableSet<Block>> blocks(name: String, default: C) =
        registryList(name, default, ValueType.BLOCK)

    fun item(name: String, default: Item) = value(name, default, ValueType.ITEM)

    fun <C : MutableSet<Item>> items(name: String, default: C) =
        registryList(name, default, ValueType.ITEM)

    fun <C : MutableSet<SoundEvent>> sounds(name: String, default: C) =
        registryList(name, default, ValueType.SOUND)

    fun <C : MutableSet<StatusEffect>> statusEffects(name: String, default: C) =
        registryList(name, default, ValueType.STATUS_EFFECT)

    fun <C : MutableSet<Identifier>> clientPackets(name: String, default: C) =
        registryList(name, default, ValueType.CLIENT_PACKET)

    fun <C : MutableSet<Identifier>> serverPackets(name: String, default: C) =
        registryList(name, default, ValueType.SERVER_PACKET)

    inline fun <reified T> multiEnumChoice(
        name: String,
        vararg default: T,
        canBeNone: Boolean = true
    ) where T : Enum<T>, T : NamedChoice =
        multiEnumChoice(name, default.toEnumSet(), canBeNone)

    inline fun <reified T> multiEnumChoice(
        name: String,
        default: EnumEntries<T>,
        canBeNone: Boolean = true
    ) where T : Enum<T>, T : NamedChoice =
        multiEnumChoice(name, default.toEnumSet(), canBeNone)

    inline fun <reified T> multiEnumChoice(
        name: String,
        default: EnumSet<T> = emptyEnumSet(),
        canBeNone: Boolean = true
    ) where T : Enum<T>, T : NamedChoice =
        multiEnumChoice(name, default.toEnumSet(), enumValues<T>().toEnumSet(), canBeNone)

    fun <T> multiEnumChoice(
        name: String,
        default: EnumSet<T>,
        choices: EnumSet<T>,
        canBeNone: Boolean = true
    ) where T : Enum<T>, T : NamedChoice =
        MultiChooseEnumListValue(name, default, choices, canBeNone).apply { this@Configurable.inner.add(this@apply) }

    inline fun <reified T> enumChoice(name: String, default: T): ChooseListValue<T>
        where T : Enum<T>, T : NamedChoice = enumChoice(name, default, enumValues<T>())

    fun <T> enumChoice(name: String, default: T, choices: Array<T>): ChooseListValue<T>
        where T : Enum<T>, T : NamedChoice =
        ChooseListValue(name, defaultValue = default, choices = choices).apply { this@Configurable.inner.add(this) }

    protected fun <T : Choice> choices(
        eventListener: EventListener,
        name: String,
        active: T,
        choices: Array<T>
    ): ChoiceConfigurable<T> {
        return choices(eventListener, name, {
            val idx = choices.indexOf(active)

            check(idx != -1) { "The active choice $active is not contained within the choice array ($it)" }

            idx
        }) { choices }
    }

    protected fun <T : Choice> choices(
        eventListener: EventListener,
        name: String,
        activeCallback: (List<T>) -> Int,
        choicesCallback: (ChoiceConfigurable<T>) -> Array<T>
    ): ChoiceConfigurable<T> {
        return ChoiceConfigurable(eventListener, name, activeCallback, choicesCallback).apply {
            this@Configurable.inner.add(this)
            this.base = this@Configurable
        }
    }

    protected fun <T : Choice> choices(
        eventListener: EventListener,
        name: String,
        activeIndex: Int = 0,
        choicesCallback: (ChoiceConfigurable<T>) -> Array<T>
    ) = choices(eventListener, name, { activeIndex }, choicesCallback)

    fun value(value: Value<*>) = value.apply { this@Configurable.inner.add(this) }

}

/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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
package net.ccbluex.liquidbounce.config.types.group

import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.mojang.blaze3d.platform.InputConstants
import net.ccbluex.fastutil.enumSetOf
import net.ccbluex.fastutil.forEachIsInstance
import net.ccbluex.fastutil.toEnumSet
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.BindValue
import net.ccbluex.liquidbounce.config.types.Config
import net.ccbluex.liquidbounce.config.types.CurveValue
import net.ccbluex.liquidbounce.config.types.CurveValue.Axis
import net.ccbluex.liquidbounce.config.types.FileDialogMode
import net.ccbluex.liquidbounce.config.types.FileValue
import net.ccbluex.liquidbounce.config.types.RangedValue
import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.ValueType
import net.ccbluex.liquidbounce.config.types.Vec3Value
import net.ccbluex.liquidbounce.config.types.list.ChoiceListValue
import net.ccbluex.liquidbounce.config.types.list.ItemListValue
import net.ccbluex.liquidbounce.config.types.list.ListValue
import net.ccbluex.liquidbounce.config.types.list.MultiChoiceListValue
import net.ccbluex.liquidbounce.config.types.list.MutableListValue
import net.ccbluex.liquidbounce.config.types.list.RegistryListValue
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.config.types.list.Tagged.Companion.asTagged
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.toLowerCamelCase
import net.ccbluex.liquidbounce.utils.input.InputBind
import net.ccbluex.liquidbounce.utils.math.Easing
import net.minecraft.core.Vec3i
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.Vec3
import org.joml.Vector2f
import org.joml.Vector2fc
import org.lwjgl.glfw.GLFW
import java.io.File
import java.util.EnumSet
import java.util.SequencedSet
import java.util.function.ToIntFunction

@Suppress("TooManyFunctions")
open class ValueGroup(
    name: String,
    value: MutableCollection<Value<*>> = mutableListOf(),
    valueType: ValueType = ValueType.CONFIGURABLE,

    /**
     * Signalizes that the [ValueGroup]'s translation key
     * should not depend on another [ValueGroup].
     * This means the [baseKey] will be directly used.
     *
     * The options should be used in common options, so that
     * descriptions don't have to be written twice.
     */
    independentDescription: Boolean = false,
    /**
     * Used for backwards compatibility when renaming.
     */
    aliases: List<String> = emptyList(),
) : Value<MutableCollection<Value<*>>>(
    name,
    aliases,
    defaultValue = value,
    valueType,
    independentDescription = independentDescription
) {

    /**
     * Stores the [ValueGroup] in which
     * the [ValueGroup] is included, can be null.
     */
    var base: ValueGroup? = null

    /**
     * The base key used when [base] is null,
     * otherwise the [baseKey] from [base]
     * is used when its base is null and so on.
     */
    open val baseKey: String
        get() = "${ConfigSystem.KEY_PREFIX}.${name.toLowerCamelCase()}"

    open fun walkInit() {
        inner.forEachIsInstance<ValueGroup> { valueGroup ->
            valueGroup.walkInit()
        }
    }

    /**
     * Walks the path of the [ValueGroup] and its children
     */
    fun walkKeyPath(previousBaseKey: String? = null) {
        this.key = if (previousBaseKey != null) {
            "$previousBaseKey.${name.toLowerCamelCase()}"
        } else {
            constructBaseKey()
        }

        // Update children
        for (currentValue in this.inner) {
            if (currentValue is ValueGroup) {
                currentValue.walkKeyPath(this.key)
            } else {
                currentValue.key = "${this.key}.${currentValue.name.toLowerCamelCase()}"
            }

            if (currentValue is ModeValueGroup<*>) {
                val currentKey = currentValue.key

                currentValue.modes.forEach { choice -> choice.walkKeyPath(currentKey) }
            }
        }
    }

    /**
     * Joins the names of all bases and this and the [baseKey] of the lowest
     * base together to create a translation base key.
     */
    private fun constructBaseKey(): String {
        val values = mutableListOf<String>()
        var current: ValueGroup? = this
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

    fun collectValuesRecursively(): Array<Value<*>> {
        val output = mutableListOf<Value<*>>()

        this.collectValuesRecursivelyInternal(output)

        return output.toTypedArray()
    }

    protected fun collectValuesRecursivelyInternal(output: MutableList<Value<*>>) {
        for (currentValue in this.inner) {
            if (currentValue is ToggleableValueGroup) {
                output.add(currentValue)
                currentValue.collectValuesRecursivelyInternal(output)
            } else {
                if (currentValue is ValueGroup) {
                    currentValue.collectValuesRecursivelyInternal(output)
                } else {
                    output.add(currentValue)
                }
            }

            if (currentValue is ModeValueGroup<*>) {
                output.add(currentValue)

                currentValue.modes.forEach {
                    it.collectValuesRecursivelyInternal(output)
                }
            }
        }
    }

    fun collectValueGroupsRecursively(): Array<ValueGroup> {
        val output = mutableListOf<ValueGroup>()

        this.collectValueGroupsRecursivelyInternal(output)

        return output.toTypedArray()
    }

    protected fun collectValueGroupsRecursivelyInternal(output: MutableList<ValueGroup>) {
        output.add(this)
        for (currentValue in this.inner) {
            when (currentValue) {
                is ModeValueGroup<*> -> {
                    output.add(currentValue)
                    currentValue.modes.forEach { it.collectValueGroupsRecursivelyInternal(output) }
                }
                is ValueGroup -> currentValue.collectValueGroupsRecursivelyInternal(output)
            }
        }
    }

    fun collectValuesRecursively(prefix: String): Sequence<Value<*>> = sequence {
        val normalizedPrefix = prefix.lowercase()

        suspend fun SequenceScope<Value<*>>.walk(current: ValueGroup) {
            val currentKey = current.key?.lowercase()
            if (!shouldWalkKey(currentKey, normalizedPrefix)) {
                return
            }
            for (value in current.inner) {
                when (value) {
                    is ToggleableValueGroup -> {
                        yield(value)
                        walk(value)
                    }
                    is ModeValueGroup<*> -> {
                        yield(value)
                        value.modes.forEach { walk(it) }
                    }
                    is ValueGroup -> walk(value)
                    else -> yield(value)
                }
            }
        }

        walk(this@ValueGroup)
    }

    fun collectValueGroupsRecursively(prefix: String): Sequence<ValueGroup> = sequence {
        val normalizedPrefix = prefix.lowercase()

        suspend fun SequenceScope<ValueGroup>.walk(current: ValueGroup) {
            val currentKey = current.key?.lowercase()
            if (!shouldWalkKey(currentKey, normalizedPrefix)) {
                return
            }
            yield(current)
            for (value in current.inner) {
                when (value) {
                    is ModeValueGroup<*> -> {
                        walk(value)
                        value.modes.forEach { walk(it) }
                    }
                    is ValueGroup -> walk(value)
                }
            }
        }

        walk(this@ValueGroup)
    }

    private fun shouldWalkKey(currentKey: String?, prefix: String): Boolean {
        if (prefix.isBlank()) {
            return true
        }
        if (currentKey == null) {
            return false
        }
        return currentKey.startsWith(prefix) || prefix.startsWith(currentKey)
    }

    /**
     * Restore all values to their default values
     */
    override fun restore() {
        inner.forEach(Value<*>::restore)
    }

    // Common value types

    fun <T : ValueGroup> tree(valueGroup: T): T {
        require(valueGroup !is Config) {
            "ValueGroup '${valueGroup.name}' is a Config and cannot be added to another ValueGroup."
        }

        if (valueGroup.base != null) {
            logger.warn("ValueGroup '${valueGroup.name}' is already added to a parent '${valueGroup.base?.name}'")
        }

        inner.add(valueGroup)
        valueGroup.base = this
        return valueGroup
    }

    fun <T : ValueGroup> treeAll(vararg valueGroups: T) {
        valueGroups.forEach(this::tree)
    }

    fun <T : ValueGroup> drop(valueGroup: T): T {
        require(valueGroup.base === this) {
            "ValueGroup '${valueGroup.name}' is not a child of '${this.name}'."
        }

        inner.remove(valueGroup)
        valueGroup.base = null
        return valueGroup
    }

    fun <T : Any> value(
        name: String,
        defaultValue: T,
        valueType: ValueType = ValueType.INVALID,
        aliases: List<String> = emptyList(),
    ) = Value(name, aliases = aliases, defaultValue = defaultValue, valueType = valueType).apply {
        this@ValueGroup.inner.add(this)
    }

    internal inline fun <T : MutableCollection<E>, reified E> list(
        name: String,
        defaultValue: T,
        valueType: ValueType,
    ) = ListValue(name, defaultValue, innerValueType = valueType, innerType = E::class.java).apply {
        this@ValueGroup.inner.add(this)
    }

    internal inline fun <T : MutableCollection<E>, reified E> mutableList(
        name: String,
        defaultValue: T,
        valueType: ValueType,
    ) = MutableListValue(name, defaultValue, valueType, E::class.java).apply {
        this@ValueGroup.inner.add(this)
    }

    internal inline fun <T : MutableSet<E>, reified E> itemList(
        name: String,
        defaultValue: T,
        items: Set<ItemListValue.NamedItem<E>>,
        valueType: ValueType,
    ) = ItemListValue(name, defaultValue, items, valueType, E::class.java).apply {
        this@ValueGroup.inner.add(this)
    }

    internal inline fun <T : SequencedSet<E>, reified E> registryList(
        name: String,
        defaultValue: T,
        valueType: ValueType,
    ) = RegistryListValue(name, defaultValue, valueType, E::class.java).apply {
        this@ValueGroup.inner.add(this)
    }

    private fun <T : Any> rangedValue(
        name: String,
        defaultValue: T,
        range: ClosedRange<*>,
        suffix: String,
        valueType: ValueType,
        aliases: List<String> = emptyList(),
    ) = RangedValue(
        name,
        aliases = aliases,
        defaultValue = defaultValue,
        range = range,
        suffix = suffix,
        valueType = valueType,
    ).apply {
        this@ValueGroup.inner.add(this)
    }

    // Fixed data types

    fun boolean(
        name: String,
        default: Boolean,
        aliases: List<String> = emptyList(),
    ) = value(name, default, ValueType.BOOLEAN, aliases)

    fun float(
        name: String,
        default: Float,
        range: ClosedFloatingPointRange<Float>,
        suffix: String = "",
        aliases: List<String> = emptyList(),
    ) = rangedValue(name, default, range, suffix, ValueType.FLOAT, aliases)

    fun floatRange(
        name: String,
        default: ClosedFloatingPointRange<Float>,
        range: ClosedFloatingPointRange<Float>,
        suffix: String = "",
        aliases: List<String> = emptyList(),
    ) = rangedValue(name, default, range, suffix, ValueType.FLOAT_RANGE, aliases)

    fun int(
        name: String,
        default: Int,
        range: IntRange,
        suffix: String = "",
        aliases: List<String> = emptyList(),
    ) = rangedValue(name, default, range, suffix, ValueType.INT, aliases)

    fun intRange(
        name: String,
        default: IntRange,
        range: IntRange,
        suffix: String = "",
        aliases: List<String> = emptyList(),
    ) = rangedValue(name, default, range, suffix, ValueType.INT_RANGE, aliases)

    fun bind(name: String, default: Int = GLFW.GLFW_KEY_UNKNOWN) = bind(
        name,
        InputBind(InputConstants.Type.KEYSYM, default, InputBind.BindAction.TOGGLE)
    )

    fun bind(name: String, default: InputBind) = BindValue(name, defaultValue = default).apply {
        this@ValueGroup.inner.add(this)
    }

    fun key(name: String, default: Int) = key(name, InputConstants.Type.KEYSYM.getOrCreate(default))

    fun key(name: String, default: InputConstants.Key = InputConstants.UNKNOWN) =
        value(name, default, ValueType.KEY)

    fun text(name: String, default: String) = value(name, default, ValueType.TEXT)

    fun regex(name: String, default: Regex) = value(name, default, ValueType.TEXT)

    fun <C : MutableCollection<String>> textList(name: String, default: C) =
        mutableList<C, String>(name, default, ValueType.TEXT)

    fun <C : MutableCollection<Regex>> regexList(name: String, default: C) =
        mutableList<C, Regex>(name, default, ValueType.TEXT)

    fun easing(name: String, default: Easing) = enumChoice(name, default)

    fun color(name: String, default: Color4b) = value(name, default, ValueType.COLOR)

    fun block(name: String, default: Block) = value(name, default, ValueType.BLOCK)

    fun vec2f(name: String, default: Vector2fc) = value(name, default, ValueType.VECTOR2_F)

    @JvmOverloads
    fun vec3i(
        name: String,
        default: Vec3i = Vec3i.ZERO,
        useLocateButton: Boolean = true,
        aliases: List<String> = emptyList(),
    ): Value<Vec3i> = Vec3Value(name, aliases, default, useLocateButton, ValueType.VECTOR3_I).also(inner::add)

    @JvmOverloads
    fun vec3d(
        name: String,
        default: Vec3 = Vec3.ZERO,
        useLocateButton: Boolean = true,
        aliases: List<String> = emptyList(),
    ): Value<Vec3> = Vec3Value(name, aliases, default, useLocateButton, ValueType.VECTOR3_D).also(inner::add)

    fun <C : SequencedSet<Block>> blocks(name: String, default: C) =
        registryList(name, default, ValueType.BLOCK)

    fun item(name: String, default: Item) = value(name, default, ValueType.ITEM)

    fun <C : SequencedSet<Item>> items(name: String, default: C) =
        registryList(name, default, ValueType.ITEM)

    fun <C : SequencedSet<SoundEvent>> sounds(name: String, default: C) =
        registryList(name, default, ValueType.SOUND_EVENT)

    fun <C : SequencedSet<MobEffect>> mobEffects(name: String, default: C) =
        registryList(name, default, ValueType.MOB_EFFECT)

    fun <C : SequencedSet<Identifier>> c2sPackets(name: String, default: C) =
        registryList(name, default, ValueType.C2S_PACKET)

    fun <C : SequencedSet<Identifier>> s2cPackets(name: String, default: C) =
        registryList(name, default, ValueType.S2C_PACKET)

    fun <C : SequencedSet<EntityType<*>>> entityTypes(name: String, default: C) =
        registryList(name, default, ValueType.ENTITY_TYPE)

    @Suppress("LongParameterList")
    fun curve(
        name: String,
        default: MutableList<Vector2f>,
        xAxis: Axis,
        yAxis: Axis,
        tension: Float = CurveValue.DEFAULT_TENSION,
    ) = CurveValue(name, default, xAxis, yAxis, tension).apply {
        this@ValueGroup.inner.add(this)
    }

    inline fun curve(name: String, block: CurveValue.Builder.() -> Unit): CurveValue {
        val builder = CurveValue.Builder()
        builder.name = name
        return builder.apply(block).build().also(::value)
    }

    fun file(
        name: String,
        default: File? = null,
        dialogMode: FileDialogMode = FileDialogMode.OPEN_FILE,
        supportedExtensions: Set<String>? = null,
    ) = FileValue(name, default, dialogMode, supportedExtensions).apply {
        this@ValueGroup.inner.add(this)
    }

    inline fun <reified T> multiEnumChoice(
        name: String,
        vararg default: T,
        canBeNone: Boolean = true,
    ) where T : Enum<T>, T : Tagged =
        multiEnumChoice(name, default.toEnumSet(), canBeNone = canBeNone)

    inline fun <reified T> multiEnumChoice(
        name: String,
        default: Iterable<T>,
        canBeNone: Boolean = true,
    ) where T : Enum<T>, T : Tagged =
        multiEnumChoice(name, default.toEnumSet(), canBeNone = canBeNone)

    inline fun <reified T> multiEnumChoice(
        name: String,
        default: EnumSet<T> = enumSetOf(),
        choices: EnumSet<T> = EnumSet.allOf(T::class.java),
        canBeNone: Boolean = true,
    ) where T : Enum<T>, T : Tagged =
        multiEnumChoice(name, default, choices, canBeNone, isOrderSensitive = false)

    inline fun <reified T> multiEnumChoice(
        name: String,
        default: SequencedSet<T>,
        choices: EnumSet<T> = EnumSet.allOf(T::class.java),
        canBeNone: Boolean = true,
    ) where T : Enum<T>, T : Tagged =
        multiEnumChoice(name, default, choices, canBeNone, isOrderSensitive = true)

    fun <T : Tagged> multiEnumChoice(
        name: String,
        default: MutableSet<T>,
        choices: Set<T>,
        canBeNone: Boolean,
        isOrderSensitive: Boolean,
    ) = MultiChoiceListValue(name, default, choices, canBeNone, isOrderSensitive).apply {
        this@ValueGroup.inner.add(this)
    }

    inline fun <reified T> enumChoice(name: String, default: T): ChoiceListValue<T>
        where T : Enum<T>, T : Tagged = enumChoice(name, default, EnumSet.allOf(T::class.java))

    fun <T : Tagged> enumChoice(name: String, default: T, choices: Set<T>): ChoiceListValue<T> =
        ChoiceListValue(name, defaultValue = default, choices = choices).apply { this@ValueGroup.inner.add(this) }

    protected fun <T : Mode> modes(
        eventListener: EventListener,
        name: String,
        active: T,
        modes: Array<T>,
    ): ModeValueGroup<T> {
        return modes(eventListener, name, {
            val idx = modes.indexOf(active)

            check(idx != -1) { "The active choice $active is not contained within the choice array ($it)" }

            idx
        }) { modes }
    }

    protected fun <T : Mode> modes(
        eventListener: EventListener,
        name: String,
        activeCallback: ToIntFunction<List<T>>,
        modesCallback: (ModeValueGroup<T>) -> Array<T>,
    ): ModeValueGroup<T> {
        return ModeValueGroup(eventListener, name, activeCallback, modesCallback).apply {
            this@ValueGroup.inner.add(this)
            this.base = this@ValueGroup
        }
    }

    protected fun <T : Mode> modes(
        eventListener: EventListener,
        name: String,
        activeIndex: Int = 0,
        choicesCallback: (ModeValueGroup<T>) -> Array<T>,
    ) = modes(eventListener, name, { activeIndex }, choicesCallback)

    fun <V : Value<*>> value(value: V) = value.apply { this@ValueGroup.inner.add(this) }

    /**
     * Assigns the value of the settings to the component
     *
     * A component can have dynamic settings which can be assigned through the JSON file
     * These have to be interpreted and assigned to the value group
     *
     * An example:
     * {
     *     "type": "INT",
     *     "name": "Size",
     *     "value": 14,
     *     "range": {
     *         "min": 1,
     *         "max": 100
     *     },
     *     "suffix": "px"
     * }
     *
     * TODO: Replace with proper deserialization
     *
     * @param valueObject JsonObject
     */
    @Suppress("LongMethod")
    fun json(valueObject: JsonObject) {
        val type = enumValueOf<ValueType>(valueObject["type"].asString)
        val name = valueObject["name"].asString

        // todo: replace this with serious deserialization
        when (type) {
            ValueType.BOOLEAN -> {
                val value = valueObject["value"].asBoolean
                boolean(name, value)
            }

            ValueType.INT -> {
                val value = valueObject["value"].asInt
                val min = valueObject["range"].asJsonObject["min"].asInt
                val max = valueObject["range"].asJsonObject["max"].asInt
                val suffix = valueObject["suffix"]?.asString ?: ""
                int(name, value, min..max, suffix)
            }

            ValueType.INT_RANGE -> {
                val valueMin = valueObject["value"].asJsonObject["min"].asInt
                val valueMax = valueObject["value"].asJsonObject["max"].asInt
                val min = valueObject["range"].asJsonObject["min"].asInt
                val max = valueObject["range"].asJsonObject["max"].asInt
                val suffix = valueObject["suffix"]?.asString ?: ""
                intRange(name, valueMin..valueMax, min..max, suffix)
            }

            ValueType.FLOAT -> {
                val value = valueObject["value"].asFloat
                val min = valueObject["range"].asJsonObject["min"].asFloat
                val max = valueObject["range"].asJsonObject["max"].asFloat
                val suffix = valueObject["suffix"]?.asString ?: ""
                float(name, value, min..max, suffix)
            }

            ValueType.FLOAT_RANGE -> {
                val valueMin = valueObject["value"].asJsonObject["min"].asFloat
                val valueMax = valueObject["value"].asJsonObject["max"].asFloat
                val min = valueObject["range"].asJsonObject["min"].asFloat
                val max = valueObject["range"].asJsonObject["max"].asFloat
                val suffix = valueObject["suffix"]?.asString ?: ""
                floatRange(name, valueMin..valueMax, min..max, suffix)
            }

            ValueType.TEXT -> {
                val value = valueObject["value"].asString
                text(name, value)
            }

            ValueType.COLOR -> {
                val value = valueObject["value"].asInt
                color(name, Color4b(value))
            }

            ValueType.CONFIGURABLE -> {
                val subValueGroup = ValueGroup(name)
                val values = valueObject["values"].asJsonArray
                for (value in values) {
                    subValueGroup.json(value.asJsonObject)
                }
                tree(subValueGroup)
            }
            // same as value group but it is [ToggleableValueGroup]
            ValueType.TOGGLEABLE -> {
                val value = valueObject["value"].asBoolean
                // Parent is NULL in that case because we are not dealing with Listenable anyway and only use it
                // as toggleable ValueGroup
                val subValueGroup = object : ToggleableValueGroup(null, name, value) {}
                val settings = valueObject["values"].asJsonArray
                for (setting in settings) {
                    subValueGroup.json(setting.asJsonObject)
                }
                tree(subValueGroup)
            }

            ValueType.CHOOSE -> {
                val value = valueObject["value"].asString.asTagged()
                val choices = valueObject["choices"].asJsonArray.mapTo(linkedSetOf()) { it.asString.asTagged() }

                enumChoice(name, value, choices)
            }

            ValueType.MULTI_CHOOSE -> {
                fun parseBoolean(key: String, default: Boolean) = when (val json = valueObject[key]) {
                    null, is JsonNull -> default
                    is JsonPrimitive, is JsonArray -> json.asBoolean
                    else -> error("Unexpected JSON value (${json.javaClass}): $json, should be boolean")
                }

                val canBeNone = parseBoolean(key = "canBeNone", default = true)
                val isOrderSensitive = parseBoolean(key = "isOrderSensitive", default = false)

                val value = valueObject["value"].asJsonArray.mapTo(
                    if (isOrderSensitive) sortedSetOf() else linkedSetOf()
                ) { it.asString.asTagged() }
                val choices = valueObject["choices"].asJsonArray.mapTo(linkedSetOf()) { it.asString.asTagged() }

                multiEnumChoice(name, default = value, choices, canBeNone, isOrderSensitive)
            }

            else -> error("Unsupported type: $type")
        }
    }


}

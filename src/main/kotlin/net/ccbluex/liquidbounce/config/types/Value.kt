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

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import net.ccbluex.liquidbounce.authlib.account.MinecraftAccount
import net.ccbluex.liquidbounce.config.gson.stategies.Exclude
import net.ccbluex.liquidbounce.config.gson.stategies.ProtocolExclude
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.ValueChangedEvent
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.script.ScriptApiRequired
import net.ccbluex.liquidbounce.utils.client.convertToString
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.toLowerCamelCase
import net.ccbluex.liquidbounce.utils.input.HumanInputDeserializer
import net.ccbluex.liquidbounce.utils.input.InputBind
import net.ccbluex.liquidbounce.utils.input.inputByName
import net.ccbluex.liquidbounce.utils.kotlin.mapArray
import net.minecraft.client.util.InputUtil
import java.util.*
import java.util.function.Supplier
import kotlin.reflect.KProperty

typealias ValueListener<T> = (T) -> T

typealias ValueChangedListener<T> = (T) -> Unit

/**
 * Value based on generics and support for readable names and descriptions.
 */
@Suppress("TooManyFunctions")
open class Value<T : Any>(
    @SerializedName("name") open val name: String,
    @Exclude val aliases: Array<out String> = emptyArray(),
    @Exclude private var defaultValue: T,
    @Exclude val valueType: ValueType,
    @Exclude @ProtocolExclude val listType: ListValueType = ListValueType.None,

    /**
     * If true, the description won't be bound to any [Configurable].
     */
    @Exclude @ProtocolExclude var independentDescription: Boolean = false
) {

    @SerializedName("value")
    internal var inner: T = defaultValue

    internal val loweredName
        get() = name.lowercase()

    @Exclude
    @ProtocolExclude
    private val listeners = mutableListOf<ValueListener<T>>()

    @Exclude
    @ProtocolExclude
    private val changedListeners = mutableListOf<ValueChangedListener<T>>()

    /**
     * If true, value will not be included in generated public config
     *
     * @see
     */
    @Exclude
    @ProtocolExclude
    var doNotInclude = { false }
        private set

    /**
     * If true, value will not be included in generated RestAPI config
     */
    @Exclude
    @ProtocolExclude
    var notAnOption = false
        private set

    @Exclude
    var key: String? = null
        set(value) {
            field = value

            this.descriptionKey = value?.let {
                if (independentDescription) {
                    "liquidbounce.common.value.${name.toLowerCamelCase()}.description"
                } else {
                    this.key?.let { s -> "$s.description" }
                }
            }
        }

    @Exclude
    @ProtocolExclude
    var descriptionKey: String? = null

    @Exclude
    open var description = Supplier {
        descriptionKey?.let { key -> translation(key).convertToString() }
    }

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

    operator fun getValue(u: Any?, property: KProperty<*>) = get()

    operator fun setValue(u: Any?, property: KProperty<*>, t: T) {
        set(t)
    }

    @ScriptApiRequired
    @JvmName("getValue")
    fun getValue(): Any {
        if (this is ChoiceConfigurable<*>) {
            return this.activeChoice.name
        }

        return when (val v = get()) {
            is ClosedFloatingPointRange<*> -> arrayOf(v.start, v.endInclusive)
            is IntRange -> arrayOf(v.first, v.last)
            is NamedChoice -> v.choiceName
            else -> v
        }
    }

    @ScriptApiRequired
    @JvmName("setValue")
    @Suppress("UNCHECKED_CAST")
    fun setValue(t: org.graalvm.polyglot.Value) = runCatching {
        if (this is ChooseListValue<*>) {
            setByString(t.asString())
            return@runCatching
        }

        set(
            when (inner) {
                is ClosedFloatingPointRange<*> -> {
                    val a = t.`as`(Array<Double>::class.java)
                    require(a.size == 2)
                    (a.first().toFloat()..a.last().toFloat()) as T
                }

                is InputUtil.Key -> {
                    inputByName(t.asString()) as T
                }

                is IntRange -> {
                    val a = t.`as`(Array<Int>::class.java)
                    require(a.size == 2)
                    (a.first()..a.last()) as T
                }

                is Float -> t.`as`(Double::class.java).toFloat() as T
                is Int -> t.`as`(Int::class.java) as T
                is String -> t.`as`(String::class.java) as T
                is MutableList<*> -> t.`as`(Array<String>::class.java).toMutableList() as T
                is Boolean -> t.`as`(Boolean::class.java) as T
                else -> error("Unsupported value type $inner")
            }
        )
    }.onFailure {
        logger.error("Could not set value, old value: ${this.inner}, throwable: $it")
    }

    fun get() = inner

    fun set(t: T) {
        // Do nothing if value is the same
        if (t == inner) {
            return
        }

        set(t) { inner = it }
    }

    fun set(t: T, apply: (T) -> Unit) {
        var currT = t
        runCatching {
            listeners.forEach {
                currT = it(t)
            }
        }.onSuccess {
            apply(currT)
            EventManager.callEvent(ValueChangedEvent(this))
            changedListeners.forEach { it(currT) }
        }.onFailure { ex ->
            logger.error("Failed to set ${this.name} from ${this.inner} to $t", ex)
        }
    }

    /**
     * Restore value to default value
     */
    open fun restore() {
        set(defaultValue)
    }

    fun type() = valueType

    fun onChange(listener: ValueListener<T>): Value<T> {
        listeners += listener
        return this
    }

    fun onChanged(listener: ValueChangedListener<T>): Value<T> {
        changedListeners += listener
        return this
    }

    fun doNotIncludeAlways(): Value<T> {
        doNotInclude = { true }
        return this
    }

    fun doNotIncludeWhen(condition: () -> Boolean): Value<T> {
        doNotInclude = condition
        return this
    }

    fun notAnOption(): Value<T> {
        notAnOption = true
        return this
    }

    fun independentDescription(): Value<T> {
        independentDescription = true
        return this
    }

    /**
     * Deserialize value from JSON
     */
    open fun deserializeFrom(gson: Gson, element: JsonElement) {
        val currValue = this.inner

        set(
            when (currValue) {
                is List<*> -> {
                    @Suppress("UNCHECKED_CAST") element.asJsonArray.mapTo(
                        mutableListOf()
                    ) { gson.fromJson(it, this.listType.type!!) } as T
                }

                is HashSet<*> -> {
                    @Suppress("UNCHECKED_CAST") element.asJsonArray.mapTo(
                        HashSet()
                    ) { gson.fromJson(it, this.listType.type!!) } as T
                }

                is Set<*> -> {
                    @Suppress("UNCHECKED_CAST") element.asJsonArray.mapTo(
                        TreeSet()
                    ) { gson.fromJson(it, this.listType.type!!) } as T
                }

                else -> {
                    var clazz: Class<*>? = currValue.javaClass
                    var r: T? = null

                    while (clazz != null && clazz != Any::class.java) {
                        try {
                            r = gson.fromJson(element, clazz) as T?
                            break
                        } catch (@Suppress("SwallowedException") e: ClassCastException) {
                            clazz = clazz.superclass
                        }
                    }

                    r ?: error("Failed to deserialize value")
                }
            })
    }

    open fun setByString(string: String) {
        val deserializer = this.valueType.deserializer

        requireNotNull(deserializer) { "Cannot deserialize values of type ${this.valueType} yet." }

        set(deserializer.deserializeThrowing(string) as T)
    }

}

/**
 * Ranged value adds support for closed ranges
 */
class RangedValue<T : Any>(
    name: String,
    aliases: Array<String> = emptyArray(),
    defaultValue: T,
    @Exclude val range: ClosedRange<*>,
    @Exclude val suffix: String,
    valueType: ValueType
) : Value<T>(name, aliases, defaultValue, valueType) {

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

class BindValue(
    name: String,
    aliases: Array<String> = emptyArray(),
    defaultValue: InputBind,
) : Value<InputBind>(name, aliases, defaultValue, ValueType.BIND) {
    override fun setByString(string: String) {
        get().bind(string)
    }
}

class ChooseListValue<T : NamedChoice>(
    name: String,
    aliases: Array<String> = emptyArray(),
    defaultValue: T,
    @Exclude val choices: Array<T>
) : Value<T>(name, aliases, defaultValue, ValueType.CHOOSE) {

    override fun deserializeFrom(gson: Gson, element: JsonElement) {
        val name = element.asString

        setByString(name)
    }

    override fun setByString(string: String) {
        val newValue = choices.firstOrNull { it.choiceName == string }

        if (newValue == null) {
            throw IllegalArgumentException(
                "ChooseListValue `${this.name}` has no option named $string" +
                    " (available options are ${this.choices.joinToString { it.choiceName }})"
            )
        }

        set(newValue)
    }

    @ScriptApiRequired
    fun getChoicesStrings(): Array<String> {
        return this.choices.mapArray { it.choiceName }
    }

}

interface NamedChoice {
    val choiceName: String
}

enum class ValueType(
    val deserializer: HumanInputDeserializer.StringDeserializer<*>? = null,
    val completer: AutoCompletionProvider.CompletionHandler = AutoCompletionProvider.defaultCompleter
) {
    BOOLEAN(HumanInputDeserializer.booleanDeserializer, AutoCompletionProvider.booleanCompleter),
    FLOAT(HumanInputDeserializer.floatDeserializer), FLOAT_RANGE(HumanInputDeserializer.floatRangeDeserializer),
    INT(HumanInputDeserializer.intDeserializer), INT_RANGE(HumanInputDeserializer.intRangeDeserializer),
    TEXT(HumanInputDeserializer.textDeserializer), TEXT_ARRAY(HumanInputDeserializer.textArrayDeserializer),
    COLOR(HumanInputDeserializer.colorDeserializer),
    BLOCK(HumanInputDeserializer.blockDeserializer), BLOCKS(HumanInputDeserializer.blockListDeserializer),
    ITEM(HumanInputDeserializer.itemDeserializer), ITEMS(HumanInputDeserializer.itemListDeserializer),
    KEY(HumanInputDeserializer.keyDeserializer),
    BIND,
    VECTOR_I,
    VECTOR_D,
    CHOICE(completer = AutoCompletionProvider.choiceCompleter),
    CHOOSE(completer = AutoCompletionProvider.chooseCompleter),
    INVALID,
    PROXY,
    CONFIGURABLE,
    TOGGLEABLE,
    ALIGNMENT,
    WALLPAPER,
}

enum class ListValueType(val type: Class<*>?) {
    Block(net.minecraft.block.Block::class.java),
    Item(net.minecraft.item.Item::class.java),
    String(kotlin.String::class.java),
    Friend(FriendManager.Friend::class.java),
    Proxy(net.ccbluex.liquidbounce.features.misc.proxy.Proxy::class.java),
    Account(MinecraftAccount::class.java),
    None(null)
}

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

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import com.mojang.brigadier.StringReader
import net.ccbluex.liquidbounce.authlib.account.MinecraftAccount
import net.ccbluex.liquidbounce.config.util.Exclude
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.ValueChangedEvent
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.misc.ProxyManager
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.script.ScriptApi
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.input.inputByName
import net.ccbluex.liquidbounce.utils.inventory.findBlocksEndingWith
import net.ccbluex.liquidbounce.web.socket.protocol.ProtocolExclude
import net.minecraft.client.util.InputUtil
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import java.awt.Color
import java.util.*
import kotlin.reflect.KProperty

typealias ValueListener<T> = (T) -> T

typealias ValueChangedListener<T> = (T) -> Unit

/**
 * Value based on generics and support for readable names and description
 */
@Suppress("TooManyFunctions")
open class Value<T : Any>(
    @SerializedName("name") open val name: String,
    @Exclude private var defaultValue: T,
    @Exclude val valueType: ValueType,
    @Exclude @ProtocolExclude val listType: ListValueType = ListValueType.None
) {

    @SerializedName("value") internal var inner: T = defaultValue

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

    @ScriptApi
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

    @ScriptApi
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
        logger.error("Could not set value ${this.inner}")
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

    /**
     * Deserialize value from JSON
     */
    open fun deserializeFrom(gson: Gson, element: JsonElement) {
        val currValue = this.inner

        set(when (currValue) {
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
                gson.fromJson(element, currValue.javaClass)
            }
        })
    }

    open fun setByString(string: String) {
        when (this.valueType) {
            ValueType.BOOLEAN -> {
                val newValue = when (string.lowercase(Locale.ROOT)) {
                    "true", "on" -> true
                    "false", "off" -> false
                    else -> throw IllegalArgumentException()
                }

                set(newValue as T)
            }

            ValueType.FLOAT -> {
                val newValue = string.toFloat()

                set(newValue as T)
            }

            ValueType.FLOAT_RANGE -> {
                val split = string.split("..")
                require(split.size == 2)
                val newValue = split[0].toFloat()..split[1].toFloat()

                set(newValue as T)
            }

            ValueType.INT -> {
                val newValue = string.toInt()

                set(newValue as T)
            }

            ValueType.INT_RANGE -> {
                val split = string.split("..")
                require(split.size == 2)
                val newValue = split[0].toInt()..split[1].toInt()

                set(newValue as T)
            }

            ValueType.TEXT -> {
                set(string as T)
            }

            ValueType.TEXT_ARRAY -> {
                val newValue = string.split(",").toMutableList()
                set(newValue as T)
            }

            ValueType.COLOR -> {
                if (string.startsWith("#")) {
                    set(Color4b.fromHex(string) as T)
                } else {
                    set(Color4b(Color(string.toInt())) as T)
                }
            }

            ValueType.BLOCK -> {
                set(Registries.BLOCK.get(Identifier.fromCommandInput(StringReader(string))) as T)
            }

            ValueType.BLOCKS -> {
                val blocks = string.split(",").map {
                    findBlocksEndingWith(it).filter {
                        !it.defaultState.isAir
                    }
                }.flatten().toHashSet()

                if (blocks.isEmpty()) {
                    error("No blocks found")
                }

                set(blocks as T)
            }

            ValueType.ITEM -> {
                set(Registries.ITEM.get(Identifier.fromCommandInput(StringReader(string))) as T)
            }

            ValueType.ITEMS -> {
                val items = string.split(",").map {
                    Registries.ITEM.get(Identifier.fromCommandInput(StringReader(it)))
                }.toMutableList()

                if (items.isEmpty()) {
                    error("No items found")
                }

                set(items as T)
            }

            ValueType.BIND -> {
                val newValue = try {
                    InputUtil.Type.KEYSYM.createFromCode(string.toInt())
                } catch (e: NumberFormatException) {
                    inputByName(string)
                }

                set(newValue as T)
            }

            else -> error("unsupported value type")
        }
    }

}

/**
 * Ranged value adds support for closed ranges
 */
class RangedValue<T : Any>(
    name: String,
    value: T,
    @Exclude val range: ClosedRange<*>,
    @Exclude val suffix: String,
    type: ValueType
) : Value<T>(name, value, valueType = type) {

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

class ChooseListValue<T : NamedChoice>(
    name: String, value: T, @Exclude val choices: Array<T>
) : Value<T>(name, value, ValueType.CHOOSE) {

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

    @ScriptApi
    fun getChoicesStrings(): Array<String> {
        return this.choices.map { it.choiceName }.toTypedArray()
    }

}

interface NamedChoice {
    val choiceName: String
}

enum class ValueType {
    BOOLEAN,
    FLOAT, FLOAT_RANGE,
    INT, INT_RANGE,
    TEXT, TEXT_ARRAY,
    COLOR,
    BLOCK, BLOCKS,
    ITEM, ITEMS,
    KEY,
    BIND,
    CHOICE, CHOOSE,
    INVALID,
    PROXY,
    CONFIGURABLE,
    TOGGLEABLE
}

enum class ListValueType(val type: Class<*>?) {
    Block(net.minecraft.block.Block::class.java),
    Item(net.minecraft.item.Item::class.java),
    String(kotlin.String::class.java),
    Friend(FriendManager.Friend::class.java),
    Proxy(ProxyManager.Proxy::class.java),
    Account(MinecraftAccount::class.java),
    None(null)
}

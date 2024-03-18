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
package net.ccbluex.liquidbounce.script.bindings.features

import net.ccbluex.liquidbounce.config.*
import org.graalvm.polyglot.Value as PolyglotValue;

/**
 * Object used by the script API to provide an idiomatic way of creating module values.
 */
object JsSetting {

    @JvmName("boolean")
    fun boolean(value: PolyglotValue): Value<Boolean> {
        val name = value.getMember("name").asString()
        val default = value.getMember("default").asBoolean()

        return value(name, default, ValueType.BOOLEAN)
    }

    @JvmName("float")
    fun float(value: PolyglotValue): RangedValue<Float> {
        val name = value.getMember("name").asString()
        val default = value.getMember("default").asFloat()
        val range = value.getMember("range").`as`(Array<Double>::class.java)
        val suffix = value.getMember("suffix")?.asString() ?: ""

        require(range.size == 2)
        return rangedValue(
            name,
            default,
            range.first().toFloat()..range.last().toFloat(),
            suffix,
            ValueType.FLOAT
        )
    }

    @JvmName("floatRange")
    fun floatRange(value: PolyglotValue): RangedValue<ClosedFloatingPointRange<Float>> {
        val name = value.getMember("name").asString()
        val default = value.getMember("default").`as`(Array<Double>::class.java)
        val range = value.getMember("range").`as`(Array<Double>::class.java)
        val suffix = value.getMember("suffix")?.asString() ?: ""

        require(default.size == 2)
        require(range.size == 2)
        return rangedValue(
            name,
            default.first().toFloat()..default.last().toFloat(),
            range.first().toFloat()..range.last().toFloat(),
            suffix,
            ValueType.FLOAT_RANGE
        )
    }

    @JvmName("int")
    fun int(value: PolyglotValue): RangedValue<Int> {
        val name = value.getMember("name").asString()
        val default = value.getMember("default").asInt()
        val range = value.getMember("range").`as`(Array<Int>::class.java)
        val suffix = value.getMember("suffix")?.asString() ?: ""

        require(range.size == 2)
        return rangedValue(name, default, range.first()..range.last(), suffix, ValueType.INT)
    }

    @JvmName("intRange")
    fun intRange(value: PolyglotValue): RangedValue<IntRange> {
        val name = value.getMember("name").asString()
        val default = value.getMember("default").`as`(Array<Int>::class.java)
        val range = value.getMember("range").`as`(Array<Int>::class.java)
        val suffix = value.getMember("suffix")?.asString() ?: ""

        require(default.size == 2)
        require(range.size == 2)
        return rangedValue(
            name,
            default.first()..default.last(),
            range.first()..range.last(),
            suffix,
            ValueType.INT_RANGE
        )
    }

    @JvmName("key")
    fun key(value: PolyglotValue): Value<Int> {
        val name = value.getMember("name").asString()
        val default = value.getMember("default").asInt()

        return value(name, default, ValueType.KEY)
    }

    @JvmName("text")
    fun text(value: PolyglotValue): Value<String> {
        val name = value.getMember("name").asString()
        val default = value.getMember("default").asString()

        return value(name, default, ValueType.TEXT)
    }

    @JvmName("textArray")
    fun textArray(value: PolyglotValue): Value<MutableList<String>> {
        val name = value.getMember("name").asString()
        val default = value.getMember("default").`as`(Array<String>::class.java)

        return value(name, default.toMutableList(), ValueType.TEXT_ARRAY, ListValueType.String)
    }

    @JvmName("choose")
    fun choose(value: PolyglotValue): ChooseListValue<NamedChoice> {
        val name = value.getMember("name").asString()
        val choices = value.getMember("choices").`as`(Array<String>::class.java).map {
            object : NamedChoice {
                override val choiceName = it
            }
        }.toTypedArray<NamedChoice>()
        val defaultStr = value.getMember("default").asString()

        val default = choices.find { it.choiceName == defaultStr }
            ?: error(
                "[ScriptAPI] Choose default value '${defaultStr}' is not part of choices '${
                    choices.joinToString(", ") { it.choiceName }
                }'"
            )

        return ChooseListValue(name, default, choices)
    }


    private fun <T : Any> value(
        name: String,
        default: T,
        valueType: ValueType = ValueType.INVALID,
        listType: ListValueType = ListValueType.None
    ) = Value(name, default, valueType, listType)

    private fun <T : Any> rangedValue(
        name: String, default: T, range: ClosedRange<*>, suffix: String,
        valueType: ValueType
    ) =
        RangedValue(name, default, range, suffix, valueType)

}

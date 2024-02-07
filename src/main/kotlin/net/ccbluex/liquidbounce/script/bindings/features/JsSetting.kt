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

import net.ccbluex.liquidbounce.config.ListValueType
import net.ccbluex.liquidbounce.config.RangedValue
import net.ccbluex.liquidbounce.config.Value
import net.ccbluex.liquidbounce.config.ValueType

/**
 * Object used by the script API to provide an idiomatic way of creating module values.
 */
object JsSetting {

    @JvmName("boolean")
    fun boolean(name: String, default: Boolean) = value(name, default, ValueType.BOOLEAN)

    @JvmName("float")
    fun float(name: String, default: Double, range: Array<Double>, suffix: String = ""): RangedValue<Float> {
        require(range.size == 2)
        return rangedValue(name, default.toFloat(), range.first().toFloat()..range.last().toFloat(), suffix, ValueType.FLOAT)
    }

    @JvmName("floatRange")
    fun floatRange(
        name: String,
        default: Array<Double>,
        range: Array<Double>,
        suffix: String = ""
    ): RangedValue<ClosedFloatingPointRange<Float>> {
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
    fun int(name: String, default: Int, range: Array<Int>, suffix: String = ""): RangedValue<Int> {
        require(range.size == 2)
        return rangedValue(name, default, range.first()..range.last(), suffix, ValueType.INT)
    }

    @JvmName("intRange")
    fun intRange(
        name: String,
        default: Array<Int>,
        range: Array<Int>,
        suffix: String = ""
    ): RangedValue<IntRange> {
        require(default.size == 2)
        require(range.size == 2)
        return rangedValue(name, default.first()..default.last(), range.first()..range.last(), suffix, ValueType.INT_RANGE)
    }

    @JvmName("key")
    fun key(name: String, default: Int) = value(name, default, ValueType.KEY)

    @JvmName("text")
    fun text(name: String, default: String) = value(name, default, ValueType.TEXT)

    @JvmName("textArray")
    fun textArray(name: String, default: Array<String>) =
        value(name, default.toMutableList(), ValueType.TEXT_ARRAY, ListValueType.String)

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

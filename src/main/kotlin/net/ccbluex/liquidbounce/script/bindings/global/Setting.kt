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
package net.ccbluex.liquidbounce.script.bindings.global

import jdk.nashorn.api.scripting.JSObject
import jdk.nashorn.api.scripting.ScriptUtils
import net.ccbluex.liquidbounce.config.ChooseListValue
import net.ccbluex.liquidbounce.config.RangedValue
import net.ccbluex.liquidbounce.config.Value
import net.minecraft.block.Block

/**
 * Object used by the script API to provide an idiomatic way of creating module values.
 */
object Setting {

    /**
     * Creates a boolean value.
     * @param settingInfo JavaScript object containing information about the value.
     * @return An instance of [Value]
     */
    @JvmStatic
    fun boolean(settingInfo: JSObject): Value<Boolean> {
        val name = settingInfo.getMember("name") as String
        val default = settingInfo.getMember("default") as Boolean

        return Value(name, value = default)
    }

    /**
     * Creates an integer value.
     * @param settingInfo JavaScript object containing information about the value.
     * @return An instance of [RangedValue]
     */
    @JvmStatic
    fun integer(settingInfo: JSObject): RangedValue<Int> {
        val name = settingInfo.getMember("name") as String
        val default = (settingInfo.getMember("default") as Number).toInt()
        val min = (settingInfo.getMember("min") as Number).toInt()
        val max = (settingInfo.getMember("max") as Number).toInt()

        return RangedValue(name, value = default, range = min..max)
    }

    /**
     * Creates a float value.
     * @param settingInfo JavaScript object containing information about the value.
     * @return An instance of [RangedValue]
     */
    @JvmStatic
    fun float(settingInfo: JSObject): RangedValue<Float> {
        val name = settingInfo.getMember("name") as String
        val default = (settingInfo.getMember("default") as Number).toFloat()
        val min = (settingInfo.getMember("min") as Number).toFloat()
        val max = (settingInfo.getMember("max") as Number).toFloat()

        return RangedValue(name, value = default, range = min..max)
    }

    /**
     * Creates a text value.
     * @param settingInfo JavaScript object containing information about the value.
     * @return An instance of [TextValue]
     */
    @JvmStatic
    fun text(settingInfo: JSObject): Value<String> {
        val name = settingInfo.getMember("name") as String
        val default = settingInfo.getMember("default") as String

        return Value(name, value = default)
    }

    /**
     * Creates a block value.
     * @param settingInfo JavaScript object containing information about the value.
     * @return An instance of [Value]
     */
    @JvmStatic
    fun block(settingInfo: JSObject): Value<Block> {
        val name = settingInfo.getMember("name") as String
        val default = settingInfo.getMember("default") as Block

        return Value(name, value = default)
    }

    /**
     * Creates a list value.
     * @param settingInfo JavaScript object containing information about the value.
     * @return An instance of [Value]
     */
    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun list(settingInfo: JSObject): ChooseListValue {
        val name = settingInfo.getMember("name") as String
        val values = ScriptUtils.convert(settingInfo.getMember("values"), Array<String>::class.java) as Array<String>
        val default = settingInfo.getMember("default") as String

        return ChooseListValue(name, selected = default, selectables = values)
    }

}

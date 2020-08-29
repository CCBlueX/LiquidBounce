/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.script.api.global

import jdk.nashorn.api.scripting.JSObject
import jdk.nashorn.api.scripting.ScriptUtils
import net.ccbluex.liquidbounce.value.*

/**
 * Object used by the script API to provide an idiomatic way of creating module values.
 */
object Setting {

    /**
     * Creates a boolean value.
     * @param settingInfo JavaScript object containing information about the value.
     * @return An instance of [BoolValue]
     */
    @JvmStatic
    fun boolean(settingInfo: JSObject): BoolValue {
        val name = settingInfo.getMember("name") as String
        val default = settingInfo.getMember("default") as Boolean

        return BoolValue(name, default)
    }

    /**
     * Creates an integer value.
     * @param settingInfo JavaScript object containing information about the value.
     * @return An instance of [IntegerValue]
     */
    @JvmStatic
    fun integer(settingInfo: JSObject): IntegerValue {
        val name = settingInfo.getMember("name") as String
        val default = (settingInfo.getMember("default") as Number).toInt()
        val min = (settingInfo.getMember("min") as Number).toInt()
        val max = (settingInfo.getMember("max") as Number).toInt()

        return IntegerValue(name, default, min, max)
    }

    /**
     * Creates a float value.
     * @param settingInfo JavaScript object containing information about the value.
     * @return An instance of [FloatValue]
     */
    @JvmStatic
    fun float(settingInfo: JSObject): FloatValue {
        val name = settingInfo.getMember("name") as String
        val default = (settingInfo.getMember("default") as Number).toFloat()
        val min = (settingInfo.getMember("min") as Number).toFloat()
        val max = (settingInfo.getMember("max") as Number).toFloat()

        return FloatValue(name, default, min, max)
    }

    /**
     * Creates a text value.
     * @param settingInfo JavaScript object containing information about the value.
     * @return An instance of [TextValue]
     */
    @JvmStatic
    fun text(settingInfo: JSObject): TextValue {
        val name = settingInfo.getMember("name") as String
        val default = settingInfo.getMember("default") as String

        return TextValue(name, default)
    }

    /**
     * Creates a block value.
     * @param settingInfo JavaScript object containing information about the value.
     * @return An instance of [BlockValue]
     */
    @JvmStatic
    fun block(settingInfo: JSObject): BlockValue {
        val name = settingInfo.getMember("name") as String
        val default = (settingInfo.getMember("default") as Number).toInt()

        return BlockValue(name, default)
    }

    /**
     * Creates a list value.
     * @param settingInfo JavaScript object containing information about the value.
     * @return An instance of [ListValue]
     */
    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun list(settingInfo: JSObject): ListValue {
        val name = settingInfo.getMember("name") as String
        val values = ScriptUtils.convert(settingInfo.getMember("values"), Array<String>::class.java) as Array<String>
        val default = settingInfo.getMember("default") as String

        return ListValue(name, values, default)
    }
}
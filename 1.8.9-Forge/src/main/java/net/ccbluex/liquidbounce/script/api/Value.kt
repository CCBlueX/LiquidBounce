/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.script.api

import net.ccbluex.liquidbounce.script.api.module.AdaptedValue
import net.ccbluex.liquidbounce.value.*
import net.ccbluex.liquidbounce.value.Value

/**
 * A script api class for creating values
 *
 * @author CCBlueX
 */
@Suppress("UNCHECKED_CAST")
object Value {

    /**
     * Creates a new block value
     *
     * @param name Name of the value
     * @param value Default value
     */
    @JvmStatic
    fun createBlock(name : String, value : Int) : AdaptedValue {
        return AdaptedValue(BlockValue(name, value) as Value<Any>)
    }

    /**
     * Creates a new bool value
     *
     * @param name Name of the value
     * @param value Default value
     */
    @JvmStatic
    fun createBoolean(name : String, value : Boolean) : AdaptedValue {
        return AdaptedValue(BoolValue(name, value) as Value<Any>)
    }

    /**
     * Creates a new float value
     *
     * @param name Name of the value
     * @param value Default value
     * @param min Smallest possible value
     * @param max Largest possible value
     */
    @JvmStatic
    fun createFloat(name : String, value : Float, min : Float, max : Float) : AdaptedValue {
        return AdaptedValue(FloatValue(name, value, min, max) as Value<Any>)
    }

    /**
     * Creates a new integer value
     *
     * @param name Name of the value
     * @param value Default value
     * @param min Smallest possible value
     * @param max Largest possible value
     */
    @JvmStatic
    fun createInteger(name : String, value : Int, min : Int, max : Int) : AdaptedValue {
        return AdaptedValue(IntegerValue(name, value, min, max) as Value<Any>)
    }

    /**
     * Creates a new list value
     *
     * @param name Name of the value
     * @param values Array containing all possible values
     * @param value Default value
     */
    @JvmStatic
    fun createList(name : String, values : Array<String>, value : String) : AdaptedValue {
        return AdaptedValue(ListValue(name, values, value) as Value<Any>)
    }

    /**
     * Creates a new text value
     *
     * @param name Name of the value
     * @param value Default value
     */
    @JvmStatic
    fun createText(name : String, value : String) : AdaptedValue {
        return AdaptedValue(TextValue(name, value) as Value<Any>)
    }
}
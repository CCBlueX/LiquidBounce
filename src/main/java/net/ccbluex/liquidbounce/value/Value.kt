/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.value

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import net.ccbluex.liquidbounce.LiquidBounce

import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.minecraft.client.gui.FontRenderer
import java.util.*

abstract class Value<T>(val name: String, protected var value: T) {

    @set:JvmName("setIsSupported")
    var isSupported = true

    fun set(newValue: T) {
        if (newValue == value)
            return

        val oldValue = get()

        try {
            onChange(oldValue, newValue)
            changeValue(newValue)
            onChanged(oldValue, newValue)
            onUpdate(newValue)
            LiquidBounce.fileManager.saveConfig(LiquidBounce.fileManager.valuesConfig)
        } catch (e: Exception) {
            ClientUtils.getLogger().error("[ValueSystem ($name)]: ${e.javaClass.name} (${e.message}) [$oldValue >> $newValue]")
        }
    }

    fun get() = value

    open fun changeValue(value: T) {
        this.value = value
    }

    open fun toJson(): JsonElement? {
        return toJsonF()
    }

    open fun fromJson(element: JsonElement) {
        val result = fromJsonF(element)
        if (result != null) {
            changeValue(result)
        }
        onInit(value)
        onUpdate(value)
    }

    abstract fun toJsonF(): JsonElement?
    abstract fun fromJsonF(element: JsonElement): T?

    protected open fun onInit(value: T) {}
    protected open fun onUpdate(value: T) {}
    protected open fun onChange(oldValue: T, newValue: T) {}
    protected open fun onChanged(oldValue: T, newValue: T) {}

}

/**
 * Bool value represents a value with a boolean
 */
open class BoolValue(name: String, value: Boolean) : Value<Boolean>(name, value) {

    override fun toJsonF() = JsonPrimitive(value)

    override fun fromJsonF(element: JsonElement): Boolean? {
        if (element.isJsonPrimitive)
            return element.asBoolean || element.asString.equals("true", ignoreCase = true)
        return null
    }

}

/**
 * Integer value represents a value with a integer
 */
open class IntegerValue(name: String, value: Int, val minimum: Int = 0, val maximum: Int = Integer.MAX_VALUE)
    : Value<Int>(name, value) {

    fun set(newValue: Number) {
        set(newValue.toInt())
    }

    override fun toJsonF() = JsonPrimitive(value)

    override fun fromJsonF(element: JsonElement): Int? {
        if (element.isJsonPrimitive)
            return element.asInt
        return null
    }

}

/**
 * Float value represents a value with a float
 */
open class FloatValue(name: String, value: Float, val minimum: Float = 0F, val maximum: Float = Float.MAX_VALUE)
    : Value<Float>(name, value) {

    fun set(newValue: Number) {
        set(newValue.toFloat())
    }

    override fun toJsonF() = JsonPrimitive(value)

    override fun fromJsonF(element: JsonElement): Float? {
        if (element.isJsonPrimitive)
            return element.asFloat
        return null
    }

}

/**
 * Text value represents a value with a string
 */
open class TextValue(name: String, value: String) : Value<String>(name, value) {

    override fun toJsonF() = JsonPrimitive(value)

    override fun fromJsonF(element: JsonElement): String? {
        if (element.isJsonPrimitive)
            return element.asString
        return null
    }
}

/**
 * Font value represents a value with a font
 */
class FontValue(valueName: String, value: FontRenderer) : Value<FontRenderer>(valueName, value) {

    override fun toJsonF(): JsonElement? {
        val fontDetails = Fonts.getFontDetails(value) ?: return null
        val valueObject = JsonObject()
        valueObject.addProperty("fontName", fontDetails.name)
        valueObject.addProperty("fontSize", fontDetails.fontSize)
        return valueObject
    }

    override fun fromJsonF(element: JsonElement): FontRenderer? {
        if (element.isJsonObject) {
            val valueObject = element.asJsonObject
            return Fonts.getFontRenderer(valueObject["fontName"].asString, valueObject["fontSize"].asInt)
        }
        return null
    }
}

/**
 * Block value represents a value with a block
 */
class BlockValue(name: String, value: Int) : IntegerValue(name, value, 1, 197)

/**
 * List value represents a selectable list of values
 */
open class ListValue(name: String, val values: Array<String>, value: String) : Value<String>(name, value) {

    @JvmField
    var openList = false

    init {
        this.value = value
    }

    operator fun contains(string: String?): Boolean {
        return Arrays.stream(values).anyMatch { s: String -> s.equals(string, ignoreCase = true) }
    }

    override fun changeValue(value: String) {
        for (element in values) {
            if (element.equals(value, ignoreCase = true)) {
                this.value = element
                break
            }
        }
    }

    override fun toJsonF() = JsonPrimitive(value)

    override fun fromJsonF(element: JsonElement): String? {
        if (element.isJsonPrimitive)
            return element.asString
        return null
    }
}

/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.value

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import net.ccbluex.liquidbounce.file.FileManager.saveConfig
import net.ccbluex.liquidbounce.file.FileManager.valuesConfig
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.ui.font.GameFontRenderer
import net.ccbluex.liquidbounce.utils.ClientUtils.LOGGER
import net.minecraft.client.gui.FontRenderer

abstract class Value<T>(val name: String, protected open var value: T) {

    fun set(newValue: T): Boolean {
        if (newValue == value)
            return false

        val oldValue = value

        try {
            val handledValue = onChange(oldValue, newValue)
            if (handledValue == oldValue) return false

            changeValue(handledValue)
            onChanged(oldValue, handledValue)
            onUpdate(handledValue)

            saveConfig(valuesConfig)
            return true
        } catch (e: Exception) {
            LOGGER.error("[ValueSystem ($name)]: ${e.javaClass.name} (${e.message}) [$oldValue >> $newValue]")
            return false
        }
    }

    fun get() = value

    open fun changeValue(newValue: T) {
        value = newValue
    }

    open fun toJson() = toJsonF()

    open fun fromJson(element: JsonElement) {
        val result = fromJsonF(element)
        if (result != null) changeValue(result)

        onInit(value)
        onUpdate(value)
    }

    abstract fun toJsonF(): JsonElement?
    abstract fun fromJsonF(element: JsonElement): T?

    protected open fun onInit(value: T) {}
    protected open fun onUpdate(value: T) {}
    protected open fun onChange(oldValue: T, newValue: T) = newValue
    protected open fun onChanged(oldValue: T, newValue: T) {}
    open fun isSupported() = true

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

    fun toggle() = changeValue(!value)

    fun isActive() = value && isSupported()

}

/**
 * Integer value represents a value with a integer
 */
open class IntegerValue(name: String, value: Int, val minimum: Int = 0, val maximum: Int = Integer.MAX_VALUE)
    : Value<Int>(name, value) {

    fun set(newValue: Number) = set(newValue.toInt())

    override fun toJsonF() = JsonPrimitive(value)

    override fun fromJsonF(element: JsonElement) = if (element.isJsonPrimitive) element.asInt else null

    fun isMinimal() = value <= minimum
    fun isMaximal() = value >= maximum

    val range = minimum..maximum
}

/**
 * Float value represents a value with a float
 */
open class FloatValue(name: String, value: Float, val minimum: Float = 0F, val maximum: Float = Float.MAX_VALUE)
    : Value<Float>(name, value) {

    fun set(newValue: Number) = set(newValue.toFloat())

    override fun toJsonF() = JsonPrimitive(value)

    override fun fromJsonF(element: JsonElement) = if (element.isJsonPrimitive) element.asFloat else null

    fun isMinimal() = value <= minimum
    fun isMaximal() = value >= maximum

    val range = minimum..maximum
}

/**
 * Text value represents a value with a string
 */
open class TextValue(name: String, value: String) : Value<String>(name, value) {

    override fun toJsonF() = JsonPrimitive(value)

    override fun fromJsonF(element: JsonElement) = if (element.isJsonPrimitive) element.asString else null
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

    val displayName: String
        get() = when (value) {
            is GameFontRenderer -> "Font: ${(value as GameFontRenderer).defaultFont.font.name} - ${(value as GameFontRenderer).defaultFont.font.size}"
            Fonts.minecraftFont -> "Font: Minecraft"
            else -> {
                val fontInfo = Fonts.getFontDetails(value)
                fontInfo?.let {
                    "${it.name}${if (it.fontSize != -1) " - ${it.fontSize}" else ""}"
                } ?: "Font: Unknown"
            }
        }

    fun next() {
        val fonts = Fonts.fonts
        value = fonts[(fonts.indexOf(value) + 1) % fonts.size]
    }

    fun previous() {
        val fonts = Fonts.fonts.reversed()
        value = fonts[(fonts.indexOf(value) + 1) % fonts.size]
    }
}

/**
 * Block value represents a value with a block
 */
class BlockValue(name: String, value: Int) : IntegerValue(name, value, 1, 197)

/**
 * List value represents a selectable list of values
 */
open class ListValue(name: String, val values: Array<String>, override var value: String) : Value<String>(name, value) {

    var openList = false

    operator fun contains(string: String?) = values.any { it.equals(string, true) }

    override fun changeValue(newValue: String) {
        values.find { it.equals(newValue, true) }?.let { value = it }
    }

    override fun toJsonF() = JsonPrimitive(value)

    override fun fromJsonF(element: JsonElement) = if (element.isJsonPrimitive) element.asString else null
}

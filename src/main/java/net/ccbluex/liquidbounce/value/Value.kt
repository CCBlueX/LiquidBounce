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
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

abstract class Value<T>(val name: String, protected open var value: T, private val isSupported: (() -> Boolean)?)
    : ReadWriteProperty<Any?, T> {

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
    open fun isSupported() = isSupported?.invoke() ?: true

    // Support for delegating values using the `by` keyword.
    override operator fun getValue(thisRef: Any?, property: KProperty<*>) = value
    override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        set(value)
    }
}

/**
 * Bool value represents a value with a boolean
 */
open class BoolValue(name: String, value: Boolean, isSupported: (() -> Boolean)? = null) : Value<Boolean>(name, value, isSupported) {

    // TODO: Remove when all modules are ported to Kotlin
    constructor(name: String, value: Boolean) : this(name, value, null)

    override fun toJsonF() = JsonPrimitive(value)

    override fun fromJsonF(element: JsonElement) =
        if (element.isJsonPrimitive) element.asBoolean || element.asString.equals("true", ignoreCase = true)
        else null

    fun toggle() = set(!value)

    fun isActive() = value && isSupported()

}

/**
 * Integer value represents a value with a integer
 */
open class IntegerValue(name: String, value: Int, val range: IntRange = 0..Int.MAX_VALUE, isSupported: (() -> Boolean)? = null)
    : Value<Int>(name, value, isSupported) {

    // TODO: Remove when all modules are ported to Kotlin
    constructor(name: String, value: Int, minimum: Int, maximum: Int) : this(name, value, minimum..maximum, null)

    fun set(newValue: Number) = set(newValue.toInt())

    override fun toJsonF() = JsonPrimitive(value)

    override fun fromJsonF(element: JsonElement) = if (element.isJsonPrimitive) element.asInt else null

    fun isMinimal() = value <= minimum
    fun isMaximal() = value >= maximum

    val minimum = range.first
    val maximum = range.last
}

/**
 * Float value represents a value with a float
 */
open class FloatValue(name: String, value: Float, val range: ClosedFloatingPointRange<Float> = 0f..Float.MAX_VALUE,
                      isSupported: (() -> Boolean)? = null) : Value<Float>(name, value, isSupported) {

    fun set(newValue: Number) = set(newValue.toFloat())

    override fun toJsonF() = JsonPrimitive(value)

    override fun fromJsonF(element: JsonElement) = if (element.isJsonPrimitive) element.asFloat else null

    fun isMinimal() = value <= minimum
    fun isMaximal() = value >= maximum

    val minimum = range.start
    val maximum = range.endInclusive
}

/**
 * Text value represents a value with a string
 */
open class TextValue(name: String, value: String, isSupported: (() -> Boolean)? = null) : Value<String>(name, value, isSupported) {

    override fun toJsonF() = JsonPrimitive(value)

    override fun fromJsonF(element: JsonElement) = if (element.isJsonPrimitive) element.asString else null
}

/**
 * Font value represents a value with a font
 */
open class FontValue(name: String, value: FontRenderer, isSupported: (() -> Boolean)? = null)
    : Value<FontRenderer>(name, value, isSupported) {

    override fun toJsonF(): JsonElement? {
        val fontDetails = Fonts.getFontDetails(value) ?: return null
        val valueObject = JsonObject()
        valueObject.run {
            addProperty("fontName", fontDetails.name)
            addProperty("fontSize", fontDetails.fontSize)
        }
        return valueObject
    }

    override fun fromJsonF(element: JsonElement) =
        if (element.isJsonObject) {
            val valueObject = element.asJsonObject
            Fonts.getFontRenderer(valueObject["fontName"].asString, valueObject["fontSize"].asInt)
        } else null

    val displayName
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
open class BlockValue(name: String, value: Int, isSupported: (() -> Boolean)? = null) : IntegerValue(name, value, 1..197, isSupported)

/**
 * List value represents a selectable list of values
 */
open class ListValue(name: String, val values: Array<String>, override var value: String, isSupported: (() -> Boolean)? = null)
    : Value<String>(name, value, isSupported) {

    // TODO: Remove when all modules are ported to Kotlin
    constructor(name: String, values: Array<String>, value: String) : this(name, values, value, null)

    var openList = false

    operator fun contains(string: String?) = values.any { it.equals(string, true) }

    override fun changeValue(newValue: String) {
        values.find { it.equals(newValue, true) }?.let { value = it }
    }

    override fun toJsonF() = JsonPrimitive(value)

    override fun fromJsonF(element: JsonElement) = if (element.isJsonPrimitive) element.asString else null
}

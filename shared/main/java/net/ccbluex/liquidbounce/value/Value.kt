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
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IFontRenderer
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import java.awt.Color

abstract class AbstractValue(var name: String, val description: String = "")
{
	val displayName = name
	var isSupported = true
	var isBelongsToGroup = false

	abstract fun toJson(): JsonElement?
	abstract fun fromJson(element: JsonElement)

	// Backward compatibility
	abstract fun isAliasPresent(jsonModule: JsonObject): Boolean
	abstract fun fromJsonAlias(jsonModule: JsonObject)
	abstract fun aliasMatches(name: String): Boolean
	abstract fun adaptToValue(name: String): Value<*>?

	open fun showCondition(): Boolean = true
}

open class ValueGroup(name: String) : AbstractValue(name)
{
	var foldState = false
	val values = mutableListOf<AbstractValue>()

	fun add(value: AbstractValue)
	{
		values += value
		value.name = "$name.${value.name}"
		if (value is ValueGroup) value.renameSubvalues(name)
		value.isBelongsToGroup = true
	}

	fun addAll(vararg values: AbstractValue)
	{
		for (value in values) add(value)
	}

	private fun renameSubvalues(newName: String)
	{
		values.forEach { if (it is ValueGroup) it.renameSubvalues(newName) else it.name = "$newName.${it.name}" }
	}

	override fun toJson(): JsonElement?
	{
		val jsonObject = JsonObject()
		for (value in values) jsonObject.add(value.displayName, value.toJson())
		return jsonObject
	}

	override fun isAliasPresent(jsonModule: JsonObject) = false

	override fun fromJsonAlias(jsonModule: JsonObject)
	{
	}

	override fun aliasMatches(name: String) = false

	override fun adaptToValue(name: String): Value<*>? = null

	override fun fromJson(element: JsonElement)
	{
		if (!element.isJsonObject) return
		val jsonObject = element.asJsonObject
		for (value in values) jsonObject[value.displayName]?.let(value::fromJson)
	}
}

abstract class Value<T>(name: String, protected var value: T, private val alias: String? = null, description: String = "") : AbstractValue(name, description)
{
	fun set(newValue: T)
	{
		if (newValue == value) return

		val oldValue = get()

		try
		{
			onChange(oldValue, newValue)
			changeValue(newValue)
			onChanged(oldValue, newValue)
			FileManager.saveConfig(LiquidBounce.fileManager.valuesConfig)
		}
		catch (e: Exception)
		{
			ClientUtils.logger.error("[ValueSystem ($name)]: ${e.javaClass.name} (${e.message}) [$oldValue >> $newValue]")
		}
	}

	fun get() = value

	open fun changeValue(value: T)
	{
		this.value = value
	}

	override fun isAliasPresent(jsonModule: JsonObject) = alias != null && jsonModule.has(alias) && jsonModule[alias].isJsonPrimitive

	override fun fromJsonAlias(jsonModule: JsonObject)
	{
		fromJson(jsonModule[alias])
	}

	override fun aliasMatches(name: String) = alias != null && alias.equals(name, ignoreCase = true)

	override fun adaptToValue(name: String): Value<*>? = this

	protected open fun onChange(oldValue: T, newValue: T)
	{
	}

	protected open fun onChanged(oldValue: T, newValue: T)
	{
	}
}

abstract class RangeValue<T : Comparable<T>>(name: String, protected var minValue: T, protected var maxValue: T, protected val aliases: Pair<String, String>? = null, description: String = "") : AbstractValue(name, description)
{
	fun setMin(newValue: T)
	{
		if (newValue == minValue || newValue > maxValue) return

		val oldValue = getMin()

		try
		{
			onMinValueChange(oldValue, newValue)
			changeMinValue(newValue)
			onMinValueChanged(oldValue, newValue)
			FileManager.saveConfig(LiquidBounce.fileManager.valuesConfig)
		}
		catch (e: Exception)
		{
			ClientUtils.logger.error("[ValueSystem ($name)]: ${e.javaClass.name} (${e.message}) [$oldValue >> $newValue]")
		}
	}

	fun setMax(newValue: T)
	{
		if (newValue == maxValue || newValue < minValue) return

		val oldValue = getMax()

		try
		{
			onMaxValueChange(oldValue, newValue)
			changeMaxValue(newValue)
			onMaxValueChanged(oldValue, newValue)
			FileManager.saveConfig(LiquidBounce.fileManager.valuesConfig)
		}
		catch (e: Exception)
		{
			ClientUtils.logger.error("[ValueSystem ($name)]: ${e.javaClass.name} (${e.message}) [$oldValue >> $newValue]")
		}
	}

	fun getMin() = minValue
	fun getMax() = maxValue

	open fun changeMinValue(value: T)
	{
		minValue = value
	}

	open fun changeMaxValue(value: T)
	{
		maxValue = value
	}

	override fun isAliasPresent(jsonModule: JsonObject) = aliases != null && aliases.toList().all { jsonModule.has(it) && jsonModule[it].isJsonPrimitive && jsonModule[it].asJsonPrimitive.isNumber }

	override fun aliasMatches(name: String) = aliases != null && aliases.toList().any { it.equals(name, ignoreCase = true) }

	protected open fun onMinValueChange(oldValue: T, newValue: T)
	{
	}

	protected open fun onMaxValueChange(oldValue: T, newValue: T)
	{
	}

	protected open fun onMinValueChanged(oldValue: T, newValue: T)
	{
	}

	protected open fun onMaxValueChanged(oldValue: T, newValue: T)
	{
	}
}

abstract class ColorValue(name: String, protected var r: Int, protected var g: Int, protected var b: Int, protected var a: Int = 255, private val aliases: List<String?>? = null, description: String = "") : AbstractValue(name, description)
{
	fun set(hex: Int)
	{
		set(hex shr 16 and 0xFF, hex shr 8 and 0xFF, hex and 0xFF, hex shr 24 and 0xFF)
	}

	fun set(newRed: Int, newGreen: Int, newBlue: Int, newAlpha: Int)
	{
		if (newRed == r && newGreen == g && newBlue == b && newAlpha == a) return

		val oldValue = get()
		val newValue = ColorUtils.createRGB(newRed, newGreen, newBlue, newAlpha)

		try
		{
			onChange(oldValue, newValue)
			changeValue(newRed, newGreen, newBlue, newAlpha)
			onChanged(oldValue, newValue)
			FileManager.saveConfig(LiquidBounce.fileManager.valuesConfig)
		}
		catch (e: Exception)
		{
			ClientUtils.logger.error("[ValueSystem ($name)]: ${e.javaClass.name} (${e.message}) [$oldValue >> $newValue]")
		}
	}

	open fun changeValue(red: Int, green: Int, blue: Int, alpha: Int)
	{
		r = red
		g = green
		b = blue
		a = alpha
	}

	fun getRed() = r
	fun getGreen() = g
	fun getBlue() = b
	fun getAlpha() = a

	fun get() = ColorUtils.createRGB(r, g, b, a)

	fun get(alpha: Int) = ColorUtils.createRGB(r, g, b, alpha)

	fun getColor() = Color(get(), true)

	fun getColor(alpha: Int) = Color(get(alpha), true)

	override fun isAliasPresent(jsonModule: JsonObject) = aliases != null && aliases.filterNotNull().all { jsonModule.has(it) && jsonModule[it].isJsonPrimitive && jsonModule[it].asJsonPrimitive.isNumber }

	override fun aliasMatches(name: String) = aliases != null && aliases.filterNotNull().any { it.equals(name, ignoreCase = true) }

	protected open fun onChange(oldValue: Int, newValue: Int)
	{
	}

	protected open fun onChanged(oldValue: Int, newValue: Int)
	{
	}
}

open class RGBColorValue(name: String, r: Int, g: Int, b: Int, private val aliases: Triple<String, String, String>? = null, description: String = "") : ColorValue(name, r, g, b, aliases = aliases?.toList(), description = description)
{
	override fun toJson(): JsonElement?
	{
		val jsonObject = JsonObject()
		jsonObject.addProperty("red", r)
		jsonObject.addProperty("green", g)
		jsonObject.addProperty("blue", b)
		return jsonObject
	}

	override fun fromJson(element: JsonElement)
	{
		if (element.isJsonObject)
		{
			val jsonObject = element.asJsonObject

			val redElement = jsonObject["red"]
			if (redElement?.isJsonPrimitive == true) r = redElement.asInt

			val greenElement = jsonObject["green"]
			if (greenElement?.isJsonPrimitive == true) g = greenElement.asInt

			val blueElement = jsonObject["blue"]
			if (blueElement?.isJsonPrimitive == true) b = blueElement.asInt
		}
	}

	override fun fromJsonAlias(jsonModule: JsonObject)
	{
		val checkedAliases = aliases ?: return

		val redElement = jsonModule[checkedAliases.first]
		if (redElement.isJsonPrimitive) r = redElement.asInt

		val greenElement = jsonModule[checkedAliases.second]
		if (greenElement.isJsonPrimitive) g = greenElement.asInt

		val blueElement = jsonModule[checkedAliases.third]
		if (blueElement.isJsonPrimitive) b = blueElement.asInt
	}

	override fun adaptToValue(name: String): Value<*>?
	{
		val checkedAliases = aliases?.toList()?.map(String::toLowerCase) ?: return null

		return when (name)
		{
			checkedAliases[0] -> object : IntegerValue(checkedAliases[0], r, 0, 255)
			{
				override fun changeValue(value: Int)
				{
					super.changeValue(value)
					r = value
				}
			}

			checkedAliases[1] -> object : IntegerValue(checkedAliases[1], g, 0, 255)
			{
				override fun changeValue(value: Int)
				{
					super.changeValue(value)
					g = value
				}
			}

			checkedAliases[2] -> object : IntegerValue(checkedAliases[2], b, 0, 255)
			{
				override fun changeValue(value: Int)
				{
					super.changeValue(value)
					b = value
				}
			}

			else -> null
		}
	}
}

open class RGBAColorValue(name: String, r: Int, g: Int, b: Int, a: Int, private val aliases: List<String?>? = null, description: String = "") : ColorValue(name, r, g, b, a, aliases, description)
{
	override fun toJson(): JsonElement?
	{
		val jsonObject = JsonObject()
		jsonObject.addProperty("red", r)
		jsonObject.addProperty("green", g)
		jsonObject.addProperty("blue", b)
		jsonObject.addProperty("alpha", a)
		return jsonObject
	}

	override fun fromJson(element: JsonElement)
	{
		if (element.isJsonObject)
		{
			val jsonObject = element.asJsonObject

			val redElement = jsonObject["red"]
			if (redElement?.isJsonPrimitive == true) r = redElement.asInt

			val greenElement = jsonObject["green"]
			if (greenElement?.isJsonPrimitive == true) g = greenElement.asInt

			val blueElement = jsonObject["blue"]
			if (blueElement?.isJsonPrimitive == true) b = blueElement.asInt

			val alphaElement = jsonObject["alpha"]
			if (alphaElement?.isJsonPrimitive == true) a = alphaElement.asInt
		}
	}

	override fun fromJsonAlias(jsonModule: JsonObject)
	{
		val checkedAliases = aliases ?: return

		val redElement = checkedAliases[0]?.let { jsonModule[it] }
		if (redElement?.isJsonPrimitive == true) r = redElement.asInt

		val greenElement = checkedAliases[1]?.let { jsonModule[it] }
		if (greenElement?.isJsonPrimitive == true) g = greenElement.asInt

		val blueElement = checkedAliases[2]?.let { jsonModule[it] }
		if (blueElement?.isJsonPrimitive == true) b = blueElement.asInt

		val alphaElement = checkedAliases[3]?.let { jsonModule[it] }
		if (alphaElement?.isJsonPrimitive == true) a = alphaElement.asInt
	}

	override fun adaptToValue(name: String): Value<*>?
	{
		val checkedAliases = aliases?.toList()?.mapNotNull { it?.toLowerCase() } ?: return null

		return when (name)
		{
			checkedAliases[0] -> object : IntegerValue(checkedAliases[0], r, 0, 255)
			{
				override fun changeValue(value: Int)
				{
					super.changeValue(value)
					r = value
				}
			}

			checkedAliases[1] -> object : IntegerValue(checkedAliases[1], g, 0, 255)
			{
				override fun changeValue(value: Int)
				{
					super.changeValue(value)
					g = value
				}
			}

			checkedAliases[2] -> object : IntegerValue(checkedAliases[2], b, 0, 255)
			{
				override fun changeValue(value: Int)
				{
					super.changeValue(value)
					b = value
				}
			}

			checkedAliases[3] -> object : IntegerValue(checkedAliases[3], a, 0, 255)
			{
				override fun changeValue(value: Int)
				{
					super.changeValue(value)
					a = value
				}
			}

			else -> null
		}
	}
}

/**
 * Bool value represents a value with a boolean
 */
open class BoolValue(name: String, value: Boolean, alias: String? = null, description: String = "") : Value<Boolean>(name, value, alias, description)
{
	override fun toJson() = JsonPrimitive(value)

	override fun fromJson(element: JsonElement)
	{
		if (element.isJsonPrimitive) value = element.asBoolean || element.asString.equals("true", ignoreCase = true)
	}
}

/**
 * Integer value represents a value with a integer
 */
open class IntegerValue(name: String, value: Int, val minimum: Int, val maximum: Int, alias: String? = null, description: String = "") : Value<Int>(name, value, alias, description)
{

	fun set(newValue: Number)
	{
		set(newValue.toInt())
	}

	override fun toJson() = JsonPrimitive(value)

	override fun fromJson(element: JsonElement)
	{
		if (element.isJsonPrimitive) value = element.asInt
	}
}

open class IntegerRangeValue(name: String, minValue: Int, maxValue: Int, val minimum: Int, val maximum: Int, aliases: Pair<String, String>? = null, description: String = "") : RangeValue<Int>(name, minValue, maxValue, aliases, description)
{
	fun setMin(newValue: Number)
	{
		setMin(newValue.toInt())
	}

	fun setMax(newValue: Number)
	{
		setMax(newValue.toInt())
	}

	fun getRandom() = RandomUtils.nextInt(minValue, maxValue)

	fun getRandomDelay() = TimeUtils.randomDelay(minValue, maxValue)

	fun getRandomClickDelay() = TimeUtils.randomClickDelay(minValue, maxValue)

	override fun toJson(): JsonElement
	{
		val jsonObject = JsonObject()
		jsonObject.addProperty("min", minValue)
		jsonObject.addProperty("max", maxValue)
		return jsonObject
	}

	override fun fromJson(element: JsonElement)
	{
		if (element.isJsonObject)
		{
			val jsonObject = element.asJsonObject

			val minElement = jsonObject["min"]
			if (minElement?.isJsonPrimitive == true) minValue = minElement.asInt

			val maxElement = jsonObject["max"]
			if (maxElement?.isJsonPrimitive == true) maxValue = maxElement.asInt
		}
	}

	override fun fromJsonAlias(jsonModule: JsonObject)
	{
		val checkedAliases = aliases ?: return

		val maxElement = jsonModule[checkedAliases.first]
		if (maxElement.isJsonPrimitive) maxValue = maxElement.asInt

		val minElement = jsonModule[checkedAliases.second]
		if (minElement.isJsonPrimitive) minValue = minElement.asInt
	}

	override fun adaptToValue(name: String): Value<*>?
	{
		val checkedAliases = aliases?.toList()?.map(String::toLowerCase) ?: return null

		return when (name)
		{
			checkedAliases[0] -> object : IntegerValue(checkedAliases[0], maxValue, minimum, maximum)
			{
				override fun changeValue(value: Int)
				{
					super.changeValue(value)
					maxValue = value
				}
			}

			checkedAliases[1] -> object : IntegerValue(checkedAliases[1], minValue, minimum, maximum)
			{
				override fun changeValue(value: Int)
				{
					super.changeValue(value)
					minValue = value
				}
			}

			else -> null
		}
	}
}

/**
 * Float value represents a value with a float
 */
open class FloatValue(name: String, value: Float, val minimum: Float, val maximum: Float, alias: String? = null, description: String = "") : Value<Float>(name, value, alias, description)
{

	fun set(newValue: Number)
	{
		set(newValue.toFloat())
	}

	override fun toJson() = JsonPrimitive(value)

	override fun fromJson(element: JsonElement)
	{
		if (element.isJsonPrimitive) value = element.asFloat
	}
}

open class FloatRangeValue(name: String, minValue: Float, maxValue: Float, val minimum: Float, val maximum: Float, aliases: Pair<String, String>? = null, description: String = "") : RangeValue<Float>(name, minValue, maxValue, aliases, description)
{
	fun setMin(newValue: Number)
	{
		setMin(newValue.toFloat())
	}

	fun setMax(newValue: Number)
	{
		setMax(newValue.toFloat())
	}

	fun getRandom() = RandomUtils.nextFloat(minValue, maxValue)

	fun getRandomStrict() = getRandom().coerceIn(minimum, maximum)

	override fun toJson(): JsonElement
	{
		val jsonObject = JsonObject()
		jsonObject.addProperty("min", minValue)
		jsonObject.addProperty("max", maxValue)
		return jsonObject
	}

	override fun fromJson(element: JsonElement)
	{
		if (element.isJsonObject)
		{
			val jsonObject = element.asJsonObject

			val minElement = jsonObject["min"]
			if (minElement?.isJsonPrimitive == true) minValue = minElement.asFloat

			val maxElement = jsonObject["max"]
			if (maxElement?.isJsonPrimitive == true) maxValue = maxElement.asFloat
		}
	}

	override fun fromJsonAlias(jsonModule: JsonObject)
	{
		val checkedAliases = aliases ?: return

		val maxElement = jsonModule[checkedAliases.first]
		if (maxElement?.isJsonPrimitive == true) maxValue = maxElement.asFloat

		val minElement = jsonModule[checkedAliases.second]
		if (minElement?.isJsonPrimitive == true) minValue = minElement.asFloat
	}

	override fun adaptToValue(name: String): Value<*>?
	{
		val checkedAliases = aliases?.toList()?.map(String::toLowerCase) ?: return null

		return when (name)
		{
			checkedAliases[0] -> object : FloatValue(checkedAliases[0], maxValue, minimum, maximum)
			{
				override fun changeValue(value: Float)
				{
					super.changeValue(value)
					maxValue = value
				}
			}

			checkedAliases[1] -> object : FloatValue(checkedAliases[1], minValue, minimum, maximum)
			{
				override fun changeValue(value: Float)
				{
					super.changeValue(value)
					minValue = value
				}
			}

			else -> null
		}
	}
}

/**
 * Text value represents a value with a string
 */
open class TextValue(name: String, value: String, alias: String? = null, description: String = "") : Value<String>(name, value, alias, description)
{

	override fun toJson() = JsonPrimitive(value)

	override fun fromJson(element: JsonElement)
	{
		if (element.isJsonPrimitive) value = element.asString
	}
}

/**
 * Font value represents a value with a font
 */
class FontValue(valueName: String, value: IFontRenderer) : Value<IFontRenderer>(valueName, value)
{

	override fun toJson(): JsonElement?
	{
		val fontDetails = Fonts.getFontDetails(value) ?: return null
		val valueObject = JsonObject()
		valueObject.addProperty("fontName", fontDetails.name)
		valueObject.addProperty("fontSize", fontDetails.fontSize)

		return valueObject
	}

	override fun fromJson(element: JsonElement)
	{
		if (!element.isJsonObject) return
		val valueObject = element.asJsonObject
		value = Fonts.getFontRenderer(valueObject["fontName"].asString, valueObject["fontSize"].asInt)
	}
}

/**
 * Block value represents a value with a block
 */
class BlockValue(name: String, value: Int, alias: String? = null) : IntegerValue(name, value, 1, 197, alias)

/**
 * List value represents a selectable list of values
 */
open class ListValue(name: String, val values: Array<String>, value: String, alias: String? = null, description: String = "") : Value<String>(name, value, alias, description)
{

	@JvmField
	var openList = false

	init
	{
		this.value = value
	}

	operator fun contains(string: String?): Boolean = sequenceOf(*values).any { it.equals(string, ignoreCase = true) }

	override fun changeValue(value: String)
	{
		values.firstOrNull { it.equals(value, ignoreCase = true) }?.let { this.value = it }
	}

	override fun toJson() = JsonPrimitive(value)

	override fun fromJson(element: JsonElement)
	{
		if (element.isJsonPrimitive) changeValue(element.asString)
	}
}

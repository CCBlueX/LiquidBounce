/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.ui.client.hud.HUD.Companion.createDefault
import net.ccbluex.liquidbounce.ui.client.hud.HUD.Companion.elements
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.value.FontValue

class Config
{

	private var jsonArray = JsonArray()

	constructor(config: String)
	{
		jsonArray = Gson().fromJson(config, JsonArray::class.java) ?: throw IllegalStateException("Config $config seems empty or broken")
	}

	constructor(hud: HUD)
	{
		hud.elements.map { element ->
			val elementObject = JsonObject()
			elementObject.addProperty("Type", element.name)
			elementObject.addProperty("X", element.x)
			elementObject.addProperty("Y", element.y)
			elementObject.addProperty("Scale", element.scale)
			elementObject.addProperty("HorizontalFacing", element.side.horizontal.sideName)
			elementObject.addProperty("VerticalFacing", element.side.vertical.sideName)

			for (value in element.values) elementObject.add(value.name, value.toJson())
			elementObject
		}.forEach(jsonArray::add)
	}

	fun toJson(): String = GsonBuilder().setPrettyPrinting().create().toJson(jsonArray)

	fun toHUD(): HUD
	{
		val hud = HUD()

		try
		{
			for (jsonObject in jsonArray)
			{
				try
				{
					if (jsonObject !is JsonObject) continue

					if (!jsonObject.has("Type")) continue

					val type = jsonObject["Type"].asString

					for (elementClass in elements)
					{
						val classType = elementClass.getAnnotation(ElementInfo::class.java).name

						if (classType == type)
						{
							val element = elementClass.newInstance()

							element.x = jsonObject["X"].asInt.toDouble()
							element.y = jsonObject["Y"].asInt.toDouble()
							element.scale = jsonObject["Scale"].asFloat
							element.side = Side(Side.Horizontal.getByName(jsonObject["HorizontalFacing"].asString) ?: Side.Horizontal.LEFT, Side.Vertical.getByName(jsonObject["VerticalFacing"].asString) ?: Side.Vertical.UP)

							element.values.filter { jsonObject.has(it.name) }.forEach { it.fromJson(jsonObject[it.name]) }

							// Support for old HUD files
							if (jsonObject.has("font")) element.values.find { it is FontValue }?.fromJson(jsonObject["font"])

							hud.addElement(element)
							break
						}
					}
				}
				catch (e: Exception)
				{
					ClientUtils.logger.error("Error while loading custom hud element from config.", e)
				}
			}

			// Add forced elements when missing
			elements.filter { elementClass -> elementClass.getAnnotation(ElementInfo::class.java).force && hud.elements.none { it.javaClass == elementClass } }.forEach { hud.addElement(it.newInstance()) }
		}
		catch (e: Exception)
		{
			ClientUtils.logger.error("Error while loading custom hud config.", e)
			return createDefault()
		}

		return hud
	}
}

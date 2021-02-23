/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.file.configs

import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.file.FileConfig
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.ModuleElement
import net.ccbluex.liquidbounce.utils.ClientUtils.logger
import net.ccbluex.liquidbounce.utils.misc.MiscUtils
import java.io.File
import java.io.IOException

/**
 * Constructor of config
 *
 * @param file
 * of config
 */
class ClickGuiConfig(file: File) : FileConfig(file)
{
	/**
	 * Load config from file
	 *
	 * @throws IOException
	 */
	@Throws(IOException::class)
	override fun loadConfig()
	{
		val jsonElement = JsonParser().parse(MiscUtils.createBufferedFileReader(file))

		if (jsonElement is JsonNull) return

		val jsonObject = jsonElement as JsonObject

		LiquidBounce.clickGui.panels.filter { jsonObject.has(it.name) }.forEach { panel ->
			try
			{
				val panelObject = jsonObject.getAsJsonObject(panel.name)
				panel.open = panelObject["open"].asBoolean
				panel.isVisible = panelObject["visible"].asBoolean
				panel.x = panelObject["posX"].asInt
				panel.y = panelObject["posY"].asInt
				panel.elements.filterIsInstance<ModuleElement>().filter { panelObject.has(it.module.name) }.forEach {
					try
					{
						val elementObject = panelObject.getAsJsonObject(it.module.name)
						it.isShowSettings = elementObject["Settings"].asBoolean
					}
					catch (e: Exception)
					{
						logger.error("Error while loading clickgui module element with the name '{}' (Panel Name: {}).", it.module.name, panel.name, e)
					}
				}
			}
			catch (e: Exception)
			{
				logger.error("Error while loading clickgui panel with the name '{}'.", panel.name, e)
			}
		}
	}

	/**
	 * Save config to file
	 *
	 * @throws IOException
	 */
	@Throws(IOException::class)
	override fun saveConfig()
	{
		val jsonObject = JsonObject()

		for (panel in LiquidBounce.clickGui.panels)
		{
			val panelObject = JsonObject()
			panelObject.addProperty("open", panel.open)
			panelObject.addProperty("visible", panel.isVisible)
			panelObject.addProperty("posX", panel.x)
			panelObject.addProperty("posY", panel.y)

			panel.elements.filterIsInstance<ModuleElement>().forEach { element ->
				val elementObject = JsonObject()
				elementObject.addProperty("Settings", element.isShowSettings)
				panelObject.add(element.module.name, elementObject)
			}

			jsonObject.add(panel.name, panelObject)
		}

		val writer = MiscUtils.createBufferedFileWriter(file)
		writer.write(FileManager.PRETTY_GSON.toJson(jsonObject) + System.lineSeparator())
		writer.close()
	}
}

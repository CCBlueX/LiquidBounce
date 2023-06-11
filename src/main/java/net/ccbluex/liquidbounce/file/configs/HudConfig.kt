/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.file.configs

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.file.FileConfig
import net.ccbluex.liquidbounce.file.FileManager.PRETTY_GSON
import net.ccbluex.liquidbounce.ui.client.hud.HUD
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.value.FontValue
import java.io.File
import java.io.IOException

class HudConfig(file: File) : FileConfig(file) {

    override fun loadDefault() = HUD.setDefault()

    /**
     * Load config from file
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    override fun loadConfig() {
        val jsonArray = PRETTY_GSON.fromJson(file.bufferedReader(), JsonArray::class.java)

        HUD.clearElements()

        try {
            for (jsonObject in jsonArray) {
                try {
                    if (jsonObject !is JsonObject)
                        continue

                    if (!jsonObject.has("Type"))
                        continue

                    val type = jsonObject["Type"].asString

                    for (elementClass in HUD.ELEMENTS) {
                        val classType = elementClass.getAnnotation(ElementInfo::class.java).name

                        if (classType == type) {
                            val element = elementClass.newInstance()

                            element.x = jsonObject["X"].asDouble
                            element.y = jsonObject["Y"].asDouble
                            element.scale = jsonObject["Scale"].asFloat
                            element.side = Side(
                                Side.Horizontal.getByName(jsonObject["HorizontalFacing"].asString) ?: Side.Horizontal.RIGHT,
                                Side.Vertical.getByName(jsonObject["VerticalFacing"].asString) ?: Side.Vertical.UP
                            )

                            for (value in element.values) {
                                if (jsonObject.has(value.name))
                                    value.fromJson(jsonObject[value.name])
                            }

                            // Support for old HUD files
                            if (jsonObject.has("font"))
                                element.values.find { it is FontValue }?.fromJson(jsonObject["font"])

                            HUD.addElement(element)
                            break
                        }
                    }
                } catch (e: Exception) {
                    ClientUtils.LOGGER.error("Error while loading custom hud element from config.", e)
                }
            }

            // Add forced elements when missing
            for (elementClass in HUD.ELEMENTS) {
                if (elementClass.getAnnotation(ElementInfo::class.java).force
                    && HUD.elements.none { it.javaClass == elementClass }) {
                    HUD.addElement(elementClass.newInstance())
                }
            }
        } catch (e: Exception) {
            ClientUtils.LOGGER.error("Error while loading custom hud config.", e)
            HUD.setDefault()
        }
    }

    /**
     * Save config to file
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    override fun saveConfig() {
        val jsonArray = JsonArray()

        for (element in HUD.elements) {
            val elementObject = JsonObject()
            elementObject.run {
                addProperty("Type", element.name)
                addProperty("X", element.x)
                addProperty("Y", element.y)
                addProperty("Scale", element.scale)
                addProperty("HorizontalFacing", element.side.horizontal.sideName)
                addProperty("VerticalFacing", element.side.vertical.sideName)
            }

            for (value in element.values)
                elementObject.add(value.name, value.toJson())

            jsonArray.add(elementObject)
        }

        file.writeText(PRETTY_GSON.toJson(jsonArray))
    }
}
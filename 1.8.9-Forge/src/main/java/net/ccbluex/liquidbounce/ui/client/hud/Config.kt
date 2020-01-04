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

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
class Config {

    private var jsonArray = JsonArray()

    constructor(config: String) {
        jsonArray = Gson().fromJson(config, JsonArray::class.java)
    }

    constructor(hud: HUD) {
        for (element in hud.elements) {
            val elementObject = JsonObject()
            elementObject.addProperty("Type", element.name)
            elementObject.addProperty("X", element.x)
            elementObject.addProperty("Y", element.y)
            elementObject.addProperty("Scale", element.scale)
            elementObject.addProperty("HorizontalFacing", element.side.horizontal.sideName)
            elementObject.addProperty("VerticalFacing", element.side.vertical.sideName)

            for (value in element.values)
                elementObject.add(value.name, value.toJson())

            jsonArray.add(elementObject)
        }
    }

    fun toJson(): String = GsonBuilder().setPrettyPrinting().create().toJson(jsonArray)

    fun toHUD(): HUD {
        val hud = HUD()

        try {
            for (jsonObject in jsonArray) {
                try {
                    if (jsonObject !is JsonObject)
                        continue

                    if (!jsonObject.has("Type"))
                        continue

                    val type = jsonObject["Type"].asString

                    for (elementClass in elements) {
                        val classType = elementClass.getAnnotation(ElementInfo::class.java).name

                        if (classType == type) {
                            val element = elementClass.newInstance()

                            element.x = jsonObject["X"].asInt.toDouble()
                            element.y = jsonObject["Y"].asInt.toDouble()
                            element.scale = jsonObject["Scale"].asFloat
                            element.side = Side(
                                    Side.Horizontal.getByName(jsonObject["HorizontalFacing"].asString)!!,
                                    Side.Vertical.getByName(jsonObject["VerticalFacing"].asString)!!
                            )

                            for (value in element.values) {
                                if (jsonObject.has(value.name))
                                    value.fromJson(jsonObject[value.name])
                            }

                            hud.addElement(element)
                            break
                        }
                    }
                } catch (e: Exception) {
                    ClientUtils.getLogger().error("Error while loading custom hud element from config.", e)
                }
            }
        } catch (e: Exception) {
            ClientUtils.getLogger().error("Error while loading custom hud config.", e)
            return createDefault()
        }

        return hud
    }
}
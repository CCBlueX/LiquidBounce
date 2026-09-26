/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ccbluex.liquidbounce.config.gson.adapter

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import net.ccbluex.liquidbounce.utils.render.Alignment
import net.ccbluex.liquidbounce.utils.render.Alignment.ScreenAxisX
import java.lang.reflect.Type

object AlignmentAdapter : JsonDeserializer<Alignment>, JsonSerializer<Alignment> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Alignment {
        val obj = json.asJsonObject
        val horizontalAlignmentStr = obj["horizontalAlignment"].asString
        val horizontalAlignment = ScreenAxisX.entries.find { it.tag == horizontalAlignmentStr }
            ?: throw IllegalArgumentException("Invalid horizontal alignment: $horizontalAlignmentStr")
        val horizontalOffset = obj["horizontalOffset"].asInt
        val verticalAlignmentStr = obj["verticalAlignment"].asString
        val verticalAlignment = Alignment.ScreenAxisY.entries.find { it.tag == verticalAlignmentStr }
            ?: throw IllegalArgumentException("Invalid vertical alignment: $verticalAlignmentStr")
        val verticalOffset = obj["verticalOffset"].asInt

        return Alignment(horizontalAlignment, horizontalOffset, verticalAlignment, verticalOffset)
    }

    override fun serialize(
        src: Alignment,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ) = JsonObject().apply {
        addProperty("horizontalAlignment", src.horizontalAlignment.tag)
        addProperty("horizontalOffset", src.horizontalOffset)
        addProperty("verticalAlignment", src.verticalAlignment.tag)
        addProperty("verticalOffset", src.verticalOffset)
    }

}

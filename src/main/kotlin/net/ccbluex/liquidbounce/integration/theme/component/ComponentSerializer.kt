/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2024 CCBlueX
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
 *
 *
 */

package net.ccbluex.liquidbounce.integration.theme.component

import com.google.gson.*
import net.ccbluex.liquidbounce.integration.theme.type.Theme
import net.ccbluex.liquidbounce.utils.render.Alignment
import net.ccbluex.liquidbounce.utils.render.Alignment.ScreenAxisX
import java.lang.reflect.Type
import java.util.*

object FriendlyAlignmentDeserializer : JsonDeserializer<Alignment> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Alignment {
        val obj = json.asJsonObject
        val horizontal = obj["horizontal"].asString
        val horizontalEnum = ScreenAxisX.entries.find { it.choiceName == horizontal }
            ?: throw IllegalArgumentException("Invalid horizontal alignment: $horizontal")
        val horizontalOffset = obj["horizontalOffset"].asInt
        val vertical = obj["vertical"].asString
        val verticalEnum = Alignment.ScreenAxisY.entries.find { it.choiceName == vertical }
            ?: throw IllegalArgumentException("Invalid vertical alignment: $vertical")
        val verticalOffset = obj["verticalOffset"].asInt

        return Alignment(horizontalEnum, horizontalOffset, verticalEnum, verticalOffset)

    }

}

object ComponentSerializer : JsonSerializer<Component> {

    override fun serialize(
        src: Component,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ) = JsonObject().apply {
        addProperty("name", src.name)
        add("settings", JsonObject().apply {
            for (v in src.inner) {
                add(v.name.lowercase(Locale.ROOT), when (v) {
                    is Alignment -> JsonPrimitive(v.toStyle())
                    else -> context.serialize(v.inner)
                })
            }
        })
    }

}

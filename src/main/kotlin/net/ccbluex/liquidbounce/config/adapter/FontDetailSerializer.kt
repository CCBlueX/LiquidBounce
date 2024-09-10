/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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

package net.ccbluex.liquidbounce.config.adapter

import com.google.gson.*
import net.ccbluex.liquidbounce.render.Fonts
import java.lang.reflect.Type

object FontDetailSerializer : JsonSerializer<Fonts.FontLoadingInfo>, JsonDeserializer<Fonts.FontLoadingInfo> {

    override fun serialize(src: Fonts.FontLoadingInfo, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val obj = JsonObject()
        obj.addProperty("name", src.name)
        return obj
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Fonts.FontLoadingInfo {
        val obj = json.asJsonObject
        return Fonts.FontLoadingInfo(obj.get("name").asString)
    }

}

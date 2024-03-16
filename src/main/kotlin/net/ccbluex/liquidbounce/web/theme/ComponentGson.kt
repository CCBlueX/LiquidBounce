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

package net.ccbluex.liquidbounce.web.theme

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import net.ccbluex.liquidbounce.utils.render.Alignment
import net.ccbluex.liquidbounce.web.theme.component.Component
import java.lang.reflect.Type
import java.util.*

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

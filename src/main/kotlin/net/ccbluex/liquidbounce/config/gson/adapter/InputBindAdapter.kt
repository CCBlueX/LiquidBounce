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
import com.mojang.blaze3d.platform.InputConstants
import net.ccbluex.fastutil.enumSetOf
import net.ccbluex.liquidbounce.authlib.utils.array
import net.ccbluex.liquidbounce.authlib.utils.string
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.utils.input.InputBind
import java.lang.reflect.Type

object InputBindAdapter : JsonSerializer<InputBind>, JsonDeserializer<InputBind> {

    override fun serialize(src: InputBind, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonObject().apply {
            add("boundKey", context.serialize(src.boundKey, InputConstants.Key::class.java))
            add("action", context.serialize(src.action, Tagged::class.java))
            if (src.modifiers.isNotEmpty()) {
                add("modifiers", context.serialize(src.modifiers))
            }
        }
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): InputBind {
        if (json.isJsonPrimitive) {
            val primitive = json.asJsonPrimitive

            // We do not want to throw an error but simply unbind the key instead.
            if (!primitive.isNumber) {
                return InputBind(InputConstants.UNKNOWN, InputBind.BindAction.TOGGLE, emptySet())
            }

            // Bind Action goes missing as we cannot access the action that is located
            // one element above - Sorry!
            return InputBind(
                InputConstants.Type.KEYSYM.getOrCreate(primitive.asInt),
                InputBind.BindAction.TOGGLE,
                emptySet(),
            )
        }

        val jsonObject = json.asJsonObject
        val boundKey = context.deserialize<InputConstants.Key>(
            jsonObject.get("boundKey"),
            InputConstants.Key::class.java
        )
        val actionStr = jsonObject.string("action")
        val action = InputBind.BindAction.entries.find { it.tag.equals(actionStr, ignoreCase = true) }
            ?: InputBind.BindAction.TOGGLE
        val modifierSet = jsonObject.array("modifiers")?.mapNotNullTo(enumSetOf<InputBind.Modifier>()) { element ->
            InputBind.Modifier.of(element.asString)
        }.orEmpty()

        return InputBind(boundKey, action, modifierSet)
    }

}

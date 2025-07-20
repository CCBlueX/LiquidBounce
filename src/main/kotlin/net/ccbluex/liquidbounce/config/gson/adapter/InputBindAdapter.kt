/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 202 CCBlueX
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

import com.google.gson.*
import net.ccbluex.liquidbounce.utils.input.InputBind
import net.minecraft.client.util.InputUtil
import java.lang.reflect.Type

object InputBindAdapter : JsonSerializer<InputBind>, JsonDeserializer<InputBind> {

    override fun serialize(src: InputBind, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonObject().apply {
            add("boundKey", context.serialize(src.boundKey, InputUtil.Key::class.java))
            addProperty("action", src.action.choiceName)
        }
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): InputBind {
        if (json.isJsonPrimitive) {
            val primitive = json.asJsonPrimitive

            // We do not want to throw an error but simply unbind the key instead.
            if (!primitive.isNumber) {
                return InputBind(InputUtil.UNKNOWN_KEY, InputBind.BindAction.TOGGLE)
            }

            // Bind Action goes missing as we cannot access the action that is located
            // one element above - Sorry!
            return InputBind(InputUtil.Type.KEYSYM.createFromCode(primitive.asInt), InputBind.BindAction.TOGGLE)
        }

        val jsonObject = json.asJsonObject
        val boundKey = context.deserialize<InputUtil.Key>(jsonObject.get("boundKey"), InputUtil.Key::class.java)
        val actionStr = jsonObject.get("action").asString
        val action = InputBind.BindAction.entries.find { it.choiceName == actionStr }
            ?: InputBind.BindAction.TOGGLE

        return InputBind(boundKey, action)
    }

}

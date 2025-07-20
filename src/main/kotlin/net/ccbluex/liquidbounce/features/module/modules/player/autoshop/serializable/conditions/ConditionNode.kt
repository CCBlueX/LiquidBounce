/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.player.autoshop.serializable.conditions

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

sealed interface ConditionNode

class ConditionNodeDeserializer : JsonDeserializer<ConditionNode> {
    override fun deserialize(json: JsonElement?, typeOfT: Type, context: JsonDeserializationContext): ConditionNode {
        if (json == null || !json.isJsonObject) {
            throw JsonParseException("Invalid JSON: Expected a JsonObject")
        }

        val jsonObject = json.asJsonObject

        return when {
            jsonObject.has("id") -> context.deserialize(json, ItemConditionNode::class.java)
            jsonObject.has("any") -> context.deserialize(json, AnyConditionNode::class.java)
            jsonObject.has("all") -> context.deserialize(json, AllConditionNode::class.java)
            else -> throw JsonParseException("Unknown ConditionNode type: Missing or invalid discriminator")
        }
    }
}

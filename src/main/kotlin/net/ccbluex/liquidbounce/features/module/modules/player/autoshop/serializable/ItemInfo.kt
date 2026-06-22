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
package net.ccbluex.liquidbounce.features.module.modules.player.autoshop.serializable

import com.google.gson.JsonDeserializer
import com.google.gson.JsonParseException

data class ItemInfo(
    val id: String,
    val minAmount: Int = 1
) {
    companion object {
        @JvmField
        val Deserializer = JsonDeserializer<ItemInfo> { json, _, _ ->
            if (json == null || !json.isJsonObject) {
                throw JsonParseException("Invalid JSON: Expected a JsonObject")
            }

            val jsonObject = json.asJsonObject

            if (!jsonObject.has("id")) {
                throw JsonParseException("Invalid JSON: Missing 'id' property")
            }

            val id = jsonObject["id"].asString
            val minAmount = jsonObject["minAmount"]?.asInt ?: 1

            ItemInfo(id, minAmount)
        }
    }
}

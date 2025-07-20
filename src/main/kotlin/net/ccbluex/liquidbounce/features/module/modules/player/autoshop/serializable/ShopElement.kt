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
package net.ccbluex.liquidbounce.features.module.modules.player.autoshop.serializable

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import net.ccbluex.liquidbounce.features.module.modules.player.autoshop.serializable.conditions.ConditionNode
import java.lang.reflect.Type

data class ShopElement (
    val item: ItemInfo,
    val amountPerClick: Int = 1,
    val categorySlot: Int,
    val itemSlot: Int,
    val price: ItemInfo,
    val purchaseConditions: ConditionNode? = null
)

class ShopElementDeserializer : JsonDeserializer<ShopElement> {
    override fun deserialize(json: JsonElement?, typeOfT: Type, context: JsonDeserializationContext): ShopElement {
        if (json == null || !json.isJsonObject) {
            throw JsonParseException("Invalid JSON: Expected a JsonObject")
        }

        val jsonObject = json.asJsonObject

        if (!jsonObject.has("item")) {
            throw JsonParseException("Invalid JSON: Missing 'item' property")
        }

        val item = context.deserialize<ItemInfo>(jsonObject["item"], ItemInfo::class.javaObjectType)
        val amountPerClick = jsonObject["amountPerClick"]?.asInt ?: 1
        val categorySlot = jsonObject["categorySlot"].asInt
        val itemSlot = jsonObject["itemSlot"].asInt
        val price = context.deserialize<ItemInfo>(jsonObject["price"], ItemInfo::class.javaObjectType)

        val purchaseConditions = jsonObject["purchaseConditions"]?.let {
            context.deserialize<ConditionNode>(it, ConditionNode::class.javaObjectType)
        }

        return ShopElement(item, amountPerClick, categorySlot, itemSlot, price, purchaseConditions)
    }
}

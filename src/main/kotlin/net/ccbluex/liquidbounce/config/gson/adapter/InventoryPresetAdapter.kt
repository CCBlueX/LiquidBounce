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
import com.google.gson.reflect.TypeToken
import net.ccbluex.liquidbounce.features.inventoryPreset.FrontendItemLimitRules
import net.ccbluex.liquidbounce.features.inventoryPreset.FrontendSlotPreference
import net.ccbluex.liquidbounce.features.inventoryPreset.InventoryPreset
import java.lang.reflect.Type

object InventoryPresetAdapter : JsonSerializer<InventoryPreset>, JsonDeserializer<InventoryPreset> {
    override fun serialize(
        src: InventoryPreset,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement = JsonObject().apply {
        add("items", context.serialize(src.itemRulesToArray()))
        add("maxStacks", context.serialize(src.itemLimitRules))
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): InventoryPreset = with(json.asJsonObject) {
        val items = getAsJsonArray("items").map { context.decode<List<FrontendSlotPreference>>(it) }
        val throws = getAsJsonArray("maxStacks").map { context.decode<FrontendItemLimitRules>(it) }

        return InventoryPreset(items.toTypedArray(), throws)
    }

    private inline fun <reified T> JsonDeserializationContext.decode(element: JsonElement): T {
        return deserialize(element, object : TypeToken<T>() {}.type)
    }
}

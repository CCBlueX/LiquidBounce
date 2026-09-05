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
import net.ccbluex.liquidbounce.features.inventoryPreset.FrontendSlotPreference
import net.ccbluex.liquidbounce.features.inventoryPreset.FrontendSlotPreference.GroupSlotPreference.ItemGroupType
import net.minecraft.world.item.Item
import java.lang.reflect.Type

object FrontendSlotPreferenceAdapter :
    JsonSerializer<FrontendSlotPreference>, JsonDeserializer<FrontendSlotPreference> {
    override fun serialize(
        src: FrontendSlotPreference,
        typeOfSrc: Type?,
        context: JsonSerializationContext
    ): JsonObject {
        return src.serialize(context)
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext
    ): FrontendSlotPreference {
        val obj = json.asJsonObject

        return when (obj["type"].asString) {
            "SINGLE" -> FrontendSlotPreference.SingleSlotPreference(
                context.deserialize(
                    obj["item"],
                    Item::class.java
                )
            )

            "GROUP" -> FrontendSlotPreference.GroupSlotPreference(
                context.deserialize(
                    obj["group"],
                    ItemGroupType::class.java
                )
            )

            "IGNORE" -> FrontendSlotPreference.IgnoreSlotPreference
            "ANY" -> FrontendSlotPreference.AnySlotPreference
            else -> error("Unknown slot preference ${obj["type"]}")
        }
    }
}

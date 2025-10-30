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

package net.ccbluex.liquidbounce.config.gson.serializer.minecraft

import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonParseException
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.mojang.serialization.JsonOps
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.registry.DynamicRegistryManager
import net.minecraft.text.Text
import net.minecraft.text.TextCodecs
import java.lang.reflect.Type

object TextSerializer : JsonSerializer<Text> {
    override fun serialize(src: Text?, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        src ?: return JsonNull.INSTANCE

        val registries = mc.world?.registryManager ?: DynamicRegistryManager.EMPTY

        return TextCodecs.CODEC.encodeStart(registries.getOps(JsonOps.INSTANCE), src)
            .getOrThrow(::JsonParseException)
    }
}

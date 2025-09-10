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

package net.ccbluex.liquidbounce.config.gson.adapter

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonParseException
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import net.minecraft.component.ComponentChanges
import java.lang.reflect.Type

/**
 * [Fabric Documentation](https://docs.fabricmc.net/1.21/develop/codecs)
 */
class CodecBasedAdapter<T>(private val codec: Codec<T>) : JsonSerializer<T>, JsonDeserializer<T> {

    override fun deserialize(
        jsonElement: JsonElement?,
        type: Type,
        jsonDeserializationContext: JsonDeserializationContext
    ): T? {
        jsonElement ?: return null

        return codec.parse(JsonOps.INSTANCE, jsonElement).resultOrPartial {
            throw JsonParseException("Failed to decode json element $jsonElement with $codec, error: $it")
        }.orElse(null)
    }

    override fun serialize(t: T?, type: Type, jsonSerializationContext: JsonSerializationContext): JsonElement? {
        t ?: return JsonNull.INSTANCE

        return codec.encodeStart(JsonOps.INSTANCE, t).resultOrPartial {
            throw JsonParseException("Failed to encode $t with $codec, error: $it")
        }.orElse(null)
    }

    companion object {
        /** For ItemStack */
        @JvmField
        val COMPONENT_CHANGES = CodecBasedAdapter(ComponentChanges.CODEC)
    }

}

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
import com.google.gson.JsonNull
import com.google.gson.JsonParseException
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.JsonOps
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.translated
import net.minecraft.core.RegistryAccess
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import java.lang.reflect.Type

/**
 * [Fabric Documentation](https://docs.fabricmc.net/1.21/develop/codecs)
 */
class CodecBasedAdapter<T>(private val codec: Codec<T>) : JsonSerializer<T>, JsonDeserializer<T> {

    private val jsonOps
        get() = (mc.level?.registryAccess() ?: RegistryAccess.EMPTY).createSerializationContext(JsonOps.INSTANCE)

    override fun deserialize(
        jsonElement: JsonElement?,
        type: Type,
        jsonDeserializationContext: JsonDeserializationContext
    ): T? {
        jsonElement ?: return null

        return when (val parsed = codec.parse(jsonOps, jsonElement)) {
            is DataResult.Success -> parsed.value
            is DataResult.Error ->
                throw JsonParseException("Failed to encode $jsonElement with $codec, error: ${parsed.message()}")
        }
    }

    override fun serialize(t: T?, type: Type, jsonSerializationContext: JsonSerializationContext): JsonElement? {
        t ?: return JsonNull.INSTANCE

        return when (val encoded = codec.encodeStart(jsonOps, t)) {
            is DataResult.Success -> encoded.value
            is DataResult.Error ->
                throw JsonParseException("Failed to encode $t with $codec, error: ${encoded.message()}")
        }
    }

    companion object {
        /** For ItemStack */
        @JvmField
        val DATA_COMPONENT_PATCH = CodecBasedAdapter(DataComponentPatch.CODEC)

        @JvmField
        val COMPONENT = CodecBasedAdapter(ComponentSerialization.CODEC)

        @JvmField
        val TRANSLATED_COMPONENT = JsonSerializer<Component> { src, t, ctx ->
            src?.translated()?.let { COMPONENT.serialize(it, t, ctx) } ?: JsonNull.INSTANCE
        }
    }

}

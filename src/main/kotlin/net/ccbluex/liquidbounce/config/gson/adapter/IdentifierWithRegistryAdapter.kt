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

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier

class IdentifierWithRegistryAdapter<T : Any>(val registry: Registry<T>) : TypeAdapter<T>() {

    override fun read(source: JsonReader): T? {
        return registry.getValue(Identifier.tryParse(source.nextString()))
    }

    override fun write(sink: JsonWriter, value: T?) {
        val id = value?.let { registry.getKey(it) }
        if (id == null) {
            sink.nullValue()
        } else {
            sink.value(id.toString())
        }
    }

    companion object {
        @JvmField
        val ENTITY_TYPE = IdentifierWithRegistryAdapter(BuiltInRegistries.ENTITY_TYPE)

        @JvmField
        val ITEM = IdentifierWithRegistryAdapter(BuiltInRegistries.ITEM)

        @JvmField
        val BLOCK = IdentifierWithRegistryAdapter(BuiltInRegistries.BLOCK)

        @JvmField
        val SOUND_EVENT = IdentifierWithRegistryAdapter(BuiltInRegistries.SOUND_EVENT)

        @JvmField
        val STATUS_EFFECT = IdentifierWithRegistryAdapter(BuiltInRegistries.MOB_EFFECT)

        @JvmField
        val SCREEN_HANDLER = IdentifierWithRegistryAdapter(BuiltInRegistries.MENU)
    }

}

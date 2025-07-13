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

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier

abstract class IdentifierAsStringAdapter<T : Any>(val registry: Registry<T>) : TypeAdapter<T>() {

    override fun read(source: JsonReader): T? {
        return registry.get(Identifier.tryParse(source.nextString()))
    }

    override fun write(sink: JsonWriter, value: T?) {
        val id = value?.let { registry.getId(it) }
        if (id == null) {
            sink.nullValue()
        } else {
            sink.value(id.toString())
        }
    }

}

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
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import net.minecraft.core.BlockPos
import net.minecraft.core.Vec3i
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import org.joml.Vector2f
import org.joml.Vector2fc

/**
 * Please use [Vec3i] instead of [BlockPos] for serialization.
 */
object Vec3iAdapter : TypeAdapter<Vec3i>() {

    override fun write(out: JsonWriter, value: Vec3i?) {
        if (value == null) {
            out.nullValue()
            return
        }
        out.beginObject()
        out.name("x").value(value.x)
        out.name("y").value(value.y)
        out.name("z").value(value.z)
        out.endObject()
    }

    override fun read(reader: JsonReader): Vec3i? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        var x = 0
        var y = 0
        var z = 0
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "x" -> x = reader.nextInt()
                "y" -> y = reader.nextInt()
                "z" -> z = reader.nextInt()
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return Vec3i(x, y, z)
    }

}

object Vec3dAdapter : TypeAdapter<Vec3>() {

    override fun write(out: JsonWriter, value: Vec3?) {
        if (value == null) {
            out.nullValue()
            return
        }
        out.beginObject()
        out.name("x").value(value.x)
        out.name("y").value(value.y)
        out.name("z").value(value.z)
        out.endObject()
    }

    override fun read(reader: JsonReader): Vec3? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        var x = 0.0
        var y = 0.0
        var z = 0.0
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "x" -> x = reader.nextDouble()
                "y" -> y = reader.nextDouble()
                "z" -> z = reader.nextDouble()
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return Vec3(x, y, z)
    }

}

object Vec2fAdapter : TypeAdapter<Vec2>() {

    override fun write(out: JsonWriter, value: Vec2?) {
        if (value == null) {
            out.nullValue()
            return
        }
        out.beginObject()
        out.name("x").value(value.x)
        out.name("y").value(value.y)
        out.endObject()
    }

    override fun read(reader: JsonReader): Vec2? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        var x = 0f
        var y = 0f
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "x" -> x = reader.nextDouble().toFloat()
                "y" -> y = reader.nextDouble().toFloat()
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return Vec2(x, y)
    }

}

object Vector2fcAdapter : TypeAdapter<Vector2fc>() {

    override fun write(out: JsonWriter, value: Vector2fc?) {
        if (value == null) {
            out.nullValue()
            return
        }
        out.beginObject()
        out.name("x").value(value.x())
        out.name("y").value(value.y())
        out.endObject()
    }

    override fun read(reader: JsonReader): Vector2fc? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        var x = 0f
        var y = 0f
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "x" -> x = reader.nextDouble().toFloat()
                "y" -> y = reader.nextDouble().toFloat()
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return Vector2f(x, y)
    }

}

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
import net.minecraft.core.BlockPos
import net.minecraft.core.Vec3i
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import org.joml.Vector2f
import org.joml.Vector2fc
import java.lang.reflect.Type

/**
 * Please use [Vec3i] instead of [BlockPos] for serialization.
 */
object Vec3iAdapter : JsonSerializer<Vec3i>, JsonDeserializer<Vec3i> {

    override fun serialize(src: Vec3i, typeOfSrc: Type, context: JsonSerializationContext) = JsonObject().apply {
        addProperty("x", src.x)
        addProperty("y", src.y)
        addProperty("z", src.z)
    }

    override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?) = Vec3i(
        json.asJsonObject["x"].asInt,
        json.asJsonObject["y"].asInt,
        json.asJsonObject["z"].asInt
    )

}

object Vec3dAdapter : JsonSerializer<Vec3>, JsonDeserializer<Vec3> {

    override fun serialize(src: Vec3, typeOfSrc: Type, context: JsonSerializationContext) = JsonObject().apply {
        addProperty("x", src.x)
        addProperty("y", src.y)
        addProperty("z", src.z)
    }

    override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?) = Vec3(
        json.asJsonObject["x"].asDouble,
        json.asJsonObject["y"].asDouble,
        json.asJsonObject["z"].asDouble
    )

}

object Vec2fAdapter : JsonSerializer<Vec2>, JsonDeserializer<Vec2> {

    override fun serialize(src: Vec2, typeOfSrc: Type, context: JsonSerializationContext) = JsonObject().apply {
        addProperty("x", src.x)
        addProperty("y", src.y)
    }

    override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?) = Vec2(
        json.asJsonObject["x"].asFloat,
        json.asJsonObject["y"].asFloat
    )

}

object Vector2fcAdapter : JsonSerializer<Vector2fc>, JsonDeserializer<Vector2fc> {

    override fun serialize(src: Vector2fc, typeOfSrc: Type, context: JsonSerializationContext) = JsonObject().apply {
        addProperty("x", src.x())
        addProperty("y", src.y())
    }

    override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?) = Vector2f(
        json.asJsonObject["x"].asFloat,
        json.asJsonObject["y"].asFloat
    )

}


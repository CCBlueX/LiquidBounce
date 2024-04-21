/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
 *
 */
package net.ccbluex.liquidbounce.config.adapter

import com.google.gson.*
import net.minecraft.block.Block
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import java.lang.reflect.Type

object BlockValueSerializer : JsonSerializer<Block>, JsonDeserializer<Block> {

    override fun serialize(src: Block, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(Registries.BLOCK.getId(src).toString())
    }

    override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?): Block {
        return Registries.BLOCK.get(Identifier.tryParse(json.asString))
    }

}

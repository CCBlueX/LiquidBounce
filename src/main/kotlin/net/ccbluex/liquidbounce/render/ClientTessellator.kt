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

package net.ccbluex.liquidbounce.render

import net.ccbluex.liquidbounce.utils.kotlin.enumMapOf
import net.minecraft.client.render.BufferBuilder
import net.minecraft.client.render.VertexFormat.DrawMode
import net.minecraft.client.util.BufferAllocator
import java.util.EnumMap

object ClientTessellator {

    private const val BUFFER_SIZE = 0xC0000

    private val bufferAllocators = enumMapOf<DrawMode, EnumMap<VertexInputType, BufferAllocator>> { _ ->
        enumMapOf()
    }

    @JvmStatic
    fun allocator(drawMode: DrawMode, vertexInputType: VertexInputType): BufferAllocator =
        bufferAllocators[drawMode]!!.getOrPut(vertexInputType) { BufferAllocator(BUFFER_SIZE) }

    @JvmStatic
    fun begin(drawMode: DrawMode, vertexInputType: VertexInputType): BufferBuilder =
        BufferBuilder(
            allocator(drawMode, vertexInputType),
            drawMode,
            vertexInputType.vertexFormat
        )

}

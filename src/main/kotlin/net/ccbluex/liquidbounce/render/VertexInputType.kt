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

import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.render.VertexFormats

sealed interface VertexInputType {
    val debugName: String

    val vertexFormat: VertexFormat


//    fun createBuffer(vertexCount: Int): GpuBuffer = RenderSystem.getDevice().createBuffer(
//        { this.debugName },
//        BufferType.VERTICES,
//        BufferUsage.DYNAMIC_WRITE,
//        vertexCount * this.vertexFormat.vertexSize,
//    )

    object Pos : VertexInputType {
        override val debugName get() = "VertexInputType.Pos"

        override val vertexFormat: VertexFormat
            get() = VertexFormats.POSITION
    }

    object PosColor : VertexInputType {
        override val debugName get() = "VertexInputType.PosColor"

        override val vertexFormat: VertexFormat
            get() = VertexFormats.POSITION_COLOR
    }

    object PosTexColor : VertexInputType {
        override val debugName get() = "VertexInputType.PosTexColor"

        override val vertexFormat: VertexFormat
            get() = VertexFormats.POSITION_TEXTURE_COLOR
    }

}

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

import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.systems.RenderPass
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.gpuDevice
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.render.createGpuBuffer
import net.minecraft.client.gl.Framebuffer
import net.minecraft.client.render.BufferBuilder
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.util.BufferAllocator
import java.util.OptionalDouble
import java.util.OptionalInt

internal val trianglePosTexVertexBuffer: GpuBuffer =
    BufferAllocator(VertexFormats.POSITION_TEXTURE.vertexSize * 3).use { allocator ->
        BufferBuilder(allocator, DrawMode.TRIANGLES, VertexFormats.POSITION_TEXTURE).apply {
            vertex(-1f, -1f, 0f).texture(0f, 0f)
            vertex(3f, -1f, 0f).texture(2f, 0f)
            vertex(-1f, 3f, 0f).texture(0f, 2f)
        }.end().createGpuBuffer { "Triangle full screen position texture vertex buffer" }
    }

fun RenderPass.drawFullScreenPositionTexture() {
    setVertexBuffer(0, trianglePosTexVertexBuffer)
    draw(0, 3)
}

@JvmOverloads
internal fun newRenderPass(framebuffer: Framebuffer = mc.framebuffer): RenderPass {
    return gpuDevice
        .createCommandEncoder()
        .createRenderPass(
            framebuffer.colorAttachment,
            OptionalInt.empty(),
            framebuffer.depthAttachment.takeIf { framebuffer.useDepthAttachment },
            OptionalDouble.empty()
        )
}

/**
 * Pass [color] as a vec4 to the shader.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun RenderPass.setUniform(name: String, color: Color4b) {
    setUniform(name, color.r / 255f, color.g / 255f, color.b / 255f, color.a / 255f)
}

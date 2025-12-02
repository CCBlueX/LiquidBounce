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
import com.mojang.blaze3d.textures.GpuTextureView
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.utils.client.gpuDevice
import net.ccbluex.liquidbounce.utils.render.createGpuBuffer
import net.minecraft.client.gl.Framebuffer
import net.minecraft.client.render.BufferBuilder
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.util.BufferAllocator
import java.util.OptionalDouble
import java.util.OptionalInt
import java.util.function.Supplier

internal val trianglePosTexVertexBuffer: GpuBuffer =
    BufferAllocator(VertexFormats.POSITION_TEXTURE.vertexSize * 3).use { allocator ->
        BufferBuilder(allocator, DrawMode.TRIANGLES, VertexFormats.POSITION_TEXTURE).apply {
            vertex(-1f, -1f, 0f).texture(0f, 0f)
            vertex(3f, -1f, 0f).texture(2f, 0f)
            vertex(-1f, 3f, 0f).texture(0f, 2f)
        }.end().createGpuBuffer { "Triangle full screen position texture VBO" }
    }

fun RenderPass.drawFullScreenPositionTexture() {
    setVertexBuffer(0, trianglePosTexVertexBuffer)
    draw(0, 3)
}

private val RENDER_PASS_DEFAULT_LABEL = Supplier { LiquidBounce.CLIENT_NAME + " RenderPass" }

@JvmOverloads
fun Framebuffer.createRenderPass(
    labelGetter: Supplier<String> = RENDER_PASS_DEFAULT_LABEL,
    clearColor: OptionalInt = OptionalInt.empty(),
    clearDepth: OptionalDouble = OptionalDouble.empty(),
    useDepthAttachment: Boolean = true,
): RenderPass = newRenderPass(
    labelGetter,
    colorAttachmentView!!,
    clearColor,
    depthAttachmentView.takeIf { this.useDepthAttachment && useDepthAttachment },
    clearDepth,
)

/**
 * Color-only RenderPass.
 */
@JvmOverloads
fun GpuTextureView.createRenderPass(
    labelGetter: Supplier<String> = RENDER_PASS_DEFAULT_LABEL,
    clearColor: OptionalInt = OptionalInt.empty(),
): RenderPass = newRenderPass(labelGetter, colorAttachment = this, clearColor)

@Suppress("NOTHING_TO_INLINE")
private inline fun newRenderPass(
    labelGetter: Supplier<String> = RENDER_PASS_DEFAULT_LABEL,
    colorAttachment: GpuTextureView,
    clearColor: OptionalInt = OptionalInt.empty(),
    depthAttachment: GpuTextureView? = null,
    clearDepth: OptionalDouble = OptionalDouble.empty(),
): RenderPass = gpuDevice.createCommandEncoder().createRenderPass(
    labelGetter,
    colorAttachment,
    clearColor,
    depthAttachment,
    clearDepth,
)

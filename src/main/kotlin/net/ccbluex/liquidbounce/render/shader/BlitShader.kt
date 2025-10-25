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
package net.ccbluex.liquidbounce.render.shader

import com.mojang.blaze3d.buffers.BufferUsage
import com.mojang.blaze3d.opengl.GlStateManager
import com.mojang.blaze3d.vertex.VertexFormat
import net.ccbluex.liquidbounce.render.bind
import net.ccbluex.liquidbounce.render.draw
import net.ccbluex.liquidbounce.render.unbind
import net.ccbluex.liquidbounce.render.upload
import net.minecraft.client.gl.GlGpuBuffer
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexFormats

open class BlitShader(vertex: String, fragment: String, uniforms: Array<UniformProvider> = emptyArray()) :
    Shader(vertex, fragment, uniforms) {

    companion object {

        private val buffer: GlGpuBuffer

        init {
            val builder = Tessellator.getInstance()
            val bufferBuilder = builder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE)
            bufferBuilder.vertex(-1f, -1f, 0f).texture(0f, 0f)
            bufferBuilder.vertex(1f, -1f, 0f).texture(1f, 0f)
            bufferBuilder.vertex(1f, 1f, 0f).texture(1f, 1f)
            bufferBuilder.vertex(-1f, 1f, 0f).texture(0f, 1f)
            buffer = bufferBuilder.upload(BufferUsage.DYNAMIC_WRITE, 4 * VertexFormats.POSITION_TEXTURE.vertexSize)
        }

    }

    fun blit() {
        GlStateManager._disableBlend()
        use()
        buffer.bind()
        buffer.draw()
        buffer.unbind()
        stop()
        GlStateManager._enableBlend()
    }

}

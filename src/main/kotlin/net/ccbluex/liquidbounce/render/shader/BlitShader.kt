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

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gl.GlUsage
import net.minecraft.client.gl.VertexBuffer
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats

open class BlitShader(vertex: String, fragment: String, uniforms: Array<UniformProvider> = emptyArray()) :
    Shader(vertex, fragment, uniforms) {

    companion object {

        private var buffer = VertexBuffer(GlUsage.DYNAMIC_WRITE)

        init {
            val builder = Tessellator.getInstance()
            val bufferBuilder = builder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE)
            bufferBuilder.vertex(-1f, -1f, 0f).texture(0f, 0f)
            bufferBuilder.vertex(1f, -1f, 0f).texture(1f, 0f)
            bufferBuilder.vertex(1f, 1f, 0f).texture(1f, 1f)
            bufferBuilder.vertex(-1f, 1f, 0f).texture(0f, 1f)
            buffer.bind()
            buffer.upload(bufferBuilder.end())
            VertexBuffer.unbind()
        }

    }

    fun blit() {
        RenderSystem.disableBlend()
        use()
        buffer.bind()
        buffer.draw()
        VertexBuffer.unbind()
        stop()
        RenderSystem.enableBlend()
    }

}

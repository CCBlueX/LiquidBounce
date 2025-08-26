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
 *
 *
 */
package net.ccbluex.liquidbounce.render.shader

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.ShaderProgramKeys
import net.minecraft.client.gl.SimpleFramebuffer
import net.minecraft.client.gl.VertexBuffer
import org.joml.Matrix4f
import java.io.Closeable

class ScalableCanvas : Closeable {

    private val identity = Matrix4f()
    private val output = MinecraftClient.getInstance().framebuffer
    private val input = SimpleFramebuffer(output.textureWidth, output.textureHeight, false)
    private val shaderProgram by lazy { mc.shaderLoader.getOrCreateProgram(ShaderProgramKeys.POSITION_TEX_COLOR) }

    fun resize(width: Int, height: Int) {
        if (width() != width && height() != height && width > 0 && height > 0) {
            input.resize(width, height)
        }
    }

    fun width() = input.textureWidth

    fun height() = input.textureHeight

    fun write() = input.beginWrite(true)

    fun read() = input.beginRead()

    fun blit(buffer: VertexBuffer, alpha: Float = 1f) {
        output.beginWrite(true)

        RenderSystem.setShaderTexture(0, input.colorAttachment)
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha)

        RenderSystem.enableBlend()
        buffer.draw(identity, identity, shaderProgram)
    }

    override fun close() {
        input.delete()
    }

}

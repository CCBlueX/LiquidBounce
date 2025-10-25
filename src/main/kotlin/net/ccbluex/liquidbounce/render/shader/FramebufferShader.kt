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
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.VertexFormat
import net.ccbluex.liquidbounce.common.GlobalFramebuffer
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.render.bind
import net.ccbluex.liquidbounce.render.buffer.LiquidBounceFramebuffer
import net.ccbluex.liquidbounce.render.defaultBlendFunc
import net.ccbluex.liquidbounce.render.draw
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.unbind
import net.ccbluex.liquidbounce.render.upload
import net.minecraft.client.gl.GlGpuBuffer
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexFormats
import org.lwjgl.opengl.GL13
import java.io.Closeable

/**
 * @author ccetl
 */
open class FramebufferShader(vararg val shaders: Shader) : MinecraftShortcuts, Closeable {

    protected val framebuffers: Array<LiquidBounceFramebuffer>
    protected var buffer: GlGpuBuffer

    init {
        require(shaders.isNotEmpty())

        val width = mc.window.framebufferWidth
        val height = mc.window.framebufferHeight
        framebuffers = Array(shaders.size) {
            val framebuffer = LiquidBounceFramebuffer(width, height, false)
            framebuffer.clearColor = Color4b.TRANSPARENT
            framebuffer
        }

        val builder = Tessellator.getInstance()
        val bufferBuilder = builder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE)
        bufferBuilder.vertex(-1f, -1f, 0f).texture(0f, 0f)
        bufferBuilder.vertex(1f, -1f, 0f).texture(1f, 0f)
        bufferBuilder.vertex(1f, 1f, 0f).texture(1f, 1f)
        bufferBuilder.vertex(-1f, 1f, 0f).texture(0f, 1f)
        buffer = bufferBuilder.upload(BufferUsage.STATIC_WRITE, 4 * VertexFormats.POSITION_TEXTURE.vertexSize)
    }

    open fun prepare(clearFramebuffer: Boolean = true) {
        var doClearFramebuffer = clearFramebuffer

        val width = mc.window.framebufferWidth
        val height = mc.window.framebufferHeight
        framebuffers.forEachIndexed { index, framebuffer ->
            if (framebuffer.width != width || framebuffer.height != height) {
                if (index == 0) {
                    doClearFramebuffer = true
                }

                framebuffer.resize(width, height)
            }
        }

        framebuffers[0].beginWrite(true, doClearFramebuffer)

        GlobalFramebuffer.push(framebuffers[0])
    }

    open fun apply(popFramebufferStack: Boolean = true) {
        if (popFramebufferStack) {
            framebuffers[0].end()
        }

        val active = GlStateManager._getActiveTexture()

        GlStateManager._bindTexture(0)
        GlStateManager._disableDepthTest()
        enableBlend()

        buffer.bind()

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        shaders.forEachIndexed { i, shader ->
            val inputFramebuffer = framebuffers.getOrNull(i) ?: framebuffers.first()
            val outputFramebuffer = framebuffers.getOrNull(i + 1)

            outputFramebuffer?.beginWrite(true) ?: GlobalFramebuffer.pop()

            GlStateManager._activeTexture(GL13.GL_TEXTURE0 + i)
            GlStateManager._bindTexture(inputFramebuffer.colorAttachment)

            shader.use()
            buffer.draw()
            shader.stop()
        }

        buffer.unbind()

        endBlend()
        GlStateManager._enableDepthTest()
        GlStateManager._activeTexture(active)
    }

    protected open fun enableBlend() {
        GlStateManager._enableBlend()
        defaultBlendFunc()
    }

    protected open fun endBlend() {
    }

    fun render(drawAction: () -> Unit) {
        prepare()
        drawAction()
        apply()
    }

    override fun close() {
        shaders.forEach { it.close() }
        buffer.close()
        framebuffers.forEach { it.close() }
    }

}

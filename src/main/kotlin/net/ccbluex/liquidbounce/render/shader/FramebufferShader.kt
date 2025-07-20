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

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.common.GlobalFramebuffer
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.minecraft.client.gl.GlUsage
import net.minecraft.client.gl.SimpleFramebuffer
import net.minecraft.client.gl.VertexBuffer
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats
import org.lwjgl.opengl.GL13
import java.io.Closeable

/**
 * @author ccetl
 */
open class FramebufferShader(vararg val shaders: Shader) : MinecraftShortcuts, Closeable {

    protected val framebuffers = mutableListOf<SimpleFramebuffer>()
    protected var buffer = VertexBuffer(GlUsage.DYNAMIC_WRITE)

    init {
        val width = mc.window.framebufferWidth
        val height = mc.window.framebufferHeight
        shaders.forEach { _ ->
            val framebuffer = SimpleFramebuffer(width, height, false)
            framebuffer.setClearColor(0f, 0f, 0f, 0f)
            framebuffers.add(framebuffer)
        }

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

    fun prepare() {
        val width = mc.window.framebufferWidth
        val height = mc.window.framebufferHeight
        framebuffers.forEach {
            if (it.textureWidth != width || it.textureHeight != height) {
                it.resize(width, height)
            }
        }

        framebuffers[0].clear()
        framebuffers[0].beginWrite(true)

        GlobalFramebuffer.push(framebuffers[0])
    }

    open fun apply(popFramebufferStack: Boolean = true) {
        if (popFramebufferStack) {
            GlobalFramebuffer.pop()
        }

        val active = GlStateManager._getActiveTexture()

        GlStateManager._bindTexture(0)
        RenderSystem.disableDepthTest()
        enableBlend()

        buffer.bind()

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        shaders.forEachIndexed { i, shader ->
            val inputFramebuffer = framebuffers.getOrNull(i) ?: framebuffers.first()
            val outputFramebuffer = framebuffers.getOrNull(i + 1)

            outputFramebuffer?.clear()
            outputFramebuffer?.beginWrite(true) ?: mc.framebuffer.beginWrite(false)

            GlStateManager._activeTexture(GL13.GL_TEXTURE0 + i)
            GlStateManager._bindTexture(inputFramebuffer.colorAttachment)

            shader.use()
            buffer.draw()
            shader.stop()
        }

        VertexBuffer.unbind()

        endBlend()
        RenderSystem.enableDepthTest()
        GlStateManager._activeTexture(active)
    }

    protected open fun enableBlend() {
        RenderSystem.enableBlend()
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA)
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
        framebuffers.forEach { it.delete() }
    }

}

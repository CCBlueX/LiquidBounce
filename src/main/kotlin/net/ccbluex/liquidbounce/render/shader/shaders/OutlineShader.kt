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
package net.ccbluex.liquidbounce.render.shader.shaders

import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.shader.FramebufferShader
import net.ccbluex.liquidbounce.render.shader.Shader
import net.ccbluex.liquidbounce.render.shader.UniformProvider
import net.ccbluex.liquidbounce.utils.client.ImmutableHandle
import net.ccbluex.liquidbounce.utils.io.resourceToString
import net.minecraft.client.gl.Framebuffer
import net.minecraft.client.render.OutlineVertexConsumerProvider
import net.minecraft.client.util.Handle
import org.lwjgl.opengl.GL20

object OutlineShader : FramebufferShader(Shader(
    resourceToString("/resources/liquidbounce/shaders/sobel.vert"),
    resourceToString("/resources/liquidbounce/shaders/outline/entity_outline.frag"),
    arrayOf(
        UniformProvider("texture0") { pointer -> GL20.glUniform1i(pointer, 0) }
    )
)) {

    var dirty = false
    var vertexConsumerProvider = OutlineVertexConsumerProvider(mc.bufferBuilders.entityVertexConsumers)
    val handle: Handle<Framebuffer> = ImmutableHandle(framebuffers[0])
    private var outlineFbo: Handle<Framebuffer>? = null
    private var outlineFbo2: Framebuffer? = null

    fun update() {
        val width = mc.window.framebufferWidth
        val height = mc.window.framebufferHeight
        framebuffers.forEach {
            if (it.textureWidth != width || it.textureHeight != height) {
                it.resize(width, height)
            }
        }

        if (dirty) {
            framebuffers[0].clear()
        }

        dirty = false
    }

    fun setColor(color4b: Color4b) {
        vertexConsumerProvider.setColor(color4b.r, color4b.g, color4b.b, color4b.a)
    }

    fun draw() {
        outlineFbo2 = mc.worldRenderer.entityOutlineFramebuffer
        outlineFbo = mc.worldRenderer.framebufferSet.entityOutlineFramebuffer
        mc.worldRenderer.entityOutlineFramebuffer = framebuffers[0]
        mc.worldRenderer.framebufferSet.entityOutlineFramebuffer = handle
        vertexConsumerProvider.draw()
        mc.worldRenderer.entityOutlineFramebuffer = outlineFbo2
        mc.worldRenderer.framebufferSet.entityOutlineFramebuffer = outlineFbo
    }

}

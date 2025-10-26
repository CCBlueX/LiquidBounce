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

import net.ccbluex.liquidbounce.render.ClientShaders
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.shader.FramebufferShader
import net.ccbluex.liquidbounce.render.shader.Shader
import net.ccbluex.liquidbounce.render.shader.UniformProvider
import net.ccbluex.liquidbounce.utils.client.ImmutableHandle
import net.ccbluex.liquidbounce.render.buffer.LiquidBounceFramebuffer
import net.minecraft.client.render.OutlineVertexConsumerProvider
import net.minecraft.client.util.Handle
import org.lwjgl.opengl.GL20

object OutlineShader : FramebufferShader(Shader(
    ClientShaders[ClientShaders.SOBEL_VSH_ID]!!,
    ClientShaders[ClientShaders.OUTLINE_FSH_ID]!!,
    arrayOf(
        UniformProvider("texture0") { pointer -> GL20.glUniform1i(pointer, 0) }
    )
)) {

    var dirty = false
    val vertexConsumerProvider = OutlineVertexConsumerProvider(mc.bufferBuilders.entityVertexConsumers)
    val handle: Handle<LiquidBounceFramebuffer> = ImmutableHandle(framebuffers[0])

    fun prepare() {
        super.prepare(dirty)
        dirty = false
    }

    fun setColor(color4b: Color4b) {
        vertexConsumerProvider.setColor(color4b.r, color4b.g, color4b.b, color4b.a)
    }

    fun draw() {
        vertexConsumerProvider.draw()
    }

}

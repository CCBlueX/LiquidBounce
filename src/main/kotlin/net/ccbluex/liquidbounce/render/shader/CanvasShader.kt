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

import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gl.GlUniform
import net.minecraft.client.gl.GlUsage
import net.minecraft.client.gl.VertexBuffer
import net.minecraft.client.render.Tessellator
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats
import org.lwjgl.opengl.GL30

/**
 * A GLSL shader renderer. Takes a vertex and fragment shader and renders it to the canvas.
 *
 * Inspired from the GLSL Panorama Shader Mod
 * https://github.com/magistermaks/mod-glsl
 */
open class CanvasShader(vertex: String, fragment: String, uniforms: Array<UniformProvider> = emptyArray())
    : Shader(vertex, fragment, uniforms) {

    private var canvas = ScalableCanvas()
    private var buffer = VertexBuffer(GlUsage.DYNAMIC_WRITE)

    private val timeLocation: Int
    private val mouseLocation: Int
    private val resolutionLocation: Int

    private var time = 0f

    init {
        // bake buffer data
        val builder = Tessellator.getInstance()
        val buffer = builder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR)
        buffer.vertex(-1.0f, -1.0f, 1.0f).texture(0f, 0f)
            .color(1f, 1f, 1f, 1f)
        buffer.vertex(1.0f, -1.0f, 1.0f).texture(1f, 0f)
            .color(1f, 1f, 1f, 1f)
        buffer.vertex(1.0f, 1.0f, 1.0f).texture(1f, 1f)
            .color(1f, 1f, 1f, 1f)
        buffer.vertex(-1.0f, 1.0f, 1.0f).texture(0f, 1f)
            .color(1f, 1f, 1f, 1f)

        this.buffer.bind()
        this.buffer.upload(buffer.end())
        VertexBuffer.unbind()

        // get uniform pointers
        timeLocation = GlUniform.getUniformLocation(program, "time")
        mouseLocation = GlUniform.getUniformLocation(program, "mouse")
        resolutionLocation = GlUniform.getUniformLocation(program, "resolution")
    }

    fun draw(mouseX: Int, mouseY: Int, delta: Float) {
        super.use()

        canvas.resize(mc.window.framebufferWidth, mc.window.framebufferHeight)
        canvas.write()

        // update uniforms
        if (timeLocation != -1) {
            GL30.glUniform1f(timeLocation, time)
            time += (delta / 10f)
        }

        if (mouseLocation != -1) {
            GL30.glUniform2f(mouseLocation, mouseX.toFloat(), mouseY.toFloat())
        }

        if (resolutionLocation != -1) {
            GL30.glUniform2f(resolutionLocation, canvas.width().toFloat(), canvas.height().toFloat())
        }

        // draw
        buffer.bind()
        buffer.draw()
        canvas.blit(buffer)
    }

    override fun close() {
        super.close()
        buffer.close()
        canvas.close()
    }

}

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
import net.minecraft.client.gl.ShaderProgram
import net.minecraft.client.gl.ShaderProgramDefinition
import net.minecraft.client.render.RenderPhase
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.util.Window
import org.joml.Matrix4f

class CustomShaderProgram(val shader: Shader) : ShaderProgram(shader.program) {

    init {
        // keep this?
        set(emptyList<ShaderProgramDefinition.Uniform>(), emptyList<ShaderProgramDefinition.Sampler>())
    }

    override fun initializeUniforms(
        drawMode: VertexFormat.DrawMode,
        viewMatrix: Matrix4f,
        projectionMatrix: Matrix4f,
        window: Window
    ) {
        super.initializeUniforms(drawMode, viewMatrix, projectionMatrix, window)

//        currentProjectionMatrix = projectionMatrix
//        currentModelViewMatrix = viewMatrix
    }

    override fun bind() {
        shader.use()
    }

}

class CustomShaderProgramPhase(val shader: Shader) : RenderPhase.ShaderProgram() {

    private val shaderProgram = CustomShaderProgram(shader)

    override fun startDrawing() {
        RenderSystem.setShader(shaderProgram)
    }
}

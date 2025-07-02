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

class CustomShaderProgram(val shader: Shader) : ShaderProgram(shader.program) {

    init {
        this.set(mutableListOf<ShaderProgramDefinition.Uniform>(
            ShaderProgramDefinition.Uniform("ModelViewMat", "matrix4x4", 16, listOf(
                1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f
            )),
            ShaderProgramDefinition.Uniform("ProjMat", "matrix4x4", 16, listOf(
                1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f
            )),
            ShaderProgramDefinition.Uniform("ColorModulator", "float", 4, listOf(
                1.0f, 1.0f, 1.0f, 1.0f
            ))
        ), mutableListOf(
            ShaderProgramDefinition.Sampler("Sampler0")
        ))
    }

}

class CustomShaderProgramPhase(val shader: Shader) : RenderPhase.ShaderProgram() {

    private val shaderProgram = CustomShaderProgram(shader)

    override fun startDrawing() {
        RenderSystem.setShader(shaderProgram)
    }

}

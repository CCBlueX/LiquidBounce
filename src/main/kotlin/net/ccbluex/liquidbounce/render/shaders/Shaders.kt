/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

package net.ccbluex.liquidbounce.render.shaders

import net.ccbluex.liquidbounce.render.engine.RenderEngine
import net.ccbluex.liquidbounce.render.engine.ShaderProgram
import net.ccbluex.liquidbounce.render.engine.memory.PositionColorUVVertexFormat
import net.ccbluex.liquidbounce.render.engine.memory.PositionColorVertexFormat
import net.ccbluex.liquidbounce.utils.io.resourceToString
import net.ccbluex.liquidbounce.utils.math.Mat4

/**
 * Here, all common shaders are controlled.
 */
object Shaders {

    /**
     * Initializes all common shaders. Please only call this function if OpenGL 2.0 is supported.
     *
     * @throws IllegalStateException When one of the program fails to initialize
     */
    fun init() {
        // Don't try to load shaders if they are not supported
        if (!RenderEngine.openglLevel.supportsShaders()) {
            return
        }

        try {
            InstancedColoredPrimitiveShader
            ColoredPrimitiveShader
            TexturedPrimitiveShader
        } catch (e: Exception) {
            throw IllegalStateException("Failed to initialize common shader programs", e)
        }
    }
}

abstract class ShaderHandler {
    /**
     * Binds the shader
     *
     * @param mvpMatrix The model-view-projection matrix to use
     */
    abstract fun bind(mvpMatrix: Mat4)
}

/**
 * Compatible with [PositionColorVertexFormat]
 */
object ColoredPrimitiveShader : ShaderHandler() {
    private var shaderProgram: ShaderProgram
    private val mvpMatrixUniformLocation: Int

    init {
        val shaderProgram = ShaderProgram(
            resourceToString("/assets/liquidbounce/shaders/primitive3d.vert"),
            resourceToString("/assets/liquidbounce/shaders/primitive3d.frag")
        )

        shaderProgram.bindAttribLocation("in_pos", 0)
        shaderProgram.bindAttribLocation("in_color", 1)

        mvpMatrixUniformLocation = shaderProgram.getUniformLocation("mvp_matrix")

        ColoredPrimitiveShader.shaderProgram = shaderProgram
    }

    override fun bind(mvpMatrix: Mat4) {
        shaderProgram.use()

        mvpMatrix.putToUniform(mvpMatrixUniformLocation)
    }
}

/**
 * Compatible with [PositionColorUVVertexFormat]
 */
object TexturedPrimitiveShader : ShaderHandler() {
    private var shaderProgram: ShaderProgram
    private val mvpMatrixUniformLocation: Int

    init {
        val shaderProgram = ShaderProgram(
            resourceToString("/assets/liquidbounce/shaders/textured3d.vert"),
            resourceToString("/assets/liquidbounce/shaders/textured3d.frag")
        )

        shaderProgram.bindAttribLocation("in_pos", 0)
        shaderProgram.bindAttribLocation("in_color", 1)
        shaderProgram.bindAttribLocation("in_uv", 2)

        mvpMatrixUniformLocation = shaderProgram.getUniformLocation("mvp_matrix")

        TexturedPrimitiveShader.shaderProgram = shaderProgram
    }

    override fun bind(mvpMatrix: Mat4) {
        shaderProgram.use()

        mvpMatrix.putToUniform(mvpMatrixUniformLocation)
    }
}

/**
 * Used for instanced rendering of [PositionColorVertexFormat]
 */
object InstancedColoredPrimitiveShader : ShaderHandler() {
    private var shaderProgram: ShaderProgram
    private val mvpMatrixUniformLocation: Int

    init {
        val shaderProgram = ShaderProgram(
            resourceToString("/assets/liquidbounce/shaders/instanced_primitive3d.vert"),
            resourceToString("/assets/liquidbounce/shaders/primitive3d.frag")
        )

        shaderProgram.bindAttribLocation("in_pos", 0)
        shaderProgram.bindAttribLocation("in_color", 1)
        shaderProgram.bindAttribLocation("instance_pos", 2)
        shaderProgram.bindAttribLocation("instance_color", 3)

        mvpMatrixUniformLocation = shaderProgram.getUniformLocation("mvp_matrix")

        InstancedColoredPrimitiveShader.shaderProgram = shaderProgram
    }

    override fun bind(mvpMatrix: Mat4) {
        shaderProgram.use()

        mvpMatrix.putToUniform(mvpMatrixUniformLocation)
    }
}

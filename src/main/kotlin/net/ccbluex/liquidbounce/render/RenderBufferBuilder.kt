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
@file:Suppress("LongParameterList")

package net.ccbluex.liquidbounce.render

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.type.UV2f
import net.ccbluex.liquidbounce.render.engine.type.Vec3
import net.minecraft.client.gl.ShaderProgramKey
import net.minecraft.client.gl.ShaderProgramKeys
import net.minecraft.client.render.*
import net.minecraft.client.render.VertexFormat.DrawMode
import net.minecraft.util.math.Position


/**
 * A utility class for drawing shapes in batches.
 *
 * Not sync, not send. Not thread-safe at all.
 *
 * This should only be used on render thread.
 */
class RenderBufferBuilder<I : VertexInputType> private constructor(
    private val drawMode: DrawMode,
    private val vertexFormat: I,
    private val tessellator: Tessellator,
    internal val vertexConsumer: BufferBuilder,
) {

    constructor(
        drawMode: DrawMode,
        vertexFormat: I,
        tessellator: Tessellator,
    ) : this(
        drawMode,
        vertexFormat,
        tessellator,
        tessellator.begin(drawMode, vertexFormat.vertexFormat),
    )

    fun draw() {
        val built = vertexConsumer.endNullable() ?: return

        RenderSystem.setShader(vertexFormat.shaderProgram)

        BufferRenderer.drawWithGlobalProgram(built)
        tessellator.clear()
    }

    fun reset() {
        vertexConsumer.endNullable()
    }
}

fun RenderBufferBuilder<VertexInputType.PosTexColor>.drawQuad(
    env: RenderEnvironment,
    pos1: Position,
    uv1: UV2f,
    pos2: Position,
    uv2: UV2f,
    color: Color4b
) {
    val matrix = env.currentMvpMatrix

    // Draw the vertices of the box
    with(vertexConsumer) {
        vertex(matrix, pos1.x.toFloat(), pos2.y.toFloat(), pos1.z.toFloat())
            .texture(uv1.u, uv2.v)
            .color(color.toARGB())
        vertex(matrix, pos2.x.toFloat(), pos2.y.toFloat(), pos2.z.toFloat())
            .texture(uv2.u, uv2.v)
            .color(color.toARGB())
        vertex(matrix, pos2.x.toFloat(), pos1.y.toFloat(), pos2.z.toFloat())
            .texture(uv2.u, uv1.v)
            .color(color.toARGB())
        vertex(matrix, pos1.x.toFloat(), pos1.y.toFloat(), pos1.z.toFloat())
            .texture(uv1.u, uv1.v)
            .color(color.toARGB())
    }
}

context(env: RenderEnvironment)
fun RenderBufferBuilder<VertexInputType.PosColor>.drawLine(
    pos1: Vec3,
    pos2: Vec3,
    color: Color4b
) {
    val matrix = env.currentMvpMatrix

    // Draw the vertices of the box
    with(vertexConsumer) {
        vertex(matrix, pos1.x, pos1.y, pos1.z).color(color.toARGB())
        vertex(matrix, pos2.x, pos2.y, pos2.z).color(color.toARGB())
    }
}

sealed interface VertexInputType {
    val vertexFormat: VertexFormat
    val shaderProgram: ShaderProgramKey

    object Pos : VertexInputType {
        override val vertexFormat: VertexFormat
            get() = VertexFormats.POSITION
        override val shaderProgram: ShaderProgramKey
            get() = ShaderProgramKeys.POSITION
    }

    object PosColor : VertexInputType {
        override val vertexFormat: VertexFormat
            get() = VertexFormats.POSITION_COLOR
        override val shaderProgram: ShaderProgramKey
            get() = ShaderProgramKeys.POSITION_COLOR
    }

    object PosTexColor : VertexInputType {
        override val vertexFormat: VertexFormat
            get() = VertexFormats.POSITION_TEXTURE_COLOR
        override val shaderProgram: ShaderProgramKey
            get() = ShaderProgramKeys.POSITION_TEX_COLOR
    }

}

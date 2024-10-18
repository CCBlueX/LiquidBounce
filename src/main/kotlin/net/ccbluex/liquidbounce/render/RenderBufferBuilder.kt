/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.UV2f
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.minecraft.client.gl.ShaderProgram
import net.minecraft.client.render.*
import net.minecraft.client.render.VertexFormat.DrawMode
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d

/**
 * A utility class for drawing shapes in batches.
 *
 * Not sync, not send. Not thread-safe at all.
 */
class RenderBufferBuilder<I : VertexInputType>(
    private val drawMode: DrawMode,
    private val vertexFormat: I,
    private val tesselator: Tessellator
) {
    // Begin drawing lines with position format
    val buffer: BufferBuilder = tesselator.begin(drawMode, vertexFormat.vertexFormat)

    /**
     * Function to draw a solid box using the specified [box].
     *
     * @param box The bounding box of the box.
     */
    fun drawBox(env: RenderEnvironment, box: Box, useOutlineVertices: Boolean = false, color: Color4b? = null) {
        val matrix = env.currentMvpMatrix

        val vertexPositions =
            if (useOutlineVertices)
                box.outlineVertexPositions
            else
                box.vertexPositions

        // Draw the vertices of the box
        vertexPositions.forEach { (x, y, z) ->
            val bb = buffer.vertex(matrix, x, y, z)

            if (color != null) {
                bb.color(color.toRGBA())
            }
        }
    }

    fun draw() {
        val built = buffer.endNullable() ?: return

        RenderSystem.setShader { vertexFormat.shaderProgram }

        BufferRenderer.drawWithGlobalProgram(built)
        tesselator.clear()
    }

    fun reset() {
        buffer.endNullable()
    }

    companion object {
        val TESSELATOR_A: Tessellator = Tessellator(0x200000)
        val TESSELATOR_B: Tessellator = Tessellator(0x200000)
    }
}

class BoxRenderer private constructor(private val env: WorldRenderEnvironment) {
    private val faceRenderer = RenderBufferBuilder(
        DrawMode.QUADS,
        VertexInputType.PosColor,
        RenderBufferBuilder.TESSELATOR_A
    )
    private val outlinesRenderer = RenderBufferBuilder(
        DrawMode.DEBUG_LINES,
        VertexInputType.PosColor,
        RenderBufferBuilder.TESSELATOR_B
    )

    companion object {
        /**
         * Draws colored boxes. Renders automatically
         */
        fun drawWith(env: WorldRenderEnvironment, fn: BoxRenderer.() -> Unit) {
            val renderer = BoxRenderer(env)

            try {
                fn(renderer)
            } finally {
                renderer.draw()
            }
        }
    }

    fun drawBox(box: Box, faceColor: Color4b, outlineColor: Color4b? = null) {
        faceRenderer.drawBox(env, box, color = faceColor)

        if (outlineColor != null) {
            outlinesRenderer.drawBox(env, box, useOutlineVertices = true, color = outlineColor)
        }
    }

    private fun draw() {
        faceRenderer.draw()
        outlinesRenderer.draw()
    }

}

internal val Box.vertexPositions: List<Vec3>
    get() = listOf(
        Vec3(minX, minY, minZ),
        Vec3(maxX, minY, minZ),
        Vec3(maxX, minY, maxZ),
        Vec3(minX, minY, maxZ),
        Vec3(minX, maxY, minZ),
        Vec3(minX, maxY, maxZ),
        Vec3(maxX, maxY, maxZ),
        Vec3(maxX, maxY, minZ),
        Vec3(minX, minY, minZ),
        Vec3(minX, maxY, minZ),
        Vec3(maxX, maxY, minZ),
        Vec3(maxX, minY, minZ),
        Vec3(maxX, minY, minZ),
        Vec3(maxX, maxY, minZ),
        Vec3(maxX, maxY, maxZ),
        Vec3(maxX, minY, maxZ),
        Vec3(minX, minY, maxZ),
        Vec3(maxX, minY, maxZ),
        Vec3(maxX, maxY, maxZ),
        Vec3(minX, maxY, maxZ),
        Vec3(minX, minY, minZ),
        Vec3(minX, minY, maxZ),
        Vec3(minX, maxY, maxZ),
        Vec3(minX, maxY, minZ)
    )

internal val Box.outlineVertexPositions: List<Vec3>
    get() = listOf(
        Vec3(minX, minY, minZ),
        Vec3(maxX, minY, minZ),
        Vec3(maxX, minY, minZ),
        Vec3(maxX, minY, maxZ),
        Vec3(maxX, minY, maxZ),
        Vec3(minX, minY, maxZ),
        Vec3(minX, minY, maxZ),
        Vec3(minX, minY, minZ),
        Vec3(minX, minY, minZ),
        Vec3(minX, maxY, minZ),
        Vec3(maxX, minY, minZ),
        Vec3(maxX, maxY, minZ),
        Vec3(maxX, minY, maxZ),
        Vec3(maxX, maxY, maxZ),
        Vec3(minX, minY, maxZ),
        Vec3(minX, maxY, maxZ),
        Vec3(minX, maxY, minZ),
        Vec3(maxX, maxY, minZ),
        Vec3(maxX, maxY, minZ),
        Vec3(maxX, maxY, maxZ),
        Vec3(maxX, maxY, maxZ),
        Vec3(minX, maxY, maxZ),
        Vec3(minX, maxY, maxZ),
        Vec3(minX, maxY, minZ)
    )

fun RenderEnvironment.drawSolidBox(consumer: VertexConsumer, box: Box, color: Color4b) {
    val matrix = currentMvpMatrix

    // Draw the vertices of the box
    box.vertexPositions.forEach { (x, y, z) ->
        consumer.vertex(matrix, x, y, z).color(color.toRGBA())
    }
}

fun RenderBufferBuilder<VertexInputType.PosTexColor>.drawQuad(
    env: RenderEnvironment,
    pos1: Vec3d,
    uv1: UV2f,
    pos2: Vec3d,
    uv2: UV2f,
    color: Color4b
) {
    val matrix = env.currentMvpMatrix

    // Draw the vertices of the box
    with(buffer) {
        vertex(matrix, pos1.x.toFloat(), pos2.y.toFloat(), pos1.z.toFloat())
            .texture(uv1.u, uv2.v)
            .color(color.toRGBA())
        vertex(matrix, pos2.x.toFloat(), pos2.y.toFloat(), pos2.z.toFloat())
            .texture(uv2.u, uv2.v)
            .color(color.toRGBA())
        vertex(matrix, pos2.x.toFloat(), pos1.y.toFloat(), pos2.z.toFloat())
            .texture(uv2.u, uv1.v)
            .color(color.toRGBA())
        vertex(matrix, pos1.x.toFloat(), pos1.y.toFloat(), pos1.z.toFloat())
            .texture(uv1.u, uv1.v)
            .color(color.toRGBA())
    }
}

fun RenderBufferBuilder<VertexInputType.Pos>.drawQuad(
    env: RenderEnvironment,
    pos1: Vec3,
    pos2: Vec3,
) {
    val matrix = env.currentMvpMatrix

    // Draw the vertices of the box
    with(buffer) {
        vertex(matrix, pos1.x, pos2.y, pos1.z)
        vertex(matrix, pos2.x, pos2.y, pos2.z)
        vertex(matrix, pos2.x, pos1.y, pos2.z)
        vertex(matrix, pos1.x, pos1.y, pos1.z)
    }
}

fun RenderBufferBuilder<VertexInputType.Pos>.drawQuadOutlines(
    env: RenderEnvironment,
    pos1: Vec3,
    pos2: Vec3,
) {
    val matrix = env.currentMvpMatrix

    // Draw the vertices of the box
    with(buffer) {
        vertex(matrix, pos1.x, pos1.y, pos1.z)
        vertex(matrix, pos1.x, pos2.y, pos1.z)

        vertex(matrix, pos1.x, pos2.y, pos1.z)
        vertex(matrix, pos2.x, pos2.y, pos1.z)

        vertex(matrix, pos2.x, pos1.y, pos1.z)
        vertex(matrix, pos2.x, pos2.y, pos1.z)

        vertex(matrix, pos1.x, pos1.y, pos1.z)
        vertex(matrix, pos2.x, pos1.y, pos1.z)
    }
}

fun RenderBufferBuilder<VertexInputType.PosColor>.drawLine(
    env: RenderEnvironment,
    pos1: Vec3,
    pos2: Vec3,
    color: Color4b
) {
    val matrix = env.currentMvpMatrix

    // Draw the vertices of the box
    with(buffer) {
        vertex(matrix, pos1.x, pos1.y, pos1.z).color(color.toRGBA())
        vertex(matrix, pos2.x, pos2.y, pos2.z).color(color.toRGBA())
    }
}

sealed class VertexInputType {
    abstract val vertexFormat: VertexFormat
    abstract val shaderProgram: ShaderProgram

    object Pos : VertexInputType() {
        override val vertexFormat: VertexFormat
            get() = VertexFormats.POSITION
        override val shaderProgram: ShaderProgram
            get() = GameRenderer.getPositionProgram()!!
    }

    object PosColor : VertexInputType() {
        override val vertexFormat: VertexFormat
            get() = VertexFormats.POSITION_COLOR
        override val shaderProgram: ShaderProgram
            get() = GameRenderer.getPositionColorProgram()!!
    }

    object PosTexColor : VertexInputType() {
        override val vertexFormat: VertexFormat
            get() = VertexFormats.POSITION_TEXTURE_COLOR
        override val shaderProgram: ShaderProgram
            get() = GameRenderer.getPositionTexColorProgram()!!
    }
}

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
class RenderBufferBuilder<I: VertexInputType>(
    private val drawMode: DrawMode,
    private val vertexFormat: I,
    private val tesselator: Tessellator
) {
    val bufferBuilder: BufferBuilder = tesselator.buffer

    init {
        // Begin drawing lines with position format
        bufferBuilder.begin(drawMode, vertexFormat.vertexFormat)
    }

    /**
     * Function to draw a solid box using the specified [box].
     *
     * @param box The bounding box of the box.
     */
    fun drawBox(env: RenderEnvironment, box: Box, useOutlineVertices: Boolean = false) {
        val matrix = env.currentMvpMatrix

        val vertexPositions =
            if(useOutlineVertices)
                boxOutlineVertexPositions(box)
            else
                boxVertexPositions(box)

        // Draw the vertices of the box
        vertexPositions.forEach { (x, y, z) ->
            bufferBuilder.vertex(matrix, x, y, z).next()
        }
    }

    private fun boxVertexPositions(box: Box): List<Vec3> {
        val vertices = listOf(
            Vec3(box.minX, box.minY, box.minZ),
            Vec3(box.maxX, box.minY, box.minZ),
            Vec3(box.maxX, box.minY, box.maxZ),
            Vec3(box.minX, box.minY, box.maxZ),
            Vec3(box.minX, box.maxY, box.minZ),
            Vec3(box.minX, box.maxY, box.maxZ),
            Vec3(box.maxX, box.maxY, box.maxZ),
            Vec3(box.maxX, box.maxY, box.minZ),
            Vec3(box.minX, box.minY, box.minZ),
            Vec3(box.minX, box.maxY, box.minZ),
            Vec3(box.maxX, box.maxY, box.minZ),
            Vec3(box.maxX, box.minY, box.minZ),
            Vec3(box.maxX, box.minY, box.minZ),
            Vec3(box.maxX, box.maxY, box.minZ),
            Vec3(box.maxX, box.maxY, box.maxZ),
            Vec3(box.maxX, box.minY, box.maxZ),
            Vec3(box.minX, box.minY, box.maxZ),
            Vec3(box.maxX, box.minY, box.maxZ),
            Vec3(box.maxX, box.maxY, box.maxZ),
            Vec3(box.minX, box.maxY, box.maxZ),
            Vec3(box.minX, box.minY, box.minZ),
            Vec3(box.minX, box.minY, box.maxZ),
            Vec3(box.minX, box.maxY, box.maxZ),
            Vec3(box.minX, box.maxY, box.minZ)
        )
        return vertices
    }

    private fun boxOutlineVertexPositions(box: Box): List<Vec3> {
        val vertices = listOf(
            Vec3(box.minX, box.minY, box.minZ),
            Vec3(box.maxX, box.minY, box.minZ),
            Vec3(box.maxX, box.minY, box.minZ),
            Vec3(box.maxX, box.minY, box.maxZ),
            Vec3(box.maxX, box.minY, box.maxZ),
            Vec3(box.minX, box.minY, box.maxZ),
            Vec3(box.minX, box.minY, box.maxZ),
            Vec3(box.minX, box.minY, box.minZ),
            Vec3(box.minX, box.minY, box.minZ),
            Vec3(box.minX, box.maxY, box.minZ),
            Vec3(box.maxX, box.minY, box.minZ),
            Vec3(box.maxX, box.maxY, box.minZ),
            Vec3(box.maxX, box.minY, box.maxZ),
            Vec3(box.maxX, box.maxY, box.maxZ),
            Vec3(box.minX, box.minY, box.maxZ),
            Vec3(box.minX, box.maxY, box.maxZ),
            Vec3(box.minX, box.maxY, box.minZ),
            Vec3(box.maxX, box.maxY, box.minZ),
            Vec3(box.maxX, box.maxY, box.minZ),
            Vec3(box.maxX, box.maxY, box.maxZ),
            Vec3(box.maxX, box.maxY, box.maxZ),
            Vec3(box.minX, box.maxY, box.maxZ),
            Vec3(box.minX, box.maxY, box.maxZ),
            Vec3(box.minX, box.maxY, box.minZ)
        )
        return vertices
    }

    fun draw() {
        if (bufferBuilder.isBatchEmpty) {
            tesselator.buffer.end()

            return
        }

        RenderSystem.setShader { vertexFormat.shaderProgram }

        tesselator.draw()
        tesselator.buffer.reset()
    }

    fun reset() {
        tesselator.buffer.end()
    }

    companion object {
        val TESSELATOR_A: Tessellator = Tessellator(0x200000)
        val TESSELATOR_B: Tessellator = Tessellator(0x200000)
    }
}

class BoxesRenderer {
    private val faceRenderer = RenderBufferBuilder(
        DrawMode.QUADS,
        VertexInputType.Pos,
        RenderBufferBuilder.TESSELATOR_A
    )
    private val outlinesRenderer = RenderBufferBuilder(
            DrawMode.DEBUG_LINES,
            VertexInputType.Pos,
            RenderBufferBuilder.TESSELATOR_B
    )

    fun drawBox(env: RenderEnvironment, box: Box, outline: Boolean) {
        faceRenderer.drawBox(env, box)
        // This can still be optimized since there will be a lot of useless matrix muls...
        if(outline) {
            outlinesRenderer.drawBox(env, box, true)
        }
    }

    fun draw(env: RenderEnvironment, faceColor: Color4b, outlineColor: Color4b) {
        env.withColor(faceColor) {
            faceRenderer.draw()
        }
        env.withColor(outlineColor) {
            outlinesRenderer.draw()
        }
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
    with(bufferBuilder) {
        vertex(matrix, pos1.x.toFloat(), pos2.y.toFloat(), pos1.z.toFloat())
            .texture(uv1.u, uv2.v)
            .color(color.toRGBA())
            .next()
        vertex(matrix, pos2.x.toFloat(), pos2.y.toFloat(), pos2.z.toFloat())
            .texture(uv2.u, uv2.v)
            .color(color.toRGBA())
            .next()
        vertex(matrix, pos2.x.toFloat(), pos1.y.toFloat(), pos2.z.toFloat())
            .texture(uv2.u, uv1.v)
            .color(color.toRGBA())
            .next()
        vertex(matrix, pos1.x.toFloat(), pos1.y.toFloat(), pos1.z.toFloat())
            .texture(uv1.u, uv1.v)
            .color(color.toRGBA())
            .next()
    }
}

fun RenderBufferBuilder<VertexInputType.Pos>.drawQuad(
    env: RenderEnvironment,
    pos1: Vec3,
    pos2: Vec3,
) {
    val matrix = env.currentMvpMatrix

    // Draw the vertices of the box
    with(bufferBuilder) {
        vertex(matrix, pos1.x, pos2.y, pos1.z).next()
        vertex(matrix, pos2.x, pos2.y, pos2.z).next()
        vertex(matrix, pos2.x, pos1.y, pos2.z).next()
        vertex(matrix, pos1.x, pos1.y, pos1.z).next()
    }
}

fun RenderBufferBuilder<VertexInputType.Pos>.drawQuadOutlines(
    env: RenderEnvironment,
    pos1: Vec3,
    pos2: Vec3,
) {
    val matrix = env.currentMvpMatrix

    // Draw the vertices of the box
    with(bufferBuilder) {
        vertex(matrix, pos1.x, pos1.y, pos1.z).next()
        vertex(matrix, pos1.x, pos2.y, pos1.z).next()

        vertex(matrix, pos1.x, pos2.y, pos1.z).next()
        vertex(matrix, pos2.x, pos2.y, pos1.z).next()

        vertex(matrix, pos2.x, pos1.y, pos1.z).next()
        vertex(matrix, pos2.x, pos2.y, pos1.z).next()

        vertex(matrix, pos1.x, pos1.y, pos1.z).next()
        vertex(matrix, pos2.x, pos1.y, pos1.z).next()
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
    with(bufferBuilder) {
        vertex(matrix, pos1.x, pos1.y, pos1.z).color(color.toRGBA()).next()
        vertex(matrix, pos2.x, pos2.y, pos2.z).color(color.toRGBA()).next()
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

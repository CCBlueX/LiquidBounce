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

const val FACE_DOWN = (1 shl 0) or (1 shl 1) or (1 shl 2) or (1 shl 3)
const val FACE_UP = (1 shl 4) or (1 shl 5) or (1 shl 6) or (1 shl 7)
const val FACE_NORTH = (1 shl 8) or (1 shl 9) or (1 shl 10) or (1 shl 11)
const val FACE_EAST = (1 shl 12) or (1 shl 13) or (1 shl 14) or (1 shl 15)
const val FACE_SOUTH = (1 shl 16) or (1 shl 17) or (1 shl 18) or (1 shl 19)
const val FACE_WEST = (1 shl 20) or (1 shl 21) or (1 shl 22) or (1 shl 23)

const val EDGE_NORTH_DOWN = ((1 shl 0) or (1 shl (1)))
const val EDGE_EAST_DOWN = ((1 shl 2) or (1 shl (3)))
const val EDGE_SOUTH_DOWN = ((1 shl 4) or (1 shl (5)))
const val EDGE_WEST_DOWN = ((1 shl 6) or (1 shl (7)))

const val EDGE_NORTH_WEST = ((1 shl 8) or (1 shl (9)))
const val EDGE_NORTH_EAST = ((1 shl 10) or (1 shl (11)))
const val EDGE_SOUTH_EAST = ((1 shl 12) or (1 shl (13)))
const val EDGE_SOUTH_WEST = ((1 shl 14) or (1 shl (15)))

const val EDGE_NORTH_UP = ((1 shl 16) or (1 shl (17)))
const val EDGE_EAST_UP = ((1 shl 18) or (1 shl (19)))
const val EDGE_SOUTH_UP = ((1 shl 20) or (1 shl (21)))
const val EDGE_WEST_UP = ((1 shl 22) or (1 shl (23)))


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
    fun drawBox(
        env: RenderEnvironment,
        box: Box,
        useOutlineVertices: Boolean = false,
        color: Color4b? = null,
        verticesToUse: Int = -1
    ) {
        val matrix = env.currentMvpMatrix

        val vertexPositions = if (useOutlineVertices) {
            boxOutlineVertexPositions(box)
        } else {
            boxVertexPositions(box)
        }

        val check = verticesToUse != -1

        // Draw the vertices of the box
        for (i in vertexPositions.indices) {
            if (check && (verticesToUse and (1 shl i)) != 0) {
                continue
            }

            val (x, y, z) = vertexPositions[i]
            val bb = buffer.vertex(matrix, x, y, z)

            if (color != null) {
                bb.color(color.toARGB())
            }
        }
    }

    private fun boxVertexPositions(box: Box): Array<Vec3> {
        return arrayOf(
            // down
            Vec3(box.minX, box.minY, box.minZ),
            Vec3(box.maxX, box.minY, box.minZ),
            Vec3(box.maxX, box.minY, box.maxZ),
            Vec3(box.minX, box.minY, box.maxZ),

            // up
            Vec3(box.minX, box.maxY, box.minZ),
            Vec3(box.minX, box.maxY, box.maxZ),
            Vec3(box.maxX, box.maxY, box.maxZ),
            Vec3(box.maxX, box.maxY, box.minZ),

            // north
            Vec3(box.minX, box.minY, box.minZ),
            Vec3(box.minX, box.maxY, box.minZ),
            Vec3(box.maxX, box.maxY, box.minZ),
            Vec3(box.maxX, box.minY, box.minZ),

            // east
            Vec3(box.maxX, box.minY, box.minZ),
            Vec3(box.maxX, box.maxY, box.minZ),
            Vec3(box.maxX, box.maxY, box.maxZ),
            Vec3(box.maxX, box.minY, box.maxZ),

            // south
            Vec3(box.minX, box.minY, box.maxZ),
            Vec3(box.maxX, box.minY, box.maxZ),
            Vec3(box.maxX, box.maxY, box.maxZ),
            Vec3(box.minX, box.maxY, box.maxZ),

            // west
            Vec3(box.minX, box.minY, box.minZ),
            Vec3(box.minX, box.minY, box.maxZ),
            Vec3(box.minX, box.maxY, box.maxZ),
            Vec3(box.minX, box.maxY, box.minZ)
        )
    }

    private fun boxOutlineVertexPositions(box: Box): Array<Vec3> {
        return arrayOf(
            // down north
            Vec3(box.minX, box.minY, box.minZ),
            Vec3(box.maxX, box.minY, box.minZ),

            // down east
            Vec3(box.maxX, box.minY, box.minZ),
            Vec3(box.maxX, box.minY, box.maxZ),

            // down south
            Vec3(box.maxX, box.minY, box.maxZ),
            Vec3(box.minX, box.minY, box.maxZ),

            // down west
            Vec3(box.minX, box.minY, box.maxZ),
            Vec3(box.minX, box.minY, box.minZ),

            // north west
            Vec3(box.minX, box.minY, box.minZ),
            Vec3(box.minX, box.maxY, box.minZ),

            // north east
            Vec3(box.maxX, box.minY, box.minZ),
            Vec3(box.maxX, box.maxY, box.minZ),

            // south east
            Vec3(box.maxX, box.minY, box.maxZ),
            Vec3(box.maxX, box.maxY, box.maxZ),

            // south west
            Vec3(box.minX, box.minY, box.maxZ),
            Vec3(box.minX, box.maxY, box.maxZ),

            // up north
            Vec3(box.minX, box.maxY, box.minZ),
            Vec3(box.maxX, box.maxY, box.minZ),

            // up east
            Vec3(box.maxX, box.maxY, box.minZ),
            Vec3(box.maxX, box.maxY, box.maxZ),

            // up south
            Vec3(box.maxX, box.maxY, box.maxZ),
            Vec3(box.minX, box.maxY, box.maxZ),

            // up west
            Vec3(box.minX, box.maxY, box.maxZ),
            Vec3(box.minX, box.maxY, box.minZ)
        )
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

    fun drawBox(
        box: Box,
        faceColor: Color4b,
        outlineColor: Color4b? = null,
        vertices: Int = -1,
        outlineVertices: Int = -1
    ) {
        faceRenderer.drawBox(env, box, color = faceColor, verticesToUse = vertices)

        if (outlineColor != null) {
            outlinesRenderer.drawBox(env, box, true, outlineColor, outlineVertices)
        }
    }

    private fun draw() {
        faceRenderer.draw()
        outlinesRenderer.draw()
    }

}

fun drawSolidBox(env: RenderEnvironment, consumer: VertexConsumer, box: Box, color: Color4b) {
    val matrix = env.currentMvpMatrix

    val vertexPositions = boxVertexPositions(box)

    // Draw the vertices of the box
    vertexPositions.forEach { (x, y, z) ->
        consumer.vertex(matrix, x, y, z).color(color.toRGBA())
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

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
import net.minecraft.util.math.Box
import net.minecraft.util.math.Position


/**
 * A utility class for drawing shapes in batches.
 *
 * Not sync, not send. Not thread-safe at all.
 *
 * This should only be used on render thread.
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
    @Suppress("CognitiveComplexMethod")
    @JvmOverloads
    fun drawBox(
        env: RenderEnvironment,
        box: Box,
        useOutlineVertices: Boolean = false,
        color: Color4b? = null,
        verticesToUse: Int = -1
    ) {
        val matrix = env.currentMvpMatrix

        val check = verticesToUse != -1

        // Draw the vertices of the box
        if (useOutlineVertices) {
            box.forEachOutlineVertex { i, x, y, z ->
                if (check && (verticesToUse and (1 shl i)) != 0) {
                    return@forEachOutlineVertex
                }

                val bb = buffer.vertex(matrix, x.toFloat(), y.toFloat(), z.toFloat())

                if (color != null) {
                    bb.color(color.toARGB())
                }
            }
        } else {
            box.forEachVertex { i, x, y, z ->
                if (check && (verticesToUse and (1 shl i)) != 0) {
                    return@forEachVertex
                }

                val bb = buffer.vertex(matrix, x.toFloat(), y.toFloat(), z.toFloat())

                if (color != null) {
                    bb.color(color.toARGB())
                }
            }
        }
    }

    fun draw() {
        val built = buffer.endNullable() ?: return

        RenderSystem.setShader(vertexFormat.shaderProgram)

        BufferRenderer.drawWithGlobalProgram(built)
        tesselator.clear()
    }

    fun reset() {
        buffer.endNullable()
    }

    companion object {
        @JvmField
        val TESSELATOR_A: Tessellator = Tessellator(0x200000)
        @JvmField
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
        @JvmStatic
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

private inline fun Box.forEachVertex(fn: (index: Int, x: Double, y: Double, z: Double) -> Unit) {
    var i = 0
    // down
    fn(i++, minX, minY, minZ)
    fn(i++, maxX, minY, minZ)
    fn(i++, maxX, minY, maxZ)
    fn(i++, minX, minY, maxZ)

    // up
    fn(i++, minX, maxY, minZ)
    fn(i++, minX, maxY, maxZ)
    fn(i++, maxX, maxY, maxZ)
    fn(i++, maxX, maxY, minZ)

    // north
    fn(i++, minX, minY, minZ)
    fn(i++, minX, maxY, minZ)
    fn(i++, maxX, maxY, minZ)
    fn(i++, maxX, minY, minZ)

    // east
    fn(i++, maxX, minY, minZ)
    fn(i++, maxX, maxY, minZ)
    fn(i++, maxX, maxY, maxZ)
    fn(i++, maxX, minY, maxZ)

    // south
    fn(i++, minX, minY, maxZ)
    fn(i++, maxX, minY, maxZ)
    fn(i++, maxX, maxY, maxZ)
    fn(i++, minX, maxY, maxZ)

    // west
    fn(i++, minX, minY, minZ)
    fn(i++, minX, minY, maxZ)
    fn(i++, minX, maxY, maxZ)
    fn(i++, minX, maxY, minZ)

    // i == 24
}

private inline fun Box.forEachOutlineVertex(fn: (index: Int, x: Double, y: Double, z: Double) -> Unit) {
    var i = 0
    // down north
    fn(i++, minX, minY, minZ)
    fn(i++, maxX, minY, minZ)

    // down east
    fn(i++, maxX, minY, minZ)
    fn(i++, maxX, minY, maxZ)

    // down south
    fn(i++, maxX, minY, maxZ)
    fn(i++, minX, minY, maxZ)

    // down west
    fn(i++, minX, minY, maxZ)
    fn(i++, minX, minY, minZ)

    // north west
    fn(i++, minX, minY, minZ)
    fn(i++, minX, maxY, minZ)

    // north east
    fn(i++, maxX, minY, minZ)
    fn(i++, maxX, maxY, minZ)

    // south east
    fn(i++, maxX, minY, maxZ)
    fn(i++, maxX, maxY, maxZ)

    // south west
    fn(i++, minX, minY, maxZ)
    fn(i++, minX, maxY, maxZ)

    // up north
    fn(i++, minX, maxY, minZ)
    fn(i++, maxX, maxY, minZ)

    // up east
    fn(i++, maxX, maxY, minZ)
    fn(i++, maxX, maxY, maxZ)

    // up south
    fn(i++, maxX, maxY, maxZ)
    fn(i++, minX, maxY, maxZ)

    // up west
    fn(i++, minX, maxY, maxZ)
    fn(i++, minX, maxY, minZ)

    // i == 24
}

fun RenderEnvironment.drawSolidBox(consumer: VertexConsumer, box: Box, color: Color4b) {
    val matrix = currentMvpMatrix
    val argb = color.toARGB()

    // Draw the vertices of the box
    box.forEachVertex { _, x, y, z ->
        consumer.vertex(matrix, x.toFloat(), y.toFloat(), z.toFloat())
            .color(argb)
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
    with(buffer) {
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

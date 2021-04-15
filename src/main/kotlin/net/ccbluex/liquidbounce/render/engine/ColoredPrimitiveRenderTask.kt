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

package net.ccbluex.liquidbounce.render.engine

import net.ccbluex.liquidbounce.render.engine.utils.imSetColorFromBuffer
import net.ccbluex.liquidbounce.render.engine.utils.imVertexPositionFromBuffer
import net.ccbluex.liquidbounce.render.engine.utils.popMVP
import net.ccbluex.liquidbounce.render.engine.utils.pushMVP
import net.ccbluex.liquidbounce.utils.math.Mat4
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL12
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL33
import java.nio.ByteBuffer
import java.nio.ShortBuffer

/**
 * Colored Primitive Render Task
 *
 * Can draw colored lines and triangles in 2 dimensions.
 *
 * If you want to render quads, use [PrimitiveType.Triangles] as [type] and set [maxPrimitiveCount] to twice the count of quads
 *
 * @throws IllegalArgumentException If [maxPrimitiveCount] there can be more vertices than 65535
 */
class ColoredPrimitiveRenderTask(private val maxPrimitiveCount: Int, internal val type: PrimitiveType) : RenderTask() {
    companion object {
        const val WORDS_PER_VERTEX = 4
    }

    /**
     * The buffer, the vertices are stored in.
     */
    internal val vertexBuffer: ByteBuffer = BufferUtils.createByteBuffer(vertexIndex(maxPrimitiveCount) * 4)

    /**
     * The buffer, the vertex indices are stored in.
     */
    internal val indexBuffer: ShortBuffer = BufferUtils.createShortBuffer(maxPrimitiveCount * type.verticesPerPrimitive)

    /**
     * This field keeps track of how many vertices are in [vertexBuffer]
     */
    private var vertexBufferIndex: Int = 0

    /**
     * The count of indices in [indexBuffer]
     */
    internal var indexBufferIndex: Int = 0

    private lateinit var vaoData: VAOData

    init {
        if (maxPrimitiveCount * type.verticesPerPrimitive > 65535) {
            throw IllegalStateException("Too many vertices")
        }
    }

    /**
     * Puts a vertex into [vertexBuffer]
     */
    fun vertex(point: Vec3, color: Color4b): Int {
        val vexIndex = vertexBufferIndex * WORDS_PER_VERTEX

        point.writeToBuffer(vexIndex * 4, this.vertexBuffer)
        color.writeToBuffer((vexIndex + 3) * 4, this.vertexBuffer)

        return vertexBufferIndex++
    }

    /**
     * Puts an index into the index buffer
     */
    fun index(index: Int) {
        this.indexBuffer.put(this.indexBufferIndex++, index.toShort())
    }

    /**
     * Renders a line. Only available if [type] is [PrimitiveType.Lines].
     *
     * @throws IllegalStateException If this tasks doesn't render lines ([type] != [PrimitiveType.Lines])
     */
    fun line(p1: Vec3, p2: Vec3, color1: Color4b, color2: Color4b = color1) {
        if (this.type != PrimitiveType.Lines) {
            throw IllegalStateException("Type is not Lines")
        }

        index(vertex(p1, color1))
        index(vertex(p2, color2))
    }

    /**
     * Renders a triangle. Points have to be passed in counter-clockwise order. Only available if [type] is [PrimitiveType.Triangles].
     *
     * @throws IllegalStateException If this tasks doesn't render lines ([type] != [PrimitiveType.Triangles])
     */
    fun triangle(
        p1: Vec3,
        p2: Vec3,
        p3: Vec3,
        color1: Color4b,
        color2: Color4b = color1,
        color3: Color4b = color1
    ) {
        if (this.type != PrimitiveType.Triangles) {
            throw IllegalStateException("Type is not Triangles")
        }

        index(vertex(p1, color1))
        index(vertex(p2, color2))
        index(vertex(p3, color3))
    }

    /**
     * Renders a quad. Points have to be passed in counter-clockwise order.
     * Only available if [type] is [PrimitiveType.Triangles].
     *
     * A quad consists out of two primitives
     *
     * @throws IllegalStateException If this tasks doesn't render lines ([type] != [PrimitiveType.Triangles])
     * @throws IllegalStateException If the maximal vertex count is reached ([primitiveCount] >= [maxPrimitiveCount])
     */
    fun quad(
        p1: Vec3,
        p2: Vec3,
        p3: Vec3,
        p4: Vec3,
        color1: Color4b,
        color2: Color4b = color1,
        color3: Color4b = color1,
        color4: Color4b = color1
    ) {
        if (this.type != PrimitiveType.Triangles) {
            throw IllegalStateException("Type is not Triangles or Quads")
        }

        val v1 = vertex(p1, color1)
        val v2 = vertex(p2, color2)
        val v3 = vertex(p3, color3)
        val v4 = vertex(p4, color4)

        index(v1)
        index(v2)
        index(v4)
        index(v4)
        index(v2)
        index(v3)
    }

    /**
     * Renders the outlines a quad. Points have to be passed in counter-clockwise order.
     * Only available if [type] is [PrimitiveType.Triangles].
     *
     * A quad consists out of two primitives
     *
     * @throws IllegalStateException If this tasks doesn't render lines ([type] != [PrimitiveType.Triangles])
     * @throws IllegalStateException If the maximal vertex count is reached ([primitiveCount] >= [maxPrimitiveCount])
     */
    fun outlineQuad(
        p1: Vec3,
        p2: Vec3,
        p3: Vec3,
        p4: Vec3,
        color1: Color4b,
        color2: Color4b = color1,
        color3: Color4b = color1,
        color4: Color4b = color1
    ) {
        if (this.type == PrimitiveType.Lines) {
            val v1 = vertex(p1, color1)
            val v2 = vertex(p2, color2)
            val v3 = vertex(p3, color3)
            val v4 = vertex(p4, color4)

            index(v1)
            index(v2)
            index(v2)
            index(v3)
            index(v3)
            index(v4)
            index(v4)
            index(v1)
        } else if (this.type == PrimitiveType.LineLoop || this.type == PrimitiveType.LineStrip) {
            vertex(p1, color1)
            vertex(p2, color2)
            vertex(p3, color3)
            vertex(p4, color4)

            if (this.type == PrimitiveType.LineStrip) {
                vertex(p1, color1)
            }
        } else {
            throw IllegalStateException("Type is not Lines, LineLoop or LineStrip")
        }
    }

    /**
     * The vertex count for a given count of primitives.
     *
     * `Two dimensions * Vertices per primitive * (1 int of color + 3 dimensions) * primitives`
     */
    private fun vertexIndex(primitives: Int) = primitives * type.verticesPerPrimitive * (1 + 3)

    override fun getBatchRenderer(): BatchRenderer? = null

    override fun initRendering(level: OpenGLLevel, mvpMatrix: Mat4) {
        GL12.glDisable(GL12.GL_LIGHTING)
        GL12.glDisable(GL12.GL_SCISSOR_TEST)
        GL12.glDisable(GL12.GL_TEXTURE_2D)
        GL12.glEnable(GL12.GL_BLEND)
        GL12.glEnable(GL12.GL_BLEND)
        GL12.glBlendFunc(GL12.GL_SRC_ALPHA, GL12.GL_ONE_MINUS_SRC_ALPHA)

        when (level) {
            OpenGLLevel.OPENGL3_3, OpenGLLevel.OPENGL4_3 -> {
                InstancedColoredPrimitiveShader.bind(mvpMatrix)
            }
            else -> {
                pushMVP(mvpMatrix)
            }
        }
    }

    override fun draw(level: OpenGLLevel) {
        when (level) {
            // Use Immediate mode for OpenGL 1.2. A cheap emulated version of the OpenGL 2.1 backend.
            OpenGLLevel.OPENGL1_2 -> {
                // Begin rendering with the type's mode
                GL11.glBegin(this.type.mode)

                // Iterate through the indices
                for (i in 0 until this.indexBufferIndex) {
                    // Get the current index from the index buffer
                    val vertexIndex = this.indexBuffer[i]

                    // Where does the vertex start?
                    val idx = vertexIndex * WORDS_PER_VERTEX

                    // Set the vertex color
                    imSetColorFromBuffer(vertexBuffer, (idx + 3))

                    // Set the vertex position
                    imVertexPositionFromBuffer(vertexBuffer, idx)
                }

                // Finish drawing
                GL11.glEnd()
            }
            // Use VBOs for later OpenGL versions.
            OpenGLLevel.OPENGL3_3, OpenGLLevel.OPENGL4_3 -> {
                // Upload if not done yet
                this.uploadIfNotUploaded()

                this.vaoData.bind()

                // Render the entire thing
                GL20.glDrawElements(
                    this.type.mode,
                    this.indexBufferIndex,
                    GL20.GL_UNSIGNED_SHORT,
                    0
                )
            }
        }
    }

    override fun upload() {
        val vboData = VAOData(this.storageType)

        vboData.bind()

        uploadVAO(vboData)

        GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0)

        vboData.unbind()

        this.vaoData = vboData
    }

    internal fun uploadVAO(vaoData: VAOData) {
        GL20.glEnableVertexAttribArray(0)
        GL20.glEnableVertexAttribArray(1)

        vaoData.arrayBuffer.bind()
        vaoData.arrayBuffer.putData(this.vertexBuffer)

        // Vertex positions at attrib[0]
        GL20.glVertexAttribPointer(0, 3, GL20.GL_FLOAT, false, 16, 0)

        // Color info at attrib[1], we want the data to be normalized
        GL20.glVertexAttribPointer(1, 4, GL20.GL_UNSIGNED_BYTE, true, 16, 12)

        vaoData.elementBuffer.bind()
        vaoData.elementBuffer.putData(this.indexBuffer)

        // Make every other attempt to put a vertex in those buffers fail :3
        this.vertexBuffer.limit(this.vertexBufferIndex * WORDS_PER_VERTEX * 4)
        this.indexBuffer.limit(this.indexBufferIndex)
    }

    override fun cleanupRendering(level: OpenGLLevel) {
        when (level) {
            OpenGLLevel.OPENGL3_3, OpenGLLevel.OPENGL4_3 -> {
                // Disable all shader programs
                GL20.glUseProgram(0)
                // Unbind VBOs, only needs to be done once during rendering
                GL33.glBindVertexArray(0)
            }
            else -> {
                popMVP()
            }
        }
    }

}

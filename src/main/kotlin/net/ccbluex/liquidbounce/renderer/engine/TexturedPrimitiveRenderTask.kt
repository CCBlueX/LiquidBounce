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

package net.ccbluex.liquidbounce.renderer.engine

import net.ccbluex.liquidbounce.utils.Mat4
import net.minecraft.client.MinecraftClient
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20
import java.nio.ByteBuffer
import java.nio.ShortBuffer

/**
 * Textured Primitive Render Task
 *
 * Can draw textured triangles in 3 dimensions.
 *
 * If you want to render quads, set [maxPrimitiveCount] to twice the count of quads
 *
 * @throws IllegalArgumentException If [maxPrimitiveCount] there can be more vertices than 65535
 */
class TexturedPrimitiveRenderTask(private val maxPrimitiveCount: Int, private val texture: Texture) : RenderTask() {
    companion object {
        const val WORDS_PER_VERTEX = 5
    }

    private val type: PrimitiveType = PrimitiveType.Triangles

    /**
     * The buffer, the vertices are stored in.
     */
    private val vertexBuffer: ByteBuffer = BufferUtils.createByteBuffer(vertexIndex(maxPrimitiveCount) * 4)

    /**
     * The buffer, the vertex indices are stored in.
     */
    private val indexBuffer: ShortBuffer = BufferUtils.createShortBuffer(maxPrimitiveCount * type.verticesPerPrimitive)

    /**
     * This field keeps track of how many vertices are in [vertexBuffer]
     */
    private var vertexBufferIndex: Int = 0

    /**
     * The count of indices in [indexBuffer]
     */
    private var indexBufferIndex: Int = 0

    private lateinit var vboData: UploadedVBOData

    init {
        if (maxPrimitiveCount * type.verticesPerPrimitive > 65535)
            throw IllegalStateException("Too many vertices")
    }

    /**
     * Puts a vertex into [vertexBuffer]
     */
    fun vertex(point: Point3f, color: Color4b, uv: UV2s): Int {
        val vexIndex = vertexBufferIndex * WORDS_PER_VERTEX

        point.writeToBuffer((vexIndex) * 4, this.vertexBuffer)
        color.writeToBuffer((vexIndex + 3) * 4, this.vertexBuffer)
        uv.writeToBuffer((vexIndex + 4) * 4, this.vertexBuffer)

        return vertexBufferIndex++
    }

    /**
     * Puts an index into the index buffer
     */
    fun index(index: Int) {
        this.indexBuffer.put(this.indexBufferIndex++, index.toShort())
    }

    /**
     * Renders a triangle. Points have to be passed in counter-clockwise order.
     */
    fun triangle(
        p1: Point3f,
        color1: Color4b,
        UV1: UV2s,
        p2: Point3f,
        color2: Color4b,
        UV2: UV2s,
        p3: Point3f,
        color3: Color4b,
        UV3: UV2s
    ) {
        index(vertex(p1, color1, UV1))
        index(vertex(p2, color2, UV2))
        index(vertex(p3, color3, UV3))
    }

    /**
     * Renders a quad. Points have to be passed in counter-clockwise order.
     *
     * A quad consists out of two triangles.
     */
    fun quad(
        p1: Point3f,
        color1: Color4b,
        UV1: UV2s,
        p2: Point3f,
        color2: Color4b,
        UV2: UV2s,
        p3: Point3f,
        color3: Color4b,
        UV3: UV2s,
        p4: Point3f,
        color4: Color4b,
        UV4: UV2s
    ) {
        val v1 = vertex(p1, color1, UV1)
        val v2 = vertex(p2, color2, UV2)
        val v3 = vertex(p3, color3, UV3)
        val v4 = vertex(p4, color4, UV4)

        index(v1)
        index(v2)
        index(v4)
        index(v4)
        index(v2)
        index(v3)
    }

    /**
     * The vertex count for a given count of primitives.
     *
     * `Two dimensions * Vertices per primitive * (1 word of color + 1 word of UV + 3 dimensions) * primitives`
     */
    private fun vertexIndex(primitives: Int) = primitives * type.verticesPerPrimitive * (1 + 1 + 3)

    override fun getBatchRenderer(): BatchRenderer? = null

    override fun initRendering(level: OpenGLLevel) {
        when (level) {
            OpenGLLevel.OpenGL3_1, OpenGLLevel.OpenGL4_3 -> {
                val mc = MinecraftClient.getInstance()

                // Create an orthographic projection matrix
                TexturedPrimitiveShader.bind(
                    Mat4.projectionMatrix(
                        0.0f,
                        0.0f,
                        mc.window.framebufferWidth.toFloat(),
                        mc.window.framebufferHeight.toFloat(),
                        -1.0f,
                        1.0f
                    )
                )
            }
            else -> {
            }
        }
    }

    override fun draw(level: OpenGLLevel) {
        when (level) {
            // Use Immediate mode for OpenGL 1.2. A cheap emulated version of the OpenGL 2.1 backend.
            OpenGLLevel.OpenGL1_2 -> {
                // Begin rendering with the type's mode
                GL11.glBegin(this.type.mode)

                val floatBuffer = this.vertexBuffer.asFloatBuffer()
                val intBuffer = this.vertexBuffer.asIntBuffer()
                val shortBuffer = this.vertexBuffer.asShortBuffer()

                // Iterate through the indices
                for (i in 0 until this.indexBufferIndex) {
                    // Get the current index from the index buffer
                    val vertexIndex = this.indexBuffer[i]

                    // Where does the vertex start?
                    val idx = vertexIndex * WORDS_PER_VERTEX

                    val color = intBuffer[idx + 3]

                    // Set the vertex color
                    GL11.glColor4f(
                        ((color shr 8) and 255) / 255.0f,
                        ((color shr 16) and 255) / 255.0f,
                        (color and 255) / 255.0f,
                        ((color shr 24) and 255) / 255.0f
                    )

                    // Set UV
                    GL11.glTexCoord2f(
                        shortBuffer.get((idx + 4) * 2).toFloat() / 65535.0f,
                        shortBuffer.get((idx + 4) * 2 + 1).toFloat() / 65535.0f,
                    )

                    // Set the vertex position
                    GL11.glVertex3f(floatBuffer[idx], floatBuffer[idx + 1], floatBuffer[idx + 2])

                }

                // Finish drawing
                GL11.glEnd()
            }
            // Use VBOs for later OpenGL versions.
            OpenGLLevel.OpenGL3_1, OpenGLLevel.OpenGL4_3 -> {
                // Upload if not done yet
                this.uploadIfNotUploaded()

                this.vboData.bind()

                GL20.glEnableVertexAttribArray(0)
                GL20.glEnableVertexAttribArray(1)
                GL20.glEnableVertexAttribArray(2)

                this.texture.bind()

                // Render the entire thing
                GL20.glDrawElements(
                    this.type.mode,
                    this.indexBufferIndex,
                    GL20.GL_UNSIGNED_SHORT,
                    0
                )

                this.texture.unbind()
            }
        }
    }

    override fun upload() {
        val vboData = UploadedVBOData(this.storageType)

        vboData.bind()

        GL20.glEnableVertexAttribArray(0)
        GL20.glEnableVertexAttribArray(1)
        GL20.glEnableVertexAttribArray(2)

        // Vertex positions at attrib[0]
        GL20.glVertexAttribPointer(0, 3, GL20.GL_FLOAT, false, 20, 0)

        // UV at attrib[1], we want the data to be normalized
        GL20.glVertexAttribPointer(1, 4, GL20.GL_UNSIGNED_BYTE, true, 20, 12)

        // Color info at attrib[2], we want the data to be normalized
        GL20.glVertexAttribPointer(2, 2, GL20.GL_UNSIGNED_SHORT, true, 20, 16)

        this.vertexBuffer.limit(this.vertexBufferIndex * WORDS_PER_VERTEX * 4)
        this.indexBuffer.limit(this.indexBufferIndex)

        vboData.arrayBuffer.putData(this.vertexBuffer)
        vboData.elementBuffer.putData(this.indexBuffer)

        vboData.unbind()

        this.vboData = vboData
    }

    override fun cleanupRendering(level: OpenGLLevel) {
        when (level) {
            OpenGLLevel.OpenGL3_1, OpenGLLevel.OpenGL4_3 -> {
                // Disable all shader programs
                GL20.glUseProgram(0)
                // Unbind VBOs, only needs to be done once during rendering
                this.vboData.unbind()
            }
            else -> {
            }
        }
    }

    /**
     * Render tasks can *always* be packed into a single VBO.
     */
    override fun typeId(): Int = 0
}

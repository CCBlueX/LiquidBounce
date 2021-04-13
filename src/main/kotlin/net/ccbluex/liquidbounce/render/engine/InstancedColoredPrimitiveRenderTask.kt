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
import net.ccbluex.liquidbounce.utils.Mat4
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.*
import java.nio.ByteBuffer

class InstancedColoredPrimitiveRenderTask(
    private val maxPrimitiveCount: Int,
    private val baseTask: ColoredPrimitiveRenderTask
) : RenderTask() {
    companion object {
        const val WORDS_PER_INSTANCE = 4
    }

    /**
     * The buffer, the vertices are stored in.
     */
    private val instanceBuffer: ByteBuffer = BufferUtils.createByteBuffer(WORDS_PER_INSTANCE * 4 * maxPrimitiveCount)

    private var instanceCount = 0

    private lateinit var vaoData: InstancedVAOData

    /**
     * Stores information about the next instance in the instance buffer
     */
    fun instance(pos: Vec3, color: Color4b) {
        val wordIdx = this.instanceCount * WORDS_PER_INSTANCE

        pos.writeToBuffer(wordIdx * 4, this.instanceBuffer)
        color.writeToBuffer((wordIdx + 3) * 4, this.instanceBuffer)

        this.instanceCount++
    }

    override fun getBatchRenderer(): BatchRenderer? = null

    override fun initRendering(level: OpenGLLevel, mvpMatrix: Mat4) {
        GL12.glDisable(GL12.GL_TEXTURE_2D)

        when (level) {
            OpenGLLevel.OpenGL3_3, OpenGLLevel.OpenGL4_3 -> {
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
            OpenGLLevel.OpenGL1_2 -> {
                val list = GL11.glGenLists(1)

                GL11.glNewList(list, GL11.GL_COMPILE)
                // Begin rendering with the type's mode
                GL11.glBegin(this.baseTask.type.mode)

                // Iterate through the indices
                for (i in 0 until this.baseTask.indexBufferIndex) {
                    // Get the current index from the index buffer
                    val vertexIndex = this.baseTask.indexBuffer[i]

                    // Where does the vertex start?
                    val idx = vertexIndex * ColoredPrimitiveRenderTask.WORDS_PER_VERTEX

                    // Set the vertex position
                    imVertexPositionFromBuffer(this.baseTask.vertexBuffer, idx)
                }

                // Finish drawing
                GL11.glEnd()

                GL11.glEndList()

                var currentPosition = Vec3(0.0f, 0.0f, 0.0f)

                // Iterate through the instances
                for (i in 0 until this.baseTask.indexBufferIndex) {
                    val idx = i * WORDS_PER_INSTANCE

                    val instancePosition = Vec3(
                        this.instanceBuffer.getFloat(idx * 4),
                        this.instanceBuffer.getFloat((idx + 1) * 4),
                        this.instanceBuffer.getFloat((idx + 2) * 4)
                    )

                    // Apply the changed translation
                    GL11.glTranslatef(
                        instancePosition.x - currentPosition.x,
                        instancePosition.y - currentPosition.y,
                        instancePosition.z - currentPosition.z
                    )

                    // Set the color for the current instance
                    imSetColorFromBuffer(this.instanceBuffer, idx + 3)

                    // Render the DL
                    GL11.glCallList(list)

                    currentPosition = instancePosition
                }

                GL11.glDeleteLists(list, 1)
            }
            // Use VBOs for later OpenGL versions.
            OpenGLLevel.OpenGL3_3, OpenGLLevel.OpenGL4_3 -> {
                // Upload if not done yet
                this.uploadIfNotUploaded()

                this.vaoData.bind()

                // Render the entire thing
                GL31.glDrawElementsInstanced(
                    this.baseTask.type.mode,
                    this.baseTask.indexBufferIndex,
                    GL20.GL_UNSIGNED_SHORT,
                    0,
                    this.instanceCount
                )
            }
        }
    }

    override fun cleanupRendering(level: OpenGLLevel) {
        when (level) {
            OpenGLLevel.OpenGL3_3, OpenGLLevel.OpenGL4_3 -> {
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

    override fun upload() {
        val vaoData = InstancedVAOData(this.storageType)

        vaoData.bind()

//        // Upload the base model
        this.baseTask.uploadVAO(vaoData.baseUploader)
//
        GL20.glEnableVertexAttribArray(2)
        GL20.glEnableVertexAttribArray(3)

        vaoData.instanceData.bind()
        vaoData.instanceData.putData(this.instanceBuffer)

        // Instance offsets at attrib[0]
        GL20.glVertexAttribPointer(2, 3, GL20.GL_FLOAT, false, 16, 0)

        // Color multiplier at attrib[1]
        GL20.glVertexAttribPointer(3, 4, GL20.GL_UNSIGNED_BYTE, true, 16, 12)

        // Make the instance attributes advance per instance, not per vertex
        GL33.glVertexAttribDivisor(2, 1)
        GL33.glVertexAttribDivisor(3, 1)

        // Make every other attempt to put a vertex in those buffers fail :3
        this.instanceBuffer.limit(this.instanceCount * WORDS_PER_INSTANCE * 4)

        GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0)

        vaoData.unbind()


        this.vaoData = vaoData
    }

}

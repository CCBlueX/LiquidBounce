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

import net.ccbluex.liquidbounce.render.engine.memory.AttributeType
import net.ccbluex.liquidbounce.render.engine.memory.IndexBuffer
import net.ccbluex.liquidbounce.render.engine.memory.VertexFormat
import net.ccbluex.liquidbounce.render.engine.utils.popMVP
import net.ccbluex.liquidbounce.render.engine.utils.pushMVP
import net.ccbluex.liquidbounce.utils.math.Mat4
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL12
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL33

class VertexFormatRenderTask(private val vertexFormat: VertexFormat, internal val type: PrimitiveType, private val indexBuffer: IndexBuffer?) : RenderTask() {
    val arrayBuffer = VertexBufferObject(VBOTarget.ArrayBuffer, storageType)
    val elementBuffer = VertexBufferObject(VBOTarget.ElementArrayBuffer, storageType)
    val vao = VertexAttributeObject()

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
                ColoredPrimitiveShader.bind(mvpMatrix)
            }
            else -> {
                this.vertexFormat.components.forEach {
                    GL11.glEnableClientState(it.attribInfo.attributeType.openGlClientState)
                }

                pushMVP(mvpMatrix)
            }
        }
    }

    override fun draw(level: OpenGLLevel) {
        when (level) {
            // Use Immediate mode for OpenGL 1.2.
            OpenGLLevel.OPENGL1_2 -> {
                this.arrayBuffer.bind()

                this.vertexFormat.components.forEach {
                    when (it.attribInfo.attributeType) {
                        AttributeType.Position -> GL11.glVertexPointer(it.count, it.type.legacyOpenGlEnum, this.vertexFormat.length, it.offset.toLong())
                        AttributeType.Color -> GL11.glColorPointer(it.count, it.type.openGlEnum, this.vertexFormat.length, it.offset.toLong())
                        AttributeType.Texture -> GL11.glTexCoordPointer(it.count, it.type.legacyOpenGlEnum, this.vertexFormat.length, it.offset.toLong())
                        else -> throw IllegalStateException()
                    }
                }
            }
            // Use VBOs for later OpenGL versions.
            OpenGLLevel.OPENGL3_3, OpenGLLevel.OPENGL4_3 -> {
                // Upload if not done yet
                this.uploadIfNotUploaded(level)

                this.vao.bind()
            }
        }

        val indexBuffer = this.indexBuffer

        if (indexBuffer != null) {
            // Render the entire thing
            GL20.glDrawElements(
                this.type.mode,
                indexBuffer.size,
                indexBuffer.indexType.openGlEnum,
                0
            )
        } else {
            GL20.glDrawArrays(
                this.type.mode,
                0,
                this.vertexFormat.vertexCount
            )
        }
    }

    override fun upload(level: OpenGLLevel) {
        if (level == OpenGLLevel.OPENGL1_2) {
            this.arrayBuffer.bind()
            this.arrayBuffer.putData(this.vertexFormat.bufferBuilder.buffer.slice())

            this.indexBuffer?.let {
                this.elementBuffer.bind()
                this.elementBuffer.putData(it.bufferBuilder.buffer)
            }
        } else {
            this.vao.bind()

            uploadVAO()

            GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0)

            this.vao.unbind()
        }
    }

    internal fun uploadVAO() {
        this.arrayBuffer.bind()
        this.arrayBuffer.putData(this.vertexFormat.bufferBuilder.buffer.slice())

        this.vertexFormat.components.forEach {
            val attribIndex = it.attribInfo.attribIndex

            GL20.glEnableVertexAttribArray(attribIndex)

            GL20.glVertexAttribPointer(attribIndex, it.count, it.type.openGlEnum, it.normalized, this.vertexFormat.length, it.offset.toLong())
        }

        this.indexBuffer?.let {
            this.elementBuffer.bind()
            this.elementBuffer.putData(it.bufferBuilder.buffer)
        }
    }

    override fun cleanupRendering(level: OpenGLLevel) {
        when (level) {
            OpenGLLevel.OPENGL3_3, OpenGLLevel.OPENGL4_3 -> {
                // Disable all shader programs
                GL20.glUseProgram(0)
                // Unbind VAOs, only needs to be done once during rendering
                GL33.glBindVertexArray(0)
            }
            else -> {
                this.vertexFormat.components.forEach {
                    GL11.glDisableClientState(it.attribInfo.attributeType.openGlClientState)
                }

                popMVP()
            }
        }
    }

}

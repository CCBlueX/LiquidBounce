/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2022 CCBlueX
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

import net.ccbluex.liquidbounce.render.engine.VBOStorageType.Stream
import org.lwjgl.opengl.GL20
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.nio.ShortBuffer

/**
 * A wrapper for VBOs
 *
 * @param target What kind of data does this buffer store?
 * @param storageType How does the data behave when it comes to updates? See [VBOStorageType]
 */
class VertexBufferObject(val target: VBOTarget, val storageType: VBOStorageType) {
    /**
     * OpenGL's id for the buffer
     */
    val id: Int

    init {
        // Get an id for our VBO
        id = GL20.glGenBuffers()
    }

    /**
     * Binds this buffer
     */
    fun bind() {
        GL20.glBindBuffer(this.target.glId, this.id)
    }

    /**
     * Unbinds all buffers for this target
     */
    fun unbind() {
        GL20.glBindBuffer(this.target.glId, 0)
    }

    /**
     * Puts data into the VBO
     */
    fun putData(buf: ByteBuffer) {
        GL20.glBufferData(this.target.glId, buf, this.storageType.glUsageId)
    }

    /**
     * @see putData
     */
    fun putData(buf: ShortBuffer) {
        GL20.glBufferData(this.target.glId, buf, this.storageType.glUsageId)
    }

    /**
     * @see putData
     */
    fun putData(buf: IntBuffer) {
        GL20.glBufferData(this.target.glId, buf, this.storageType.glUsageId)
    }

    /**
     * Deletes the buffer
     */
    fun delete() {
        GL20.glDeleteBuffers(id)
    }

    fun finalize() {
        val id = this.id

        RenderEngine.runOnGlContext {
            GL20.glDeleteBuffers(id)
        }
    }
}

/**
 * Declares options for buffers about their storage behaviour. If you are unsure what to use, use [Stream]
 */
enum class VBOStorageType(val glUsageId: Int) {
    /**
     * The data is updated once, then used for a few frames
     */
    Stream(GL20.GL_STREAM_DRAW),

    /**
     * The data is updated once, then used for a long time
     */
    Static(GL20.GL_STATIC_DRAW),

    /**
     * The data is multiple times
     */
    Dynamic(GL20.GL_DYNAMIC_DRAW)
}

/**
 * Declares options for what is a VBO
 */
enum class VBOTarget(val glId: Int) {
    /**
     * Vertex attributes
     */
    ArrayBuffer(GL20.GL_ARRAY_BUFFER),

    /**
     * Vertex array indices
     */
    ElementArrayBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER),
}

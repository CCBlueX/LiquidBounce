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

package net.ccbluex.liquidbounce.render.engine.memory

class IndexBuffer(capacity: Int, val indexType: VertexFormatComponentDataType) {
    var size: Int = 0
        private set

    val bufferBuilder: BufferBuilder

    init {
        if (indexType != VertexFormatComponentDataType.GlUnsignedShort && indexType != VertexFormatComponentDataType.GlUnsignedByte && indexType != VertexFormatComponentDataType.GlUnsignedInt) {
            throw IllegalStateException("Invalid index buffer type")
        }

        this.bufferBuilder = BufferBuilder(capacity * indexType.length)
    }

    fun index(index: Int) {
        val offset = bufferBuilder.pos

        when (indexType) {
            VertexFormatComponentDataType.GlUnsignedShort -> this.bufferBuilder.buffer.putShort(offset, index.toShort())
            VertexFormatComponentDataType.GlUnsignedInt -> this.bufferBuilder.buffer.putInt(offset, index)
            VertexFormatComponentDataType.GlUnsignedByte -> this.bufferBuilder.buffer.put(offset, index.toByte())
            else -> throw IllegalStateException()
        }

        bufferBuilder.pos += this.indexType.length

        size++
    }

    fun indexQuad(p0: Int, p1: Int, p2: Int, p3: Int) {
        index(p0)
        index(p1)
        index(p3)

        index(p1)
        index(p2)
        index(p3)
    }

    fun indexLine(p0: Int, p1: Int) {
        index(p0)
        index(p1)
    }
}

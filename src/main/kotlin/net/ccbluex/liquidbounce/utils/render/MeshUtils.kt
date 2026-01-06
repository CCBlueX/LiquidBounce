/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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

package net.ccbluex.liquidbounce.utils.render

import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.vertex.MeshData
import com.mojang.blaze3d.vertex.VertexFormat
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.utils.client.logger
import net.minecraft.client.renderer.MappableRingBuffer
import org.lwjgl.system.MemoryUtil

object MeshUtils {

    private const val MIN_BUFFER_SIZE = 1 shl 20 // 1 MB

    private var sharedVertexBuffer: MappableRingBuffer? = null

    private fun ensureVertexBufferCapacity(byteCount: Int): MappableRingBuffer {
        if (sharedVertexBuffer == null || sharedVertexBuffer!!.size() < byteCount) {
            val size = maxOf(MIN_BUFFER_SIZE, byteCount)
            sharedVertexBuffer?.close()
            sharedVertexBuffer = MappableRingBuffer(
                { "${LiquidBounce.CLIENT_NAME} Shared VBO" },
                GpuBuffer.USAGE_VERTEX or GpuBuffer.USAGE_MAP_WRITE,
                size
            )
            logger.debug("Shared vertex buffer grown to $size bytes")
        }

        return sharedVertexBuffer!!
    }

    /**
     * Upload the vertices of this [MeshData] to a shared [MappableRingBuffer].
     *
     * @returns VBO
     */
    @JvmStatic
    fun MeshData.uploadVertices(format: VertexFormat): GpuBufferSlice {
        val byteCount = this.drawState().vertexCount() * format.vertexSize
        val buffer = ensureVertexBufferCapacity(byteCount).currentBuffer()

        val slice = buffer.slice(0, this.vertexBuffer().remaining().toLong())
        buffer.mapBuffer(read = false, write = true).use {
            MemoryUtil.memCopy(this.vertexBuffer(), it.data())
        }

        return slice
    }

    @JvmStatic
    fun rotateVertexBuffer() {
        sharedVertexBuffer?.rotate()
    }

}

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
import java.util.function.Supplier

class DynamicVertexStorage(
    private val label: Supplier<String>,
    private val minBufferSize: Int = 1 shl 10,
) {

    private var sharedVertexBuffer: MappableRingBuffer? = null
    var currentVertexCount: Int = -1
        private set
    var currentSlice: GpuBufferSlice? = null
        private set

    private fun ensureVertexBufferCapacity(byteCount: Int): MappableRingBuffer {
        if (sharedVertexBuffer == null || sharedVertexBuffer!!.size() < byteCount) {
            val size = maxOf(minBufferSize, byteCount)
            clear()
            sharedVertexBuffer = MappableRingBuffer(
                label,
                GpuBuffer.USAGE_VERTEX or GpuBuffer.USAGE_MAP_WRITE,
                size
            )
            logger.debug("Shared vertex buffer grown to $size bytes")
        }

        return sharedVertexBuffer!!
    }

    /**
     * Upload the vertices of the [MeshData] to a shared [MappableRingBuffer].
     *
     * @returns VBO
     */
    fun upload(meshData: MeshData, format: VertexFormat): GpuBufferSlice {
        val vertexCount = meshData.drawState().vertexCount()
        val byteCount = vertexCount * format.vertexSize
        val buffer = ensureVertexBufferCapacity(byteCount).currentBuffer()

        val slice = buffer.slice(0, meshData.vertexBuffer().remaining().toLong())
        buffer.mapBuffer(read = false, write = true).use {
            MemoryUtil.memCopy(meshData.vertexBuffer(), it.data())
        }
        currentSlice = slice
        currentVertexCount = vertexCount

        return slice
    }

    fun rotate() {
        sharedVertexBuffer?.rotate()
        currentSlice = null
        currentVertexCount = -1
    }

    fun clear() {
        sharedVertexBuffer?.close()
        currentSlice = null
        currentVertexCount = -1
    }

    companion object {
        @JvmField
        internal val SHARED = DynamicVertexStorage({ "${LiquidBounce.CLIENT_NAME} Shared VBO" }, 1 shl 20)
    }

}

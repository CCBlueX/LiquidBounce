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

package net.ccbluex.liquidbounce.render

import com.google.common.base.Suppliers
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.buffers.GpuBufferSlice
import net.ccbluex.liquidbounce.utils.client.formatAsCapacity
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.render.mapBuffer
import net.minecraft.client.renderer.MappableRingBuffer
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer

class GrowableMappableRingBuffer @JvmOverloads constructor(
    val label: String,
    val usage: @GpuBuffer.Usage Int,
    val growPolicy: GrowPolicy = GrowPolicy.of(paddingScale = 7, min = 0), // 128 bytes padding
) {

    private var ring: MappableRingBuffer? = null

    private fun ensureCapacity(byteCount: Int) {
        val current = ring
        if (current == null || current.size() < byteCount) {
            val newSize = growPolicy.getNewSize(byteCount, current?.size() ?: 0)
            current?.close()
            ring = MappableRingBuffer(
                Suppliers.ofInstance(label),
                usage or GpuBuffer.USAGE_MAP_WRITE,
                newSize
            ).also {
                // TODO: change this to debug log
                logger.info("$label buffer grown to $newSize bytes (${newSize.toLong().formatAsCapacity()})")
            }
        }
    }

    /**
     * Upload [data] to the ring buffer.
     *
     * @param data the data to upload.
     * @param byteCount the number of bytes to upload. Default is the remaining bytes of [data].
     * @return the uploaded slice, whose lifecycle is bound to the ring buffer.
     */
    @JvmOverloads
    fun upload(data: ByteBuffer, byteCount: Int = data.remaining()): GpuBufferSlice {
        ensureCapacity(byteCount)

        val ring = checkNotNull(this.ring) { "Ring buffer not initialized" }
        val buffer = ring.currentBuffer()

        val slice = buffer.slice(0, byteCount.toLong())

        buffer.mapBuffer(read = false, write = true).use { mappedView ->
            MemoryUtil.memCopy(data, mappedView.data())
        }

        return slice
    }

    /**
     * Rotate the ring buffer to the next buffer.
     *
     * This method should be called at the end of each frame to ensure that the fence is processed.
     */
    fun rotate() {
        ring?.rotate()
    }

    /**
     * Clear the ring buffer and release all GPU resources.
     */
    fun clear() {
        ring?.close()
        ring = null
    }

    fun interface GrowPolicy {
        /**
         * Calculate the new size of the ring buffer.
         *
         * @param requested the requested size.
         * @param current the current size. `0` if the ring buffer is not initialized.
         * @return the new size, should be greater than or equal to [requested].
         */
        fun getNewSize(requested: Int, current: Int): Int

        companion object {
            @JvmStatic
            fun of(paddingScale: Int, min: Int) = GrowPolicy { requested, current ->
                val default = maxOf(min, requested, current)
                val padding = 1 shl paddingScale
                (default + padding - 1) and (padding - 1).inv()
            }
        }
    }

}

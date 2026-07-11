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

import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.utils.text.formatAsCapacity
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.render.write
import net.minecraft.util.Mth
import java.nio.ByteBuffer
import kotlin.math.max

/**
 * Per-frame dynamic [GpuBuffer] writer for streaming VBO/IBO uploads.
 *
 * Follows vanilla's [net.minecraft.client.renderer.StagedVertexBuffer] pattern:
 * CPU data is written to a properly-flagged GPU buffer via [com.mojang.blaze3d.systems.CommandEncoder.writeToBuffer],
 * which performs an immediate DMA copy without mapping. Buffers are recycled via fence when the GPU
 * has finished consuming them.
 *
 * Unlike [net.minecraft.client.renderer.MappableRingBuffer],
 * this class does not use persistent mapping, avoiding the fence issue of wrapping within a single command encoder submit.
 *
 * ## Lifecycle
 * - [upload]: acquire/write data (auto-grows buffer as needed)
 * - [endFrame]: fence the current buffer for recycling
 *
 * @author MukjepScarlet
 */
class DynamicGpuBufferWriter @JvmOverloads constructor(
    val label: String,
    val usage: @GpuBuffer.Usage Int,
    val growPolicy: GrowPolicy = GrowPolicy.DEFAULT,
) {

    // --- State ---
    private var currentBuffer: GpuBuffer? = null
    private var writeOffset: Int = 0
    private var peakBytesThisFrame: Int = 0
    private val closer = GpuBufferDeferredCloser(StaticGpuBufferPool::release)

    /**
     * Upload [data] into the buffer and return a slice.
     *
     * @param data      Byte data to upload. Its [ByteBuffer.remaining] bytes are copied.
     * @param alignment Byte alignment for the slice offset.
     * @return A [GpuBufferSlice] covering the uploaded region.
     */
    @JvmOverloads
    fun upload(data: ByteBuffer, alignment: Int = 1): GpuBufferSlice {
        val byteCount = data.remaining()
        require(byteCount >= 0) { "byteCount must be non-negative" }

        val alignedOffset = if (alignment == 1) writeOffset else Mth.roundToward(writeOffset, alignment)
        val requiredSize = alignedOffset + byteCount

        val buffer = currentBuffer
        if (buffer == null || requiredSize > buffer.size()) {
            // Need bigger buffer: recycle the old one and allocate a new, larger one
            val newSize = growPolicy.getNewSize(requiredSize, buffer?.size()?.toInt() ?: 0)
            if (buffer != null) {
                closer.add(buffer)
            }
            currentBuffer = RenderSystem.getDevice().createBuffer(
                { "$label (${newSize.toLong().formatAsCapacity()})" },
                usage or GpuBuffer.USAGE_COPY_DST,
                newSize.toLong(),
            )
            writeOffset = 0

            if (buffer != null) {
                logger.debug(
                    "$label buffer grown: ${buffer.size()} → ${newSize.toLong().formatAsCapacity()}"
                )
            }
        }

        val buf = currentBuffer!!
        val slice = buf.slice(alignedOffset.toLong(), byteCount.toLong())
        slice.write(data)

        writeOffset = alignedOffset + byteCount
        if (writeOffset > peakBytesThisFrame) {
            peakBytesThisFrame = writeOffset
        }
        return slice
    }

    /**
     * End the current frame: fence the active buffer for deferred recycling.
     * After this call, the next [upload] will allocate a fresh buffer.
     */
    fun endFrame() {
        closer.tryClose()
        val buffer = currentBuffer ?: return
        closer.add(buffer)
        currentBuffer = null
        writeOffset = 0
    }

    /**
     * Peak bytes written in the current frame (for adaptive sizing).
     */
    fun peakBytes(): Int = max(peakBytesThisFrame, writeOffset)

    fun interface GrowPolicy {
        /**
         * Calculate the new buffer size.
         *
         * @param requested The required size for this upload.
         * @param current   The current buffer size, or 0 if uninitialized.
         * @return A new size, which should be >= [requested].
         */
        fun getNewSize(requested: Int, current: Int): Int

        companion object {
            /**
             * 128 bytes padding, minimum 0.
             */
            @JvmField
            val DEFAULT = of(paddingScale = 7, min = 0)

            @JvmStatic
            fun of(paddingScale: Int, min: Int) = GrowPolicy { requested, current ->
                val base = maxOf(min, requested, current)
                val padding = 1 shl paddingScale
                (base + padding - 1) and (padding - 1).inv()
            }
        }
    }
}

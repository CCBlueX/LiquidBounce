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

package net.ccbluex.liquidbounce.render.buffers

import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.buffers.GpuBufferSlice
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.render.write
import net.ccbluex.liquidbounce.utils.text.formatAsCapacity
import net.minecraft.util.Mth
import java.nio.ByteBuffer
import kotlin.math.max

/**
 * Per-frame dynamic [com.mojang.blaze3d.buffers.GpuBuffer] writer for streaming VBO/IBO uploads.
 *
 * Follows vanilla's [net.minecraft.client.renderer.StagedVertexBuffer] pattern:
 * CPU data is written to a properly-flagged GPU buffer via [com.mojang.blaze3d.systems.CommandEncoder.writeToBuffer],
 * which performs an immediate DMA copy without mapping. Buffers are recycled via fence when the GPU
 * has finished consuming them.
 *
 * Unlike [net.minecraft.client.renderer.MappableRingBuffer],
 * this class does not use persistent mapping, avoiding the fence
 * issue of wrapping within a single command encoder submit.
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
) : AutoCloseable {

    // --- State ---
    private var currentBuffer: GpuBuffer? = null
    private var writeOffset: Int = 0
    private var peakBytesThisFrame: Int = 0
    private var highWaterCapacity: Int = 0
    private val pool = FrameGpuBufferPool(label, usage)
    private var closed = false

    /**
     * Upload [data] into the buffer and return a slice.
     *
     * @param data      Byte data to upload. Its [java.nio.Buffer.remaining] bytes are copied.
     * @param alignment Byte alignment for the slice offset.
     * @return A [com.mojang.blaze3d.buffers.GpuBufferSlice] covering the uploaded region.
     */
    @JvmOverloads
    fun upload(data: ByteBuffer, alignment: Int = 1): GpuBufferSlice {
        check(!closed) { "$label writer is closed" }
        require(alignment > 0) { "alignment must be positive" }

        val byteCount = data.remaining()
        require(byteCount >= 0) { "byteCount must be non-negative" }

        var alignedOffset = if (alignment == 1) writeOffset else Mth.roundToward(writeOffset, alignment)
        val requiredSize = Math.addExact(alignedOffset, byteCount)

        val buffer = currentBuffer
        if (buffer == null || requiredSize > buffer.size()) {
            // The old buffer remains valid for this frame and is fenced with its replacement.
            if (buffer != null) {
                pool.retireCurrentFrame(buffer)
            }

            val previousHighWater = highWaterCapacity
            val newCapacity = growPolicy.getNewSize(requiredSize, highWaterCapacity)
            require(newCapacity >= requiredSize) {
                "Grow policy returned $newCapacity bytes for a $requiredSize byte upload"
            }
            highWaterCapacity = newCapacity

            writeOffset = 0
            alignedOffset = 0
            currentBuffer = pool.acquire(highWaterCapacity)

            if (previousHighWater != 0 && newCapacity > previousHighWater) {
                logger.debug(
                    "$label buffer grown: ${previousHighWater.toLong().formatAsCapacity()} " +
                        "→ ${newCapacity.toLong().formatAsCapacity()}"
                )
            }
        }

        val buf = currentBuffer!!
        val slice = buf.slice(alignedOffset.toLong(), byteCount.toLong())
        slice.write(data)

        writeOffset = Math.addExact(alignedOffset, byteCount)
        if (writeOffset > peakBytesThisFrame) {
            peakBytesThisFrame = writeOffset
        }
        return slice
    }

    /**
     * End the current frame: fence all buffers used by this writer for deferred recycling.
     */
    fun endFrame() {
        check(!closed) { "$label writer is closed" }
        currentBuffer?.let(pool::retireCurrentFrame)
        currentBuffer = null
        writeOffset = 0
        pool.endFrame(highWaterCapacity)
        peakBytesThisFrame = 0
    }

    override fun close() {
        if (closed) {
            return
        }
        closed = true

        currentBuffer?.close()
        currentBuffer = null
        writeOffset = 0
        pool.close()
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

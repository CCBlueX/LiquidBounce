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
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.ccbluex.liquidbounce.additions.isSafeForClose
import net.ccbluex.liquidbounce.utils.client.formatAsCapacity
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.render.mapBuffer
import net.minecraft.client.renderer.MappableRingBuffer
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer

/**
 * A growable [GpuBuffer] wrapper for streaming GPU data (e.g. dynamic VBO/IBO uploads).
 *
 * This class acts like a simple linear allocator on top of [MappableRingBuffer]:
 * multiple uploads can append to the current buffer until it runs out of space,
 * then it automatically rotates to the next backing buffer. If the requested
 * upload is larger than the backing buffer size, the ring is grown.
 *
 * Behavior for each upload(data):
 * 1) If there is enough space after the last write, append to the current buffer.
 * 2) If the buffer is large enough but the remaining space is not, rotate() and write from offset 0.
 * 3) If the entire ring buffer is too small, grow it and write from offset 0.
 *
 * @author MukjepScarlet
 */
class GrowableMappableRingBuffer @JvmOverloads constructor(
    val label: String,
    val usage: @GpuBuffer.Usage Int,
    val growPolicy: GrowPolicy = GrowPolicy.DEFAULT,
) {

    private var ring: MappableRingBuffer? = null

    /**
     * Current write offset (in bytes) into the *current* GPU buffer
     * returned by [MappableRingBuffer.currentBuffer].
     */
    private var currentOffset: Int = 0

    /**
     * Ensure there is a ring buffer allocated that can hold at least [minSize] bytes
     * in a single contiguous upload. If not, a new [MappableRingBuffer] is created
     * with a size determined by [growPolicy], and the old one is scheduled for
     * deferred close.
     */
    private fun ensureCapacityFor(minSize: Int) {
        val current = ring
        if (current == null || current.size() < minSize) {
            val newSize = growPolicy.getNewSize(minSize, current?.size() ?: 0)
            current?.let {
                // Defer closing the old ring buffer to avoid races with GPU usage.
                BUFFERS_TO_CLOSE += it
            }
            ring = MappableRingBuffer(
                Suppliers.ofInstance(label),
                usage or GpuBuffer.USAGE_MAP_WRITE,
                newSize
            )
            currentOffset = 0

            logger.debug("$label buffer grown to $newSize bytes (${newSize.toLong().formatAsCapacity()})")
        }
    }

    /**
     * Upload [data] into the ring buffer and return a slice covering the written region.
     *
     * Upload decision:
     * 1. If the current buffer has enough remaining space, append at [currentOffset].
     * 2. Otherwise, if the buffer is large enough, rotate() and write from offset 0.
     * 3. If the buffer is still too small for this upload, grow the ring and write from 0.
     *
     * @param data The data to upload. Its remaining() bytes will be copied.
     * @return The uploaded [GpuBufferSlice]. Its lifetime is tied to the underlying ring buffer.
     */
    fun upload(data: ByteBuffer): GpuBufferSlice {
        val byteCount = data.remaining()
        require(byteCount >= 0) { "byteCount must be non-negative" }

        // Step 0: make sure we at least can hold this upload once (case 3)
        ensureCapacityFor(byteCount)

        // ring is guaranteed non-null and large enough at this point
        var ring = checkNotNull(this.ring) { "Ring buffer not initialized" }
        var capacity = ring.size()

        // Step 1 / 2: decide whether to append or rotate
        if (currentOffset + byteCount > capacity) {
            // Not enough remaining space in this backing buffer.
            // Case 2: buffer is big enough, but remaining < byteCount → rotate and reset offset.
            rotate()

            // After rotate we might be on a different backing buffer,
            // but size() is the same for all buffers in the ring.
            ring = checkNotNull(this.ring)
            capacity = ring.size()
        }

        // At this point we know byteCount <= capacity and currentOffset + byteCount <= capacity.
        val buffer = ring.currentBuffer()
        val sliceOffset = currentOffset.toLong()
        val slice = buffer.slice(sliceOffset, byteCount.toLong())

        // Map the buffer slice and copy data into it.
        slice.mapBuffer(read = false, write = true).use { mappedView ->
            MemoryUtil.memCopy(data, mappedView.data())
        }

        currentOffset += byteCount
        return slice
    }

    /**
     * Manually rotate the ring buffer to the next backing buffer and reset the
     * current write offset. This is useful for per-frame usage patterns where
     * each frame starts writing from a fresh buffer.
     */
    fun rotate() {
        ring?.rotate()
        currentOffset = 0
    }

    /**
     * Clear the ring buffer and release all GPU resources.
     */
    fun clear() {
        ring?.close()
        ring = null
        currentOffset = 0
    }

    fun interface GrowPolicy {
        /**
         * Calculate the new size of the ring buffer.
         *
         * @param requested The requested size for this upload.
         * @param current   The current buffer size, or 0 if uninitialized.
         * @return A new size, which should be >= [requested].
         */
        fun getNewSize(requested: Int, current: Int): Int

        companion object {
            /**
             * 128 bytes padding, minimum 0
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

    companion object {
        /**
         * [MappableRingBuffer] to be closed.
         */
        @JvmStatic
        private val BUFFERS_TO_CLOSE = ObjectArrayList<MappableRingBuffer>()

        /**
         * Checks and closes discarded [MappableRingBuffer] instances that are safe to release.
         *
         * This method cleans up old backing buffers that were replaced when the ring buffer grew.
         * It should be called periodically (e.g. end of frame) to ensure GPU resources are released.
         *
         * @see net.ccbluex.liquidbounce.injection.mixins.blaze3d.MixinRenderSystem.onFlipFrame
         */
        @JvmStatic
        fun cleanup() {
            BUFFERS_TO_CLOSE.removeIf {
                if (it.isSafeForClose) {
                    it.close()
                    true
                } else {
                    false
                }
            }
        }
    }
}

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
import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.utils.text.formatAsCapacity

/**
 * Reuses dynamic GPU buffers after the frame that referenced them has completed on the GPU.
 *
 * A pool is owned by one [DynamicGpuBufferWriter]. All buffers retired during a frame share
 * one fence, which is created immediately before vanilla submits the frame command buffer.
 */
internal class FrameGpuBufferPool(
    private val label: String,
    private val usage: @GpuBuffer.Usage Int,
    private val maxAvailableBuffers: Int = 3,
) : AutoCloseable {

    private val available = GpuBufferAvailableCache()
    private val usedThisFrame = ArrayList<GpuBuffer>()
    private val closer = GpuBufferDeferredCloser(available::add)
    private var closed = false

    fun acquire(minCapacity: Int): GpuBuffer {
        check(!closed) { "$label pool is closed" }
        require(minCapacity >= 0) { "minCapacity must be non-negative" }

        recycleCompleted(minCapacity)

        available.takeBest(minCapacity.toLong(), Long.MAX_VALUE)?.let { return it }

        return RenderSystem.getDevice().createBuffer(
            { "$label (${minCapacity.toLong().formatAsCapacity()})" },
            usage or GpuBuffer.USAGE_COPY_DST,
            minCapacity.toLong(),
        )
    }

    fun retireCurrentFrame(buffer: GpuBuffer) {
        check(!closed) { "$label pool is closed" }
        check(!buffer.isClosed()) { "Cannot retire a closed GPU buffer" }
        usedThisFrame += buffer
    }

    /**
     * Recycles completed frames and fences all buffers used by the current frame.
     */
    fun endFrame(minReusableCapacity: Int) {
        check(!closed) { "$label pool is closed" }
        recycleCompleted(minReusableCapacity)

        if (usedThisFrame.isEmpty()) {
            return
        }

        closer.add(usedThisFrame)
        usedThisFrame.clear()
    }

    override fun close() {
        if (closed) {
            return
        }
        closed = true

        available.close()
        usedThisFrame.forEach(GpuBuffer::close)
        closer.close()
        usedThisFrame.clear()
    }

    private fun recycleCompleted(minReusableCapacity: Int) {
        closer.tryClose()
        available.discardSmallerThan(minReusableCapacity.toLong())
        available.trimTo(maxAvailableBuffers)
    }
}

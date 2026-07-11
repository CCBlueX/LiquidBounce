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

import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.buffers.Std140Builder
import net.ccbluex.liquidbounce.render.ClientUniformDefine
import net.ccbluex.liquidbounce.utils.render.writeStd140

/**
 * Retains the last uniform slice across frames and uploads a new one only when [T] changes.
 *
 * The backing ring buffer prevents an update from overwriting data still consumed by the GPU.
 */
class CachedUniform<T : Any>(
    define: ClientUniformDefine,
    private val writer: Std140Builder.(T) -> Unit,
) : AutoCloseable {

    private val buffers = define.createRingBuffer()
    private var lastValue: T? = null
    private var currentSlice: GpuBufferSlice? = null

    fun get(value: T): GpuBufferSlice {
        currentSlice?.takeIf { value == lastValue }?.let { return it }

        if (currentSlice != null) {
            buffers.rotate()
        }

        return buffers.currentBuffer().slice().also { slice ->
            slice.writeStd140 { writer(value) }
            lastValue = value
            currentSlice = slice
        }
    }

    override fun close() {
        buffers.close()
    }

}

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

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import net.ccbluex.fastutil.Pool
import net.ccbluex.liquidbounce.utils.render.begin

object ClientTesselator {

    private const val BUFFER_SIZE = 0xC0000

    @JvmField
    val Shared = ByteBufferBuilder(BUFFER_SIZE)

    private val bufferAllocatorPool = Pool(
        initializer = { ByteBufferBuilder(BUFFER_SIZE) },
        finalizer = ByteBufferBuilder::clear,
    )

    @JvmStatic
    fun begin(
        pipeline: RenderPipeline,
        allocatorInUse: MutableCollection<ByteBufferBuilder>,
    ): BufferBuilder {
        val allocator = bufferAllocatorPool.borrow()
        allocatorInUse += allocator
        return allocator.begin(pipeline)
    }

    @JvmStatic
    fun recycleAll(allocatorInUse: Iterable<ByteBufferBuilder>) {
        bufferAllocatorPool.recycleAll(allocatorInUse)
    }

}

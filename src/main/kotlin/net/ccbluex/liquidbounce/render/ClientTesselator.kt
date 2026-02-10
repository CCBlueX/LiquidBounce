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
import com.mojang.blaze3d.textures.GpuTextureView
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap
import net.ccbluex.liquidbounce.utils.render.begin

object ClientTesselator {

    private const val BUFFER_SIZE = 0xC0000

    @JvmField
    val Shared = ByteBufferBuilder(BUFFER_SIZE)

    private val bufferAllocators = Reference2ReferenceOpenHashMap<RenderPipeline, ByteBufferBuilder>()

    @JvmStatic
    internal val texQuadsSpecialAllocators = Reference2ReferenceOpenHashMap<GpuTextureView, ByteBufferBuilder>()

    @JvmStatic
    fun allocator(pipeline: RenderPipeline): ByteBufferBuilder =
        bufferAllocators.getOrPut(pipeline) { ByteBufferBuilder(BUFFER_SIZE) }

    @JvmStatic
    fun begin(pipeline: RenderPipeline): BufferBuilder =
        allocator(pipeline).begin(pipeline)

    @JvmStatic
    fun allocator(texture: GpuTextureView): ByteBufferBuilder =
        texQuadsSpecialAllocators.getOrPut(texture) { ByteBufferBuilder(BUFFER_SIZE) }

    @JvmStatic
    fun begin(texture: GpuTextureView): BufferBuilder =
        allocator(texture).begin(ClientRenderPipelines.TexQuads)

}

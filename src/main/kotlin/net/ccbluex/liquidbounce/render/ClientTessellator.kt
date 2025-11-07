/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap
import net.minecraft.client.render.BufferBuilder
import net.minecraft.client.util.BufferAllocator
import kotlin.jvm.JvmStatic

object ClientTessellator {

    private const val BUFFER_SIZE = 0xC0000

    private val bufferAllocators = Reference2ReferenceOpenHashMap<RenderPipeline, BufferAllocator>()

    @JvmStatic
    internal val texQuadsSpecialAllocators = Reference2ReferenceOpenHashMap<GpuTextureView, BufferAllocator>()

    @JvmStatic
    fun allocator(pipeline: RenderPipeline): BufferAllocator =
        bufferAllocators.getOrPut(pipeline) { BufferAllocator(BUFFER_SIZE) }

    @JvmStatic
    fun begin(pipeline: RenderPipeline): BufferBuilder =
        BufferBuilder(
            allocator(pipeline),
            pipeline.vertexFormatMode,
            pipeline.vertexFormat
        )

    @JvmStatic
    fun allocator(texture: GpuTextureView): BufferAllocator =
        texQuadsSpecialAllocators.getOrPut(texture) { BufferAllocator(BUFFER_SIZE) }

    @JvmStatic
    fun begin(texture: GpuTextureView): BufferBuilder =
        BufferBuilder(
            allocator(texture),
            ClientRenderPipelines.TexQuads.vertexFormatMode,
            ClientRenderPipelines.TexQuads.vertexFormat
        )

}

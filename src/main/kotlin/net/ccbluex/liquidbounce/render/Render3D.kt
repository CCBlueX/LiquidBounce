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

@file:Suppress("detekt:TooManyFunctions")

package net.ccbluex.liquidbounce.render

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.GpuTextureView
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap
import net.ccbluex.fastutil.fastIterator
import net.ccbluex.liquidbounce.render.engine.type.Vec3
import net.minecraft.client.gl.Framebuffer
import net.minecraft.client.render.BufferBuilder
import net.minecraft.client.render.Camera
import net.minecraft.client.render.Tessellator
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.Position
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i

/**
 * Context representing the rendering environment.
 */
sealed class RenderEnvironment(val framebuffer: Framebuffer) {
    var isBatchMode: Boolean = false
        private set

    fun getOrCreateBuffer(pipeline: RenderPipeline): BufferBuilder {
        return if (isBatchMode) {
            batchBuffer.computeIfAbsent(pipeline, ClientTessellator::begin)
        } else {
            Tessellator.getInstance().begin(
                pipeline.vertexFormatMode,
                pipeline.vertexFormat
            )
        }
    }

    fun getOrCreateBuffer(texture: GpuTextureView): BufferBuilder {
        return if (isBatchMode) {
            texQuadBatchBuffer.computeIfAbsent(texture, ClientTessellator::begin)
        } else {
            Tessellator.getInstance().begin(
                ClientRenderPipelines.TexQuads.vertexFormatMode,
                ClientRenderPipelines.TexQuads.vertexFormat
            )
        }
    }

    fun startBatch() {
        if (isBatchMode) commitBatch()
        isBatchMode = true
    }

    fun commitBatch() {
        require(isBatchMode) {
            "Current environment is not in batch mode!"
        }

        batchBuffer.fastIterator().forEach { (pipeline, bufferBuilder) ->
            bufferBuilder.endNullable()?.let {
                pipeline.draw(it)
                ClientTessellator.allocator(pipeline).clear()
            }
        }
        batchBuffer.clear()

        texQuadBatchBuffer.fastIterator().forEach { (gpuTexture, bufferBuilder) ->
            bufferBuilder.endNullable()?.let {
                RenderSystem.setShaderTexture(0, gpuTexture) // Sampler0
                ClientRenderPipelines.TexQuads.draw(it)
                ClientTessellator.allocator(gpuTexture).clear()
            }
        }
        texQuadBatchBuffer.clear()
    }

    companion object {
        @JvmStatic
        private val batchBuffer = Reference2ReferenceOpenHashMap<RenderPipeline, BufferBuilder>()

        /**
         * For [ClientRenderPipelines.TexQuads] only. Each texture has its buffer builder.
         */
        @JvmStatic
        private val texQuadBatchBuffer = Reference2ReferenceOpenHashMap<GpuTextureView, BufferBuilder>()
    }
}

class WorldRenderEnvironment(
    framebuffer: Framebuffer,
    val matrixStack: MatrixStack,
    val camera: Camera,
) : RenderEnvironment(framebuffer) {
    fun relativeToCamera(pos: Vec3): Vec3d {
        return Vec3d(pos.x.toDouble() - camera.pos.x, pos.y.toDouble() - camera.pos.y, pos.z.toDouble() - camera.pos.z)
    }

    fun relativeToCamera(pos: Position): Vec3d {
        return Vec3d(pos.x - camera.pos.x, pos.y - camera.pos.y, pos.z - camera.pos.z)
    }

    fun relativeToCamera(pos: Vec3i): Vec3d {
        return Vec3d(pos.x.toDouble() - camera.pos.x, pos.y.toDouble() - camera.pos.y, pos.z.toDouble() - camera.pos.z)
    }
}

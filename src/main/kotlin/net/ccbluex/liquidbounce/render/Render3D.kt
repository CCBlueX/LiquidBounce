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
import com.mojang.blaze3d.textures.GpuTextureView
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap
import net.ccbluex.fastutil.fastIterator
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.type.Vec3f
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.vertex.BufferBuilder
import net.minecraft.client.Camera
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.core.Position
import net.minecraft.world.phys.Vec3
import net.minecraft.core.Vec3i
import org.joml.Vector3fc

/**
 * Context representing the rendering environment.
 *
 * @param framebuffer The render target framebuffer.
 */
sealed class RenderEnvironment(val framebuffer: RenderTarget) {

    val shaderTextures = arrayOfNulls<GpuTextureView>(TEXTURE_COUNT)
    var shaderColor = Color4b.WHITE

    var isBatchMode: Boolean = false
        private set

    fun getOrCreateBuffer(pipeline: RenderPipeline): BufferBuilder {
        return if (isBatchMode) {
            batchBuffer.computeIfAbsent(pipeline, ClientTesselator::begin)
        } else {
            Tesselator.getInstance().begin(pipeline.vertexFormatMode, pipeline.vertexFormat)
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
            bufferBuilder.build()?.let {
                draw(pipeline, it)
                ClientTesselator.allocator(pipeline).clear()
            }
        }
        batchBuffer.clear()
    }

    companion object {
        const val TEXTURE_COUNT = 12

        @JvmStatic
        private val batchBuffer = Reference2ReferenceOpenHashMap<RenderPipeline, BufferBuilder>()
    }
}

class WorldRenderEnvironment(
    framebuffer: RenderTarget,
    val matrixStack: PoseStack,
    val camera: Camera,
) : RenderEnvironment(framebuffer) {
    fun relativeToCamera(pos: Vec3f): Vec3 = pos.relativeTo(camera)

    fun relativeToCamera(pos: Position): Vec3 = pos.relativeTo(camera)

    fun relativeToCamera(pos: Vec3i): Vec3 = pos.relativeTo(camera)
}

fun Vec3f.relativeTo(camera: Camera): Vec3 = Vec3(
    x - camera.position().x,
    y - camera.position().y,
    z - camera.position().z,
)

fun Position.relativeTo(camera: Camera): Vec3 = Vec3(
    x() - camera.position().x,
    y() - camera.position().y,
    z() - camera.position().z,
)

fun Vec3i.relativeTo(camera: Camera): Vec3 = Vec3(
    x.toDouble() - camera.position().x,
    y.toDouble() - camera.position().y,
    z.toDouble() - camera.position().z,
)

fun Vector3fc.relativeTo(camera: Camera): Vec3 = Vec3(
    x() - camera.position().x,
    y() - camera.position().y,
    z() - camera.position().z,
)

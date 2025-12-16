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
import net.minecraft.client.gl.Framebuffer
import net.minecraft.client.render.BufferBuilder
import net.minecraft.client.render.Camera
import net.minecraft.client.render.Tessellator
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.Position
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import org.joml.Vector3fc

/**
 * Context representing the rendering environment.
 *
 * @param framebuffer The render target framebuffer.
 */
sealed class RenderEnvironment(val framebuffer: Framebuffer) {

    val shaderTextures = arrayOfNulls<GpuTextureView>(TEXTURE_COUNT)
    var shaderColor = Color4b.WHITE

    var isBatchMode: Boolean = false
        private set

    fun getOrCreateBuffer(pipeline: RenderPipeline): BufferBuilder {
        return if (isBatchMode) {
            batchBuffer.computeIfAbsent(pipeline, ClientTessellator::begin)
        } else {
            Tessellator.getInstance().begin(pipeline.vertexFormatMode, pipeline.vertexFormat)
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
                draw(pipeline, it)
                ClientTessellator.allocator(pipeline).clear()
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
    framebuffer: Framebuffer,
    val matrixStack: MatrixStack,
    val camera: Camera,
) : RenderEnvironment(framebuffer) {
    fun relativeToCamera(pos: Vec3f): Vec3d = pos.relativeTo(camera)

    fun relativeToCamera(pos: Position): Vec3d = pos.relativeTo(camera)

    fun relativeToCamera(pos: Vec3i): Vec3d = pos.relativeTo(camera)
}

fun Vec3f.relativeTo(camera: Camera): Vec3d = Vec3d(
    x - camera.cameraPos.x,
    y - camera.cameraPos.y,
    z - camera.cameraPos.z,
)

fun Position.relativeTo(camera: Camera): Vec3d = Vec3d(
    x - camera.cameraPos.x,
    y - camera.cameraPos.y,
    z - camera.cameraPos.z,
)

fun Vec3i.relativeTo(camera: Camera): Vec3d = Vec3d(
    x.toDouble() - camera.cameraPos.x,
    y.toDouble() - camera.cameraPos.y,
    z.toDouble() - camera.cameraPos.z,
)

fun Vector3fc.relativeTo(camera: Camera): Vec3d = Vec3d(
    x() - camera.cameraPos.x,
    y() - camera.cameraPos.y,
    z() - camera.cameraPos.z,
)

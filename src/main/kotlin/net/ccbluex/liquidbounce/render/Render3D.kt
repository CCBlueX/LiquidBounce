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

@file:Suppress("detekt:TooManyFunctions")

package net.ccbluex.liquidbounce.render

import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.MeshData
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.Tesselator
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap
import net.ccbluex.fastutil.fastIterator
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.type.Vec3f
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.collection.Pools
import net.minecraft.client.Camera
import net.minecraft.client.renderer.texture.AbstractTexture
import net.minecraft.core.Position
import net.minecraft.core.Vec3i
import net.minecraft.world.phys.Vec3
import org.joml.Vector3fc
import java.util.Collections.singletonMap
import java.util.function.Function

inline fun <T> usePoseStack(block: PoseStack.() -> T): T {
    val matrices = Pools.MatStack.borrow()
    try {
        return block(matrices)
    } finally {
        Pools.MatStack.recycle(matrices)
    }
}

inline fun PoseStack.withPush(block: PoseStack.() -> Unit) {
    pushPose()
    try {
        block()
    } finally {
        popPose()
    }
}

inline fun PoseStack.translate(vec3i: Vec3i) =
    translate(vec3i.x.toFloat(), vec3i.y.toFloat(), vec3i.z.toFloat())

/**
 * Context representing the rendering environment.
 *
 * @param renderTarget The render target framebuffer.
 */
class WorldRenderEnvironment(
    val renderTarget: RenderTarget,
    val matrixStack: PoseStack,
    val camera: Camera = mc.gameRenderer.mainCamera,
) {

    fun relativeToCamera(pos: Vec3f): Vec3 = pos.relativeTo(camera)

    fun relativeToCamera(pos: Position): Vec3 = pos.relativeTo(camera)

    fun relativeToCamera(pos: Vec3i): Vec3 = pos.relativeTo(camera)

    var shaderColor = Color4b.WHITE

    var isBatchMode: Boolean = false
        private set

    fun uniform(name: String, value: GpuBufferSlice) {
        uniforms[name] = value
    }

    fun getOrCreateBuffer(texture: AbstractTexture): BufferBuilder {
        return if (isBatchMode) {
            texQuadsBatchBuffer.computeIfAbsent(texture) {
                ClientTesselator.begin(texture.textureView)
            }
        } else {
            val pipeline = ClientRenderPipelines.TexQuads
            Tesselator.getInstance().begin(pipeline.vertexFormatMode, pipeline.vertexFormat)
        }
    }

    fun getOrCreateBuffer(pipeline: RenderPipeline): BufferBuilder {
        return if (isBatchMode) {
            batchBuffer.computeIfAbsent(pipeline, Function(ClientTesselator::begin))
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
                draw(pipeline, it, emptyMap())
                ClientTesselator.allocator(pipeline).clear()
            }
        }
        batchBuffer.clear()

        texQuadsBatchBuffer.fastIterator().forEach { (texture, bufferBuilder) ->
            bufferBuilder.build()?.let {
                draw(ClientRenderPipelines.TexQuads, it, singletonMap("Sampler0", texture))
                ClientTesselator.allocator(texture.textureView).clear()
            }
        }
        texQuadsBatchBuffer.clear()

        uniforms.clear()
    }

    fun draw(
        pipeline: RenderPipeline,
        meshData: MeshData,
        shaderTextureProvider: Map<String, AbstractTexture>,
    ) = drawMesh(
        pipeline,
        meshData,
        this.renderTarget,
        colorModulator = shaderColor,
        shaderTextures = shaderTextureProvider,
        uniforms = uniforms,
    )

    companion object {
        @JvmStatic
        private val batchBuffer =
            Reference2ReferenceOpenHashMap<RenderPipeline, BufferBuilder>()

        /**
         * @see ClientRenderPipelines.TexQuads
         */
        @JvmStatic
        private val texQuadsBatchBuffer =
            Reference2ReferenceOpenHashMap<AbstractTexture, BufferBuilder>()

        @JvmStatic
        private val uniforms = Object2ObjectOpenHashMap<String, GpuBufferSlice>()
    }
}

private fun Vec3f.relativeTo(camera: Camera): Vec3 = Vec3(
    x - camera.position().x,
    y - camera.position().y,
    z - camera.position().z,
)

private fun Position.relativeTo(camera: Camera): Vec3 = Vec3(
    x() - camera.position().x,
    y() - camera.position().y,
    z() - camera.position().z,
)

private fun Vec3i.relativeTo(camera: Camera): Vec3 = Vec3(
    x.toDouble() - camera.position().x,
    y.toDouble() - camera.position().y,
    z.toDouble() - camera.position().z,
)

private fun Vector3fc.relativeTo(camera: Camera): Vec3 = Vec3(
    x() - camera.position().x,
    y() - camera.position().y,
    z() - camera.position().z,
)

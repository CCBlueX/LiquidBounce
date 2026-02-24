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

@file:Suppress("detekt:TooManyFunctions", "NOTHING_TO_INLINE")

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
import net.ccbluex.liquidbounce.render.mesh.MeshDraw
import net.ccbluex.liquidbounce.render.mesh.MeshDraw.Companion.bindAndDraw
import net.ccbluex.liquidbounce.render.mesh.MeshDraw.Companion.toMeshDraw
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.collection.Pools
import net.ccbluex.liquidbounce.utils.render.begin
import net.minecraft.client.Camera
import net.minecraft.client.renderer.texture.AbstractTexture
import net.minecraft.core.Position
import net.minecraft.core.Vec3i
import net.minecraft.world.phys.Vec3
import org.joml.Vector3fc
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

    /**
     * For texture `Sampler0`
     */
    fun getOrCreateBuffer(texture: AbstractTexture): BufferBuilder {
        return if (isBatchMode) {
            texQuadsBatchBuffer.computeIfAbsent(texture) {
                ClientTesselator.begin(texture.textureView)
            }
        } else {
            val pipeline = ClientRenderPipelines.TexQuads
            Tesselator.getInstance().begin(pipeline)
        }
    }

    fun getOrCreateBuffer(pipeline: RenderPipeline): BufferBuilder {
        return if (isBatchMode) {
            batchBuffer.computeIfAbsent(pipeline, Function(ClientTesselator::begin))
        } else {
            Tesselator.getInstance().begin(pipeline)
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

        val dynamicTransforms = getDynamicTransformsUniform(colorModulator = this.shaderColor)
        commitPlainBatch(dynamicTransforms)
        commitTexturedBatch(dynamicTransforms)

        uniforms.clear()
    }

    private fun commitPlainBatch(dynamicTransforms: GpuBufferSlice) {
        batchBuffer.fastIterator().forEach { (pipeline, bufferBuilder) ->
            bufferBuilder.build()?.use {
                draws.add(pipeline to it.toMeshDraw(pipeline))
            }
            ClientTesselator.clear(pipeline)
        }
        batchBuffer.clear()

        if (draws.isEmpty()) return

        renderTarget.createRenderPass(
            { "WorldRenderEnvironment Batch draw" },
            allowOverride = true,
        ).use { pass ->
            pass.setupRenderTypeScissor()
            pass.bindDefaultUniforms()
            pass.bindDynamicTransformsUniform(dynamicTransforms)
            pass.setUniforms(uniforms)

            draws.forEach { (pipeline, meshDraw) ->
                pass.setPipeline(pipeline as RenderPipeline)
                pass.bindAndDraw(meshDraw)
            }
        }

        draws.clear()
    }

    private fun commitTexturedBatch(dynamicTransforms: GpuBufferSlice) {
        val pipeline = ClientRenderPipelines.TexQuads
        texQuadsBatchBuffer.fastIterator().forEach { (texture, bufferBuilder) ->
            bufferBuilder.build()?.use {
                draws.add(texture to it.toMeshDraw(pipeline))
            }
            ClientTesselator.clear(texture.textureView)
        }
        texQuadsBatchBuffer.clear()

        if (draws.isEmpty()) return

        renderTarget.createRenderPass(
            { "WorldRenderEnvironment Batch draw" },
            allowOverride = true,
        ).use { pass ->
            pass.setupRenderTypeScissor()
            pass.bindDefaultUniforms()
            pass.bindDynamicTransformsUniform(dynamicTransforms)
            pass.setUniforms(uniforms)

            pass.setPipeline(pipeline)
            draws.forEach { (texture, meshDraw) ->
                pass.bindTexture("Sampler0", texture as AbstractTexture)
                pass.bindAndDraw(meshDraw)
            }
        }

        draws.clear()
    }

    /**
     * Reference: (1.21.5-10/Yarn: RenderLayer.MultiPhase.draw)
     * @see net.minecraft.client.renderer.rendertype.RenderType.draw
     */
    fun immediateDraw(
        pipeline: RenderPipeline,
        meshData: MeshData,
        textures: Map<String, AbstractTexture>,
    ) {
        val dynamicTransforms = getDynamicTransformsUniform(colorModulator = this.shaderColor)
        val draw = meshData.use { it.toMeshDraw(pipeline) }

        renderTarget.createRenderPass(
            { "WorldRenderEnvironment Immediate draw" },
            allowOverride = true,
        ).use { pass ->
            pass.setupRenderTypeScissor()
            pass.bindDefaultUniforms()
            pass.bindDynamicTransformsUniform(dynamicTransforms)
            pass.setUniforms(uniforms)

            pass.setPipeline(pipeline)
            pass.bindTextures(textures)
            pass.bindAndDraw(draw)
        }
    }

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

        @JvmStatic
        private val draws = ArrayList<Pair<Any, MeshDraw>>()
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

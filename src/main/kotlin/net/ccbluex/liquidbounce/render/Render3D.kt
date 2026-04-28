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
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.MeshData
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.ccbluex.fastutil.Pool
import net.ccbluex.fastutil.fastIterator
import net.ccbluex.liquidbounce.render.engine.type.Vec3f
import net.ccbluex.liquidbounce.render.mesh.MeshDraw
import net.ccbluex.liquidbounce.render.mesh.MeshDraw.Companion.bindAndDraw
import net.ccbluex.liquidbounce.render.mesh.MeshDraw.Companion.toMeshDraw
import net.ccbluex.liquidbounce.utils.collection.Pools
import net.ccbluex.liquidbounce.utils.kotlin.immutableCopy
import net.ccbluex.liquidbounce.utils.kotlin.memorizingFunction
import net.ccbluex.liquidbounce.utils.render.begin
import net.ccbluex.liquidbounce.utils.render.reset
import net.minecraft.client.Camera
import net.minecraft.client.renderer.texture.AbstractTexture
import net.minecraft.core.Position
import net.minecraft.core.Vec3i
import net.minecraft.world.phys.Vec3
import org.joml.Vector3fc
import java.util.Comparator
import java.util.IdentityHashMap
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
 * Submission strategy for geometry started in [WorldRenderEnvironment].
 */
enum class DrawMode {
    IMMEDIATE,
    BATCH,
}

/**
 * Buffer grouping key used by [BatchCollector].
 */
@JvmRecord
data class RenderBufferKey(
    val pipeline: RenderPipeline,
    val textures: Map<String, AbstractTexture> = emptyMap(),
    val uniforms: Map<String, GpuBufferSlice> = emptyMap(),
)

internal class BatchCollector {

    private val bufferAllocatorInUse = ObjectArrayList<ByteBufferBuilder>()
    private val bufferBuilders = Object2ObjectOpenHashMap<RenderBufferKey, BufferBuilder>()
    private val builtBuffers = ObjectArrayList<Pair<RenderBufferKey, MeshDraw>>()

    private val keyCache = memorizingFunction(Object2ObjectOpenHashMap<RenderPipeline, RenderBufferKey>()) {
        RenderBufferKey(it)
    }

    fun key(
        pipeline: RenderPipeline,
        textures: Map<String, AbstractTexture>,
        uniforms: Map<String, GpuBufferSlice>,
    ): RenderBufferKey {
        if (textures.isEmpty() && uniforms.isEmpty()) {
            return keyCache.apply(pipeline)
        }

        return RenderBufferKey(
            pipeline,
            textures = textures.immutableCopy(),
            uniforms = uniforms.immutableCopy(),
        )
    }

    fun start(key: RenderBufferKey): VertexConsumer =
        bufferBuilders.computeIfAbsent(key, Function {
            ClientTesselator.begin(it.pipeline, bufferAllocatorInUse)
        })

    @JvmOverloads
    fun flush(renderTarget: RenderTarget, dynamicTransforms: GpuBufferSlice = getDynamicTransformsUniform()) {
        try {
            if (bufferBuilders.isEmpty()) {
                return
            }

            bufferBuilders.fastIterator().forEach { (key, builder) ->
                builder.build()?.use { meshData ->
                    builtBuffers += key to meshData.toMeshDraw(key.pipeline)
                }
            }
            bufferBuilders.clear()

            if (builtBuffers.isEmpty()) {
                return
            }

            builtBuffers.sortWith(PAIR_COMPARATOR)

            renderTarget.createRenderPass(
                { "WorldRenderEnvironment Batch draw" },
                allowOverride = true,
            ).use { pass ->
                pass.setupRenderTypeScissor()
                pass.bindDefaultUniforms()
                pass.bindDynamicTransformsUniform(dynamicTransforms)

                builtBuffers.forEach { (key, meshDraw) ->
                    pass.setPipeline(key.pipeline)
                    pass.setUniforms(key.uniforms)
                    pass.bindTextures(key.textures)
                    pass.bindAndDraw(meshDraw)
                }
            }
        } finally {
            builtBuffers.clear()
            bufferBuilders.clear()
            ClientTesselator.recycleAll(bufferAllocatorInUse)
            bufferAllocatorInUse.clear()
        }
    }

    companion object {
        private val KEY_COMPARATOR = Comparator.comparingInt<RenderBufferKey> { it.pipeline.sortKey }
            .thenComparingInt { it.textures.size }
            .thenComparingInt { it.uniforms.size }
            .thenComparingInt { it.hashCode() }

        private val PAIR_COMPARATOR = Comparator.comparing(Function(Pair<RenderBufferKey, *>::first), KEY_COMPARATOR)
    }
}

/**
 * Context representing the rendering environment.
 *
 * @param renderTarget The render target framebuffer.
 */
class WorldRenderEnvironment internal constructor(
    val renderTarget: RenderTarget,
    val poseStack: PoseStack,
    val camera: Camera,
    private val batchCollector: BatchCollector,
    private val frameBoundCollector: Boolean,
) {
    @PublishedApi
    internal var drawMode: DrawMode = DrawMode.IMMEDIATE

    private var pendingImmediateDraws: IdentityHashMap<BufferBuilder, RenderBufferKey>? = null

    /**
     * Converts a world-space position to the camera-relative coordinate system.
     */
    fun relativeToCamera(pos: Vec3f): Vec3 = pos.relativeTo(camera)

    /**
     * Converts a world-space position to the camera-relative coordinate system.
     */
    fun relativeToCamera(pos: Position): Vec3 = pos.relativeTo(camera)

    /**
     * Converts a world-space position to the camera-relative coordinate system.
     */
    fun relativeToCamera(pos: Vec3i): Vec3 = pos.relativeTo(camera)

    /**
     * Temporarily switches the environment to batch mode.
     */
    inline fun batch(block: WorldRenderEnvironment.() -> Unit) = withMode(DrawMode.BATCH, block)

    /**
     * Temporarily switches the environment to immediate mode.
     */
    inline fun immediate(block: WorldRenderEnvironment.() -> Unit) = withMode(DrawMode.IMMEDIATE, block)

    /**
     * Low-level draw entrypoint.
     *
     * Prefer [net.ccbluex.liquidbounce.render.drawCustomMesh] for regular use.
     */
    fun start(
        pipeline: RenderPipeline,
        textures: Map<String, AbstractTexture> = emptyMap(),
        uniforms: Map<String, GpuBufferSlice> = emptyMap(),
    ): VertexConsumer {
        val key = batchCollector.key(pipeline, textures, uniforms)
        val shouldUseBatch = drawMode == DrawMode.BATCH && !pipeline.requiresImmediateDrawInBatch()

        if (shouldUseBatch) {
            return batchCollector.start(key)
        }

        val immediateBuilder = ClientTesselator.Shared.begin(pipeline)
        val pending = pendingImmediateDraws ?: immediateDrawMapPool.borrow().also {
            pendingImmediateDraws = it
        }
        pending[immediateBuilder] = key
        return immediateBuilder
    }

    /**
     * Low-level completion for a [VertexConsumer] obtained from [start].
     *
     * Prefer [net.ccbluex.liquidbounce.render.drawCustomMesh] for regular use.
     */
    fun finish(consumer: VertexConsumer, submit: Boolean = true) {
        val builder = consumer as? BufferBuilder ?: return
        val pending = pendingImmediateDraws ?: return
        val key = pending.remove(builder) ?: return

        if (pending.isEmpty()) {
            pendingImmediateDraws = null
            immediateDrawMapPool.recycle(pending)
        }

        if (submit) {
            builder.build()?.use { meshData ->
                drawImmediate(key, meshData)
            }
        }
    }

    @PublishedApi
    internal fun flushBatchIfLocalEnvironment() {
        if (!frameBoundCollector) {
            batchCollector.flush(renderTarget)
        }
    }

    @PublishedApi
    internal inline fun withMode(mode: DrawMode, block: WorldRenderEnvironment.() -> Unit) {
        val previousMode = drawMode
        drawMode = mode

        try {
            block(this)
        } finally {
            drawMode = previousMode
        }
    }

    /**
     * Reference: (1.21.5-10/Yarn: RenderLayer.MultiPhase.draw)
     * @see net.minecraft.client.renderer.rendertype.RenderType.draw
     */
    private fun drawImmediate(key: RenderBufferKey, meshData: MeshData) {
        val dynamicTransforms = getDynamicTransformsUniform()
        val draw = meshData.toMeshDraw(key.pipeline)

        renderTarget.createRenderPass(
            { "WorldRenderEnvironment Immediate draw" },
            allowOverride = true,
        ).use { pass ->
            pass.setupRenderTypeScissor()
            pass.bindDefaultUniforms()
            pass.bindDynamicTransformsUniform(dynamicTransforms)
            pass.setUniforms(key.uniforms)

            pass.setPipeline(key.pipeline)
            pass.bindTextures(key.textures)
            pass.bindAndDraw(draw)
        }
    }

    companion object {

        @JvmRecord
        private data class ActiveWorldFrame(
            val renderTarget: RenderTarget,
            val poseStack: PoseStack,
            val camera: Camera,
            val collector: BatchCollector,
        )

        private val globalPoseStack = PoseStack()
        private val globalBatchCollector = BatchCollector()
        private val immediateDrawMapPool = Pool(
            initializer = ::IdentityHashMap,
            finalizer = IdentityHashMap<BufferBuilder, RenderBufferKey>::clear,
        )

        private var activeWorldFrame: ActiveWorldFrame? = null

        /**
         * Starts world-frame scoped rendering context.
         */
        @JvmStatic
        fun beginWorldFrame(renderTarget: RenderTarget, eventPoseStack: PoseStack, camera: Camera) {
            endWorldFrame()

            globalPoseStack.copyFrom(eventPoseStack)

            activeWorldFrame = ActiveWorldFrame(
                renderTarget = renderTarget,
                poseStack = globalPoseStack,
                camera = camera,
                collector = globalBatchCollector,
            )
        }

        /**
         * Flushes and clears world-frame scoped rendering context.
         */
        @JvmStatic
        fun endWorldFrame() {
            val frame = activeWorldFrame ?: return
            frame.collector.flush(frame.renderTarget)
            activeWorldFrame = null
        }

        @PublishedApi
        @JvmStatic
        internal fun create(renderTarget: RenderTarget, poseStack: PoseStack, camera: Camera): WorldRenderEnvironment {
            val frame = activeWorldFrame
            if (frame != null && frame.renderTarget === renderTarget) {
                return WorldRenderEnvironment(
                    renderTarget = renderTarget,
                    poseStack = frame.poseStack,
                    camera = frame.camera,
                    batchCollector = frame.collector,
                    frameBoundCollector = true,
                )
            }

            return WorldRenderEnvironment(
                renderTarget = renderTarget,
                poseStack = poseStack,
                camera = camera,
                batchCollector = BatchCollector(),
                frameBoundCollector = false,
            )
        }
    }
}

private fun PoseStack.copyFrom(source: PoseStack) {
    reset()
    mulPose(source.last().pose())
}

private fun RenderPipeline.requiresImmediateDrawInBatch(): Boolean =
    vertexFormatMode.connectedPrimitives

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

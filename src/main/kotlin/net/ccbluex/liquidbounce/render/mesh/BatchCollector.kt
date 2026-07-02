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

package net.ccbluex.liquidbounce.render.mesh

import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.VertexConsumer
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.ccbluex.liquidbounce.render.ClientTesselator
import net.ccbluex.liquidbounce.render.bindDefaultUniforms
import net.ccbluex.liquidbounce.render.bindDynamicTransformsUniform
import net.ccbluex.liquidbounce.render.bindTextures
import net.ccbluex.liquidbounce.render.createRenderPass
import net.ccbluex.liquidbounce.render.engine.RenderDrawKey
import net.ccbluex.liquidbounce.render.getDynamicTransformsUniform
import net.ccbluex.liquidbounce.render.mesh.MeshDraw.DefaultUploader.bindAndDraw
import net.ccbluex.liquidbounce.render.mesh.MeshDraw.DefaultUploader.toMeshDraw
import net.ccbluex.liquidbounce.render.setUniforms
import net.ccbluex.liquidbounce.render.setupRenderTypeScissor
import java.util.IdentityHashMap
import java.util.function.Function

internal class BatchCollector {

    private data class PendingDraw(
        val key: RenderDrawKey,
        val builder: BufferBuilder,
        val order: Int,
        var submitted: Boolean,
    )

    private data class BuiltDraw(
        val key: RenderDrawKey,
        val meshDraw: MeshDraw,
        val order: Int,
    ) : Comparable<BuiltDraw> {
        override fun compareTo(other: BuiltDraw): Int {
            val r1 = key.compareTo(other.key)
            if (r1 != 0) return r1
            return order.compareTo(other.order)
        }
    }

    private val bufferAllocatorInUse = ObjectArrayList<ByteBufferBuilder>()
    private val consolidatedDraws = Object2ObjectOpenHashMap<RenderDrawKey, PendingDraw>()
    private val pendingSeparateDraws = IdentityHashMap<BufferBuilder, PendingDraw>()
    private val drawOrder = ObjectArrayList<PendingDraw>()
    private val builtBuffers = ObjectArrayList<BuiltDraw>()

    fun start(key: RenderDrawKey): VertexConsumer {
        if (!key.pipeline.canConsolidateConsecutiveGeometry()) {
            val builder = ClientTesselator.begin(key.pipeline, bufferAllocatorInUse)
            val draw = PendingDraw(key, builder, order = drawOrder.size, submitted = false)
            pendingSeparateDraws[builder] = draw
            drawOrder += draw
            return builder
        }

        return consolidatedDraws.computeIfAbsent(key, Function {
            PendingDraw(
                it,
                ClientTesselator.begin(it.pipeline, bufferAllocatorInUse),
                order = drawOrder.size,
                submitted = true,
            ).also(drawOrder::add)
        }).builder
    }

    fun finish(consumer: VertexConsumer, submit: Boolean) {
        val builder = consumer as? BufferBuilder ?: return
        val draw = pendingSeparateDraws.remove(builder) ?: return
        draw.submitted = submit
    }

    @JvmOverloads
    fun flush(renderTarget: RenderTarget, dynamicTransforms: GpuBufferSlice = getDynamicTransformsUniform()) {
        try {
            if (drawOrder.isEmpty) {
                return
            }

            for (draw in drawOrder) {
                if (draw.submitted) {
                    draw.builder.build()?.use { meshData ->
                        builtBuffers += BuiltDraw(
                            draw.key,
                            meshData.toMeshDraw(draw.key.pipeline),
                            draw.order,
                        )
                    }
                }
            }
            clearBuilders()

            if (builtBuffers.isEmpty) {
                return
            }

            builtBuffers.sort()

            renderTarget.createRenderPass(
                { "WorldRenderEnvironment draw" },
                allowOverride = true,
            ).use { pass ->
                pass.setupRenderTypeScissor()
                pass.bindDefaultUniforms()
                pass.bindDynamicTransformsUniform(dynamicTransforms)

                builtBuffers.forEach { draw ->
                    pass.setPipeline(draw.key.pipeline)
                    pass.setUniforms(draw.key.uniforms)
                    pass.bindTextures(draw.key.textures)
                    pass.bindAndDraw(draw.meshDraw)
                }
            }
        } finally {
            builtBuffers.clear()
            clearBuilders()
            ClientTesselator.recycleAll(bufferAllocatorInUse)
            bufferAllocatorInUse.clear()
        }
    }

    private fun clearBuilders() {
        consolidatedDraws.clear()
        pendingSeparateDraws.clear()
        drawOrder.clear()
    }
}

/**
 * @see net.minecraft.client.renderer.rendertype.RenderType.canConsolidateConsecutiveGeometry
 */
private fun RenderPipeline.canConsolidateConsecutiveGeometry(): Boolean =
    !primitiveTopology.connectedPrimitives

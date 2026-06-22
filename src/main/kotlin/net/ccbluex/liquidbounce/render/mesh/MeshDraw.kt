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

import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.systems.RenderPass
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.MeshData
import com.mojang.blaze3d.vertex.VertexFormat
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap
import net.ccbluex.fastutil.enumMapOf
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.render.ClientTesselator
import net.ccbluex.liquidbounce.render.GrowableMappableRingBuffer
import net.ccbluex.liquidbounce.render.bindAndDraw
import net.ccbluex.liquidbounce.utils.kotlin.memorizingFunction
import java.util.function.Function

/**
 * GPU-ready draw descriptor produced from [MeshData].
 *
 * It stores uploaded vertex/index slices plus the draw parameters needed by [RenderPass.bindAndDraw].
 */
@JvmRecord
data class MeshDraw(
    val vertexSlice: GpuBufferSlice,
    val indexSlice: GpuBufferSlice,
    val vertexFormat: VertexFormat,
    val indexType: VertexFormat.IndexType,
    val indexCount: Int,
) {

    companion object {

        /**
         * Shared dynamic VBO pool (keyed by [VertexFormat]).
         *
         * This is the default upload target for per-frame dynamic meshes.
         */
        private val sharedVboGetter =
            memorizingFunction<VertexFormat, GrowableMappableRingBuffer>(Object2ObjectArrayMap()) {
                GrowableMappableRingBuffer(
                    "${LiquidBounce.CLIENT_NAME} Shared VBO for $it",
                    GpuBuffer.USAGE_VERTEX,
                    GrowableMappableRingBuffer.GrowPolicy.of(paddingScale = 8, min = 1 shl 13),
                )
            }

        /**
         * Shared dynamic IBO pool (keyed by [VertexFormat.IndexType]).
         *
         * This is the default upload target for per-frame dynamic meshes.
         */
        private val sharedIboGetter =
            memorizingFunction<VertexFormat.IndexType, GrowableMappableRingBuffer>(enumMapOf()) {
                GrowableMappableRingBuffer(
                    "${LiquidBounce.CLIENT_NAME} Shared IBO for $it",
                    GpuBuffer.USAGE_INDEX,
                    GrowableMappableRingBuffer.GrowPolicy.of(paddingScale = 7, min = 1 shl 11),
                )
            }

        /**
         * Sort Quads (If needed) and upload vertices and indices of [MeshData] to given [GrowableMappableRingBuffer].
         *
         * This might use shared index buffer from [RenderSystem.getSequentialBuffer],
         * if [MeshData.indexBuffer] returns null.
         *
         * This function doesn't close the [MeshData].
         *
         * [vboGetter]/[iboGetter] decide the storage strategy:
         * default shared getters are intended for dynamic per-frame meshes,
         * while custom getters (e.g. from [net.ccbluex.liquidbounce.render.StaticMeshStorage])
         * allow static meshes to keep dedicated buffers.
         *
         * @return The uploaded data. The lifecycle is handled by backend buffer storage.
         */
        @JvmStatic
        @JvmName("create")
        fun MeshData.toMeshDraw(
            pipeline: RenderPipeline,
            vboGetter: Function<VertexFormat, GrowableMappableRingBuffer> = sharedVboGetter,
            iboGetter: Function<VertexFormat.IndexType, GrowableMappableRingBuffer> = sharedIboGetter,
        ): MeshDraw {
            if (pipeline.vertexFormatMode === VertexFormat.Mode.QUADS) {
                this.sortQuads(
                    ClientTesselator.Shared,
                    RenderSystem.getProjectionType().vertexSorting(),
                )
            }

            val vertexSlice = vboGetter.apply(pipeline.vertexFormat).upload(this.vertexBuffer())

            val rawIndices = this.indexBuffer()
            val indexCount = this.drawState().indexCount
            val indexSlice: GpuBufferSlice
            val indexType: VertexFormat.IndexType
            if (rawIndices == null) {
                val shapeIndexBuffer = RenderSystem.getSequentialBuffer(pipeline.vertexFormatMode)
                indexType = shapeIndexBuffer.type()
                indexSlice = shapeIndexBuffer.getBuffer(indexCount)
                    .slice(0L, indexCount.toLong() * indexType.bytes)
            } else {
                indexType = this.drawState().indexType
                indexSlice = iboGetter.apply(indexType).upload(rawIndices)
            }

            return MeshDraw(
                vertexSlice,
                indexSlice,
                pipeline.vertexFormat,
                indexType,
                indexCount,
            )
        }

        /**
         * Bind mesh buffers and issue one indexed draw call.
         */
        @JvmStatic
        fun RenderPass.bindAndDraw(meshDraw: MeshDraw) = bindAndDraw(
            meshDraw.vertexSlice,
            meshDraw.indexSlice,
            meshDraw.vertexFormat,
            meshDraw.indexType,
            meshDraw.indexCount,
        )

    }
}

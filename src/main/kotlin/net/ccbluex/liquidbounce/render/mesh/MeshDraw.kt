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

import com.mojang.blaze3d.IndexType
import com.mojang.blaze3d.PrimitiveTopology
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.systems.RenderPass
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.MeshData
import com.mojang.blaze3d.vertex.VertexFormat
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.render.ClientTesselator
import net.ccbluex.liquidbounce.render.buffers.DynamicGpuBufferWriter
import java.nio.ByteBuffer

/**
 * GPU-ready draw descriptor produced from [MeshData].
 *
 * It stores uploaded vertex data plus an index binding strategy
 * and the draw parameters needed by [RenderPass.bindAndDraw].
 */
@JvmRecord
data class MeshDraw(
    val vertexSlice: GpuBufferSlice,
    val indexBinding: MeshIndexBinding,
    val indexCount: Int,
) {

    /**
     * Describes how a [MeshDraw] resolves its index buffer when it is submitted.
     *
     * Some meshes own a dedicated uploaded index buffer, while others rely on
     * vanilla's shared sequential buffers and must resolve the current buffer
     * lazily at draw time.
     */
    sealed interface MeshIndexBinding {
        /**
         * A dedicated uploaded index buffer owned by the mesh storage strategy.
         */
        @JvmRecord
        data class Uploaded(
            val slice: GpuBufferSlice,
            val type: IndexType,
        ) : MeshIndexBinding

        /**
         * A draw that uses vanilla's shared sequential index buffer for the given topology.
         *
         * The actual [GpuBuffer] and [IndexType] are looked up at draw time so long-lived
         * meshes do not retain a stale slice when the shared sequential buffer grows.
         */
        @JvmRecord
        data class Sequential(
            val primitiveTopology: PrimitiveTopology,
        ) : MeshIndexBinding
    }

    fun interface VertexUploader {
        /**
         * Uploads vertex data.
         *
         * The returned slice's byte offset must be aligned to [VertexFormat.vertexSize] for [format].
         */
        fun upload(format: VertexFormat, data: ByteBuffer): GpuBufferSlice
    }

    fun interface IndexUploader {
        /**
         * Uploads index data.
         *
         * The returned slice's byte offset must be aligned to [IndexType.bytes] for [type].
         */
        fun upload(type: IndexType, data: ByteBuffer): GpuBufferSlice
    }

    companion object DefaultUploader : VertexUploader, IndexUploader {

        /**
         * Shared dynamic VBO writer for per-frame meshes.
         *
         * Vertex uploads are aligned to their respective [VertexFormat.vertexSize].
         */
        private val sharedVbo = DynamicGpuBufferWriter(
            "${LiquidBounce.CLIENT_NAME} Shared VBO",
            GpuBuffer.USAGE_VERTEX,
            DynamicGpuBufferWriter.GrowPolicy.of(paddingScale = 8, min = 1 shl 13),
        )

        /**
         * Shared dynamic IBO writer for per-frame meshes.
         *
         * Index uploads are aligned to their respective [IndexType.bytes].
         */
        private val sharedIbo = DynamicGpuBufferWriter(
            "${LiquidBounce.CLIENT_NAME} Shared IBO",
            GpuBuffer.USAGE_INDEX,
            DynamicGpuBufferWriter.GrowPolicy.of(paddingScale = 7, min = 1 shl 11),
        )

        override fun upload(format: VertexFormat, data: ByteBuffer): GpuBufferSlice {
            return sharedVbo.upload(data, format.vertexSize)
        }

        override fun upload(type: IndexType, data: ByteBuffer): GpuBufferSlice {
            return sharedIbo.upload(data, type.bytes)
        }

        /**
         * End the current frame for all shared writers. After this call, all active buffers
         * are fenced for recycling.
         *
         * Must be called once per frame after all draw calls that reference uploaded slices
         * have been submitted.
         */
        fun endFrame() {
            sharedVbo.endFrame()
            sharedIbo.endFrame()
        }

        /**
         * Release the shared per-frame upload buffers before the render device is shut down.
         */
        fun close() {
            sharedVbo.close()
            sharedIbo.close()
        }

        /**
         * Sort quads (if needed) and upload or describe the buffers needed by [MeshData].
         *
         * If [MeshData.indexBuffer] returns null, the resulting [MeshDraw] resolves
         * vanilla's shared sequential index buffer lazily via [RenderSystem.getSequentialBuffer]
         * when it is drawn.
         *
         * This function doesn't close the [MeshData].
         *
         * [vertexUploader]/[indexUploader] decide the storage strategy:
         * the default companion uploader uses shared dynamic per-frame buffers,
         * while custom uploaders can use dedicated static buffers or staged upload paths.
         *
         * @return The uploaded data. The lifecycle is handled by backend buffer storage.
         */
        @JvmStatic
        @JvmName("create")
        fun MeshData.toMeshDraw(
            pipeline: RenderPipeline,
            vertexUploader: VertexUploader = DefaultUploader,
            indexUploader: IndexUploader = DefaultUploader,
        ): MeshDraw {
            val vertexFormat = requireNotNull(pipeline.getVertexFormatBinding(0)) {
                "Pipeline ${pipeline.location} has no vertex format binding"
            }

            if (pipeline.primitiveTopology === PrimitiveTopology.QUADS) {
                this.sortQuads(
                    ClientTesselator.Shared,
                    RenderSystem.getProjectionType().vertexSorting(),
                )
            }

            val vertexSlice = vertexUploader.upload(vertexFormat, this.vertexBuffer())

            val rawIndices = this.indexBuffer()
            val indexCount = this.drawState().indexCount
            val indexBinding = if (rawIndices == null) {
                MeshIndexBinding.Sequential(pipeline.primitiveTopology)
            } else {
                val indexType = this.drawState().indexType
                val indexSlice = indexUploader.upload(indexType, rawIndices)
                MeshIndexBinding.Uploaded(indexSlice, indexType)
            }

            return MeshDraw(
                vertexSlice,
                indexBinding,
                indexCount,
            )
        }

        /**
         * Bind mesh buffers and issue one indexed draw call.
         */
        @JvmStatic
        fun RenderPass.bindAndDraw(meshDraw: MeshDraw) {
            setVertexBuffer(0, meshDraw.vertexSlice)

            when (val indexBinding = meshDraw.indexBinding) {
                is MeshIndexBinding.Uploaded -> {
                    setIndexBuffer(indexBinding.slice.buffer, indexBinding.type)
                    drawIndexed(
                        meshDraw.indexCount,
                        1,
                        (indexBinding.slice.offset / indexBinding.type.bytes).toInt(),
                        0,
                        0,
                    )
                }

                is MeshIndexBinding.Sequential -> {
                    val sequentialBuffer = RenderSystem.getSequentialBuffer(indexBinding.primitiveTopology)
                    val indexBuffer = sequentialBuffer.getBuffer(meshDraw.indexCount)
                    val indexType = sequentialBuffer.type()

                    setIndexBuffer(indexBuffer, indexType)
                    drawIndexed(
                        meshDraw.indexCount,
                        1,
                        0,
                        0,
                        0,
                    )
                }
            }
        }

    }
}

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

package net.ccbluex.liquidbounce.render

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.minecraft.world.phys.AABB
import org.joml.Matrix4fc

fun VertexConsumer.addBoxOutlines(
    pose: Matrix4fc,
    box: AABB,
    color: Color4b? = null,
    verticesToUse: Int = -1,
) {
    val checkNeeded = verticesToUse and 0xFFFFFF != 0xFFFFFF

    box.forEachOutlineVertex { i, x, y, z ->
        if (checkNeeded && (verticesToUse and (1 shl i)) == 0) {
            return@forEachOutlineVertex
        }

        addVertex(pose, x.toFloat(), y.toFloat(), z.toFloat())
        if (color != null) setColor(color.argb)
    }
}

fun VertexConsumer.addBoxFaces(
    pose: Matrix4fc,
    box: AABB,
    color: Color4b? = null,
    verticesToUse: Int = -1,
) {
    val checkNeeded = verticesToUse and 0xFFFFFF != 0xFFFFFF

    box.forEachFaceVertex { i, x, y, z ->
        if (checkNeeded && (verticesToUse and (1 shl i)) == 0) {
            return@forEachFaceVertex
        }

        addVertex(pose, x.toFloat(), y.toFloat(), z.toFloat())
        if (color != null) setColor(color.argb)
    }
}

inline fun RenderPassRenderState.buildMesh(
    pipeline: RenderPipeline,
    vboStorage: GrowableMappableRingBuffer,
    iboStorage: GrowableMappableRingBuffer,
    sortQuads: Boolean = false,
    block: VertexConsumer.(pose: PoseStack) -> Unit,
) {
    clearStates()

    val byteBufferBuilder = ClientTesselator.allocator(pipeline)
    val bufferBuilder = BufferBuilder(
        byteBufferBuilder,
        pipeline.vertexFormatMode,
        pipeline.vertexFormat
    )
    usePoseStack {
        bufferBuilder.block(this)
    }

    bufferBuilder.build()?.use { meshData ->
        if (sortQuads) {
            meshData.sortQuads(byteBufferBuilder, RenderSystem.getProjectionType().vertexSorting())
        }
        this.uploadAndSetIndices(meshData, iboStorage, pipeline.vertexFormatMode)
        this.uploadAndSetVertices(meshData, vboStorage)
    }

    byteBufferBuilder.clear()
    this.ready = true
}

inline fun RenderPassRenderState.WithBuffers.buildMesh(
    pipeline: RenderPipeline,
    sortQuads: Boolean = false,
    block: VertexConsumer.(pose: PoseStack) -> Unit,
) = buildMesh(
    pipeline,
    vboStorage,
    iboStorage,
    sortQuads,
    block
)

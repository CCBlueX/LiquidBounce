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

@file:Suppress("NOTHING_TO_INLINE", "detekt:TooManyFunctions")

package net.ccbluex.liquidbounce.render

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.type.Vec3f
import net.ccbluex.liquidbounce.utils.math.forAllFaces
import net.ccbluex.liquidbounce.utils.math.forAllSideFaces
import net.ccbluex.liquidbounce.utils.math.forAllSideOutlineEdges
import net.ccbluex.liquidbounce.utils.render.begin
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.VoxelShape
import org.joml.Matrix4fc
import org.joml.Vector3fc

inline fun VertexConsumer.addVertex(pose: Matrix4fc, x: Double, y: Double, z: Double): VertexConsumer =
    addVertex(pose, x.toFloat(), y.toFloat(), z.toFloat())

inline fun VertexConsumer.addVertex(pose: PoseStack.Pose, x: Double, y: Double, z: Double): VertexConsumer =
    addVertex(pose.pose(), x, y, z)

inline fun VertexConsumer.addVertex(pose: Matrix4fc, pos: Vec3): VertexConsumer =
    addVertex(pose, pos.x, pos.y, pos.z)

inline fun VertexConsumer.addVertex(pose: PoseStack.Pose, pos: Vec3): VertexConsumer =
    addVertex(pose, pos.x, pos.y, pos.z)

inline fun VertexConsumer.addVertex(pose: Matrix4fc, pos: Vec3f): VertexConsumer =
    addVertex(pose, pos.x, pos.y, pos.z)

inline fun VertexConsumer.addVertex(pose: PoseStack.Pose, pos: Vec3f): VertexConsumer =
    addVertex(pose, pos.x, pos.y, pos.z)

inline fun VertexConsumer.addVertex(pose: Matrix4fc, pos: Vector3fc): VertexConsumer =
    addVertex(pose, pos.x(), pos.y(), pos.z())

inline fun VertexConsumer.addVertex(pose: PoseStack.Pose, pos: Vector3fc): VertexConsumer =
    addVertex(pose, pos.x(), pos.y(), pos.z())

inline fun VertexConsumer.setNormal(pose: PoseStack.Pose, normalVector: Vec3f): VertexConsumer =
    setNormal(pose, normalVector.x, normalVector.y, normalVector.z)

inline fun VertexConsumer.setColor(color: Color4b): VertexConsumer = setColor(color.argb)

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

        addVertex(pose, x, y, z)
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

        addVertex(pose, x, y, z)
        if (color != null) setColor(color.argb)
    }
}

fun VertexConsumer.addShapeFaces(
    pose: Matrix4fc,
    shape: VoxelShape,
    color: Color4b? = null,
) {
    shape.forAllFaces { direction, minX, minY, minZ, maxX, maxY, maxZ ->
        addFaceVertices(pose, direction, minX, minY, minZ, maxX, maxY, maxZ, color)
    }
}

fun VertexConsumer.addShapeOutlines(
    pose: Matrix4fc,
    shape: VoxelShape,
    color: Color4b? = null,
) {
    shape.forAllEdges { startX, startY, startZ, endX, endY, endZ ->
        addVertex(pose, startX, startY, startZ)
        if (color != null) setColor(color.argb)

        addVertex(pose, endX, endY, endZ)
        if (color != null) setColor(color.argb)
    }
}

fun VertexConsumer.addShapeSideFaces(
    pose: Matrix4fc,
    shape: VoxelShape,
    side: Direction,
    hitPos: Vec3,
    color: Color4b? = null,
) {
    shape.forAllSideFaces(side, hitPos) { direction, minX, minY, minZ, maxX, maxY, maxZ ->
        addFaceVertices(pose, direction, minX, minY, minZ, maxX, maxY, maxZ, color)
    }
}

fun VertexConsumer.addShapeSideOutlines(
    pose: Matrix4fc,
    shape: VoxelShape,
    side: Direction,
    hitPos: Vec3,
    color: Color4b? = null,
) {
    shape.forAllSideOutlineEdges(side, hitPos) { startX, startY, startZ, endX, endY, endZ ->
        addVertex(pose, startX, startY, startZ)
        if (color != null) setColor(color.argb)

        addVertex(pose, endX, endY, endZ)
        if (color != null) setColor(color.argb)
    }
}

private fun VertexConsumer.addFaceVertices(
    pose: Matrix4fc,
    direction: Direction,
    minX: Double,
    minY: Double,
    minZ: Double,
    maxX: Double,
    maxY: Double,
    maxZ: Double,
    color: Color4b?,
) {
    when (direction) {
        Direction.DOWN -> {
            addColoredVertex(pose, minX, minY, minZ, color)
            addColoredVertex(pose, maxX, minY, minZ, color)
            addColoredVertex(pose, maxX, minY, maxZ, color)
            addColoredVertex(pose, minX, minY, maxZ, color)
        }

        Direction.UP -> {
            addColoredVertex(pose, minX, maxY, minZ, color)
            addColoredVertex(pose, minX, maxY, maxZ, color)
            addColoredVertex(pose, maxX, maxY, maxZ, color)
            addColoredVertex(pose, maxX, maxY, minZ, color)
        }

        Direction.NORTH -> {
            addColoredVertex(pose, minX, minY, minZ, color)
            addColoredVertex(pose, minX, maxY, minZ, color)
            addColoredVertex(pose, maxX, maxY, minZ, color)
            addColoredVertex(pose, maxX, minY, minZ, color)
        }

        Direction.EAST -> {
            addColoredVertex(pose, maxX, minY, minZ, color)
            addColoredVertex(pose, maxX, maxY, minZ, color)
            addColoredVertex(pose, maxX, maxY, maxZ, color)
            addColoredVertex(pose, maxX, minY, maxZ, color)
        }

        Direction.SOUTH -> {
            addColoredVertex(pose, minX, minY, maxZ, color)
            addColoredVertex(pose, maxX, minY, maxZ, color)
            addColoredVertex(pose, maxX, maxY, maxZ, color)
            addColoredVertex(pose, minX, maxY, maxZ, color)
        }

        Direction.WEST -> {
            addColoredVertex(pose, minX, minY, minZ, color)
            addColoredVertex(pose, minX, minY, maxZ, color)
            addColoredVertex(pose, minX, maxY, maxZ, color)
            addColoredVertex(pose, minX, maxY, minZ, color)
        }
    }
}

private fun VertexConsumer.addColoredVertex(
    pose: Matrix4fc,
    x: Double,
    y: Double,
    z: Double,
    color: Color4b?,
) {
    addVertex(pose, x, y, z)
    if (color != null) {
        setColor(color.argb)
    }
}

/**
 * Build new mesh data and upload it.
 * This method is designed for lazy building so [rotate] defaults to true.
 *
 * @param origin a preferred origin; the lambda receives the resolved origin that must be used
 * for relative vertex positions.
 */
inline fun StaticMeshStorage.buildMesh(
    pipeline: RenderPipeline,
    rotate: Boolean = true,
    origin: BlockPos = BlockPos.ZERO,
    block: VertexConsumer.(pose: PoseStack, origin: BlockPos) -> Unit,
) {
    clearStates()
    val resolvedOrigin = this.resolveBaseBlockPos(origin)

    val bufferBuilder = this.byteBufferBuilder.begin(pipeline)
    usePoseStack {
        bufferBuilder.block(this, resolvedOrigin)
    }

    bufferBuilder.build()?.use { meshData ->
        this.uploadAndSet(meshData, pipeline, rotate)
    }

    this.byteBufferBuilder.clear()
}

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
import com.mojang.blaze3d.vertex.PoseStack
import net.ccbluex.liquidbounce.render.engine.RenderDrawKey
import net.ccbluex.liquidbounce.render.engine.type.Vec3f
import net.ccbluex.liquidbounce.render.mesh.BatchCollector
import net.ccbluex.liquidbounce.render.mesh.MeshBuildScope
import net.ccbluex.liquidbounce.utils.collection.Pools
import net.minecraft.client.Camera
import net.minecraft.client.renderer.texture.AbstractTexture
import net.minecraft.core.BlockPos
import net.minecraft.core.Position
import net.minecraft.core.Vec3i
import net.minecraft.world.phys.Vec3
import org.joml.Vector3fc

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

inline fun PoseStack.translate(x: Int, y: Int, z: Int) =
    translate(x.toFloat(), y.toFloat(), z.toFloat())

inline fun PoseStack.translate(vec3i: Vec3i) =
    translate(vec3i.x, vec3i.y, vec3i.z)

/**
 * @see net.ccbluex.liquidbounce.features.module.modules.render.ModuleBlockESP
 * @see net.ccbluex.liquidbounce.features.module.modules.render.ModuleStorageESP
 */
inline fun PoseStack.translate(blockPos: Long, origin: BlockPos) {
    translate(
        BlockPos.getX(blockPos) - origin.x,
        BlockPos.getY(blockPos) - origin.y,
        BlockPos.getZ(blockPos) - origin.z,
    )
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
) {
    /**
     * Converts a world-space position to the camera-relative coordinate system.
     */
    fun relativeToCamera(pos: Vec3f): Vec3 = Vec3(
        pos.x - camera.position().x,
        pos.y - camera.position().y,
        pos.z - camera.position().z,
    )

    /**
     * Converts a world-space position to the camera-relative coordinate system.
     */
    fun relativeToCamera(pos: Position): Vec3 = Vec3(
        pos.x() - camera.position().x,
        pos.y() - camera.position().y,
        pos.z() - camera.position().z,
    )

    /**
     * Converts a world-space position to the camera-relative coordinate system.
     */
    fun relativeToCamera(pos: Vec3i): Vec3 = Vec3(
        pos.x.toDouble() - camera.position().x,
        pos.y.toDouble() - camera.position().y,
        pos.z.toDouble() - camera.position().z,
    )

    /**
     * Converts a world-space position to the camera-relative coordinate system.
     */
    fun relativeToCamera(pos: Vector3fc): Vec3 = Vec3(
        pos.x() - camera.position().x,
        pos.y() - camera.position().y,
        pos.z() - camera.position().z,
    )

    /**
     * Low-level draw entrypoint.
     *
     * The returned scope must be closed after writing vertices.
     *
     * Prefer [net.ccbluex.liquidbounce.render.drawCustomMesh] for regular use.
     */
    fun start(
        pipeline: RenderPipeline,
        textures: Map<String, AbstractTexture> = emptyMap(),
        uniforms: Map<String, GpuBufferSlice> = emptyMap(),
    ): MeshBuildScope {
        val key = RenderDrawKey.of(
            pipeline,
            textures,
            uniforms,
        )
        return batchCollector.start(key)
    }
}

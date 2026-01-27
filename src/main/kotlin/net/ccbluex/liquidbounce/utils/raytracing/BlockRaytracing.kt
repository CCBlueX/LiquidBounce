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
package net.ccbluex.liquidbounce.utils.raytracing

import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext

fun rayTraceCollidingBlocks(start: Vec3, end: Vec3): BlockHitResult? {
    val result = mc.level!!.clip(
        ClipContext(
            start,
            end,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.ANY,
            mc.player!!
        )
    )

    return result.takeIf { it.type == HitResult.Type.BLOCK }
}

fun raytraceBlock(
    range: Double,
    rotation: Rotation = RotationManager.currentRotation ?: player.rotation,
    pos: BlockPos,
    state: BlockState,
): BlockHitResult? {
    val entity: Entity = mc.cameraEntity ?: return null

    val start = entity.eyePosition
    val rotationVec = rotation.directionVector

    val end = start.add(rotationVec.x * range, rotationVec.y * range, rotationVec.z * range)

    return mc.level?.clipWithInteractionOverride(
        start,
        end,
        pos,
        state.getShape(mc.level!!, pos, CollisionContext.of(mc.player!!)),
        state,
    )
}

/**
 * Allows you to check if a point is behind a wall
 */
fun facingBlock(
    eyes: Vec3,
    vec3: Vec3,
    blockPos: BlockPos,
    expectedSide: Direction? = null,
    expectedMaxRange: Double? = null,
): Boolean {
    val searchedPos =
        mc.level?.clip(
            ClipContext(
                eyes, vec3, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player!!,
            ),
        ) ?: return false

    if (searchedPos.type != HitResult.Type.BLOCK || (expectedSide != null && searchedPos.direction != expectedSide)) {
        return false
    }
    if (expectedMaxRange != null && searchedPos.location.distanceToSqr(eyes) > expectedMaxRange * expectedMaxRange) {
        return false
    }

    return searchedPos.blockPos == blockPos
}

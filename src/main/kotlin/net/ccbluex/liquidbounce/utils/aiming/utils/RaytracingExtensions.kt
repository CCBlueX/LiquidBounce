/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.utils.aiming.utils

import net.ccbluex.liquidbounce.utils.aiming.data.Orientation
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.minecraft.block.BlockState
import net.minecraft.block.ShapeContext
import net.minecraft.entity.Entity
import net.minecraft.entity.projectile.ProjectileUtil
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext
import kotlin.math.max

fun rayTraceCollidingBlocks(start: Vec3d, end: Vec3d): BlockHitResult? {
    val result = mc.world!!.raycast(
        RaycastContext(
            start,
            end,
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.ANY,
            mc.player!!
        )
    )

    if (result == null || result.type != HitResult.Type.BLOCK) {
        return null
    }

    return result
}

fun raytraceEntity(
    range: Double,
    rotation: Orientation,
    filter: (Entity) -> Boolean,
): EntityHitResult? {
    val entity = mc.cameraEntity ?: return null

    val cameraVec = entity.eyes
    val rotationVec = rotation.polar3d

    val vec3d3 = cameraVec.add(rotationVec.x * range, rotationVec.y * range, rotationVec.z * range)
    val box = entity.boundingBox.stretch(rotationVec.multiply(range)).expand(1.0, 1.0, 1.0)

    val hitResult =
        ProjectileUtil.raycast(
            entity,
            cameraVec,
            vec3d3,
            box,
            { !it.isSpectator && it.canHit() && filter(it) },
            range * range,
        )

    return hitResult
}

fun raytraceBlock(
    range: Double,
    rotation: Orientation,
    pos: BlockPos,
    state: BlockState,
): BlockHitResult? {
    val entity: Entity = mc.cameraEntity ?: return null

    val start = entity.eyes
    val rotationVec = rotation.polar3d

    val end = start.add(rotationVec.x * range, rotationVec.y * range, rotationVec.z * range)

    return mc.world?.raycastBlock(
        start,
        end,
        pos,
        state.getOutlineShape(mc.world, pos, ShapeContext.of(mc.player)),
        state,
    )
}

fun raycast(
    rotation: Orientation,
    range: Double = max(player.blockInteractionRange, player.entityInteractionRange),
    includeFluids: Boolean = false,
    tickDelta: Float = 1.0f,
): BlockHitResult? {
    val entity = mc.cameraEntity ?: return null

    val start = entity.getCameraPosVec(tickDelta)
    val rotationVec = rotation.polar3d

    val end = start.add(rotationVec.x * range, rotationVec.y * range, rotationVec.z * range)

    return mc.world?.raycast(
        RaycastContext(
            start,
            end,
            RaycastContext.ShapeType.OUTLINE,
            if (includeFluids) RaycastContext.FluidHandling.ANY else RaycastContext.FluidHandling.NONE,
            entity,
        ),
    )
}

/**
 * Allows you to check if a point is behind a wall
 */
fun canSeePointFrom(
    eyes: Vec3d,
    vec3: Vec3d,
) = mc.world?.raycast(
    RaycastContext(
        eyes, vec3, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player,
    ),
)?.type == HitResult.Type.MISS

/**
 * Allows you to check if your enemy is behind a wall
 */
fun facingEnemy(
    toEntity: Entity,
    range: Double,
    rotation: Orientation,
): Boolean {
    return raytraceEntity(range, rotation) { it == toEntity } != null
}

fun facingEnemy(
    fromEntity: Entity = mc.cameraEntity!!,
    toEntity: Entity,
    rotation: Orientation,
    range: Double,
    wallsRange: Double,
): Boolean {
    val cameraVec = fromEntity.eyes
    val rotationVec = rotation.polar3d

    val rangeSquared = range * range
    val wallsRangeSquared = wallsRange * wallsRange

    val vec3d3 = cameraVec.add(rotationVec.x * range, rotationVec.y * range, rotationVec.z * range)
    val box = fromEntity.boundingBox.stretch(rotationVec.multiply(range)).expand(1.0, 1.0, 1.0)

    val entityHitResult =
        ProjectileUtil.raycast(
            fromEntity, cameraVec, vec3d3, box, { !it.isSpectator && it.canHit() && it == toEntity }, rangeSquared,
        ) ?: return false

    val distance = cameraVec.squaredDistanceTo(entityHitResult.pos)

    return distance <= rangeSquared && canSeePointFrom(cameraVec, entityHitResult.pos) || distance <= wallsRangeSquared
}

/**
 * Allows you to check if a point is behind a wall
 */
fun facingBlock(
    eyes: Vec3d,
    vec3: Vec3d,
    blockPos: BlockPos,
    expectedSide: Direction? = null,
    expectedMaxRange: Double? = null,
): Boolean {
    val searchedPos =
        mc.world?.raycast(
            RaycastContext(
                eyes, vec3, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player,
            ),
        ) ?: return false

    if (searchedPos.type != HitResult.Type.BLOCK || (expectedSide != null && searchedPos.side != expectedSide)) {
        return false
    }
    if (expectedMaxRange != null && searchedPos.pos.squaredDistanceTo(eyes) > expectedMaxRange * expectedMaxRange) {
        return false
    }

    return searchedPos.blockPos == blockPos
}

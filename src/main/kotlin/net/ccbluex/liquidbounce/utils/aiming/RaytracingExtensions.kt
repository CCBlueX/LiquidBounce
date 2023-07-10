/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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
package net.ccbluex.liquidbounce.utils.aiming

import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.minecraft.block.BlockState
import net.minecraft.block.ShapeContext
import net.minecraft.entity.Entity
import net.minecraft.entity.projectile.ProjectileUtil
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext

fun raytraceEntity(range: Double, rotation: Rotation, filter: (Entity) -> Boolean): Entity? {
    val entity = mc.cameraEntity ?: return null

    val cameraVec = entity.eyes
    val rotationVec = rotation.rotationVec

    val vec3d3 = cameraVec.add(rotationVec.x * range, rotationVec.y * range, rotationVec.z * range)
    val box = entity.boundingBox.stretch(rotationVec.multiply(range)).expand(1.0, 1.0, 1.0)

    val entityHitResult = ProjectileUtil.raycast(
        entity, cameraVec, vec3d3, box, { !it.isSpectator && it.canHit() && filter(it) }, range * range
    )

    return entityHitResult?.entity
}

fun raytraceBlock(range: Double, rotation: Rotation, pos: BlockPos, state: BlockState): BlockHitResult? {
    val entity: Entity = mc.cameraEntity ?: return null

    val start = entity.eyes
    val rotationVec = rotation.rotationVec

    val end = start.add(rotationVec.x * range, rotationVec.y * range, rotationVec.z * range)

    return mc.world?.raycastBlock(
        start, end, pos, state.getOutlineShape(mc.world, pos, ShapeContext.of(mc.player)), state
    )
}

fun raycast(range: Double, rotation: Rotation): BlockHitResult? {
    val entity = mc.cameraEntity ?: return null

    val start = entity.eyes
    val rotationVec = rotation.rotationVec

    val end = start.add(rotationVec.x * range, rotationVec.y * range, rotationVec.z * range)

    return mc.world?.raycast(
        RaycastContext(
            start, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, entity
        )
    )
}

/**
 * Allows you to check if a point is behind a wall
 */
fun isVisible(eyes: Vec3d, vec3: Vec3d) = mc.world?.raycast(
    RaycastContext(
        eyes, vec3, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player
    )
)?.type == HitResult.Type.MISS

/**
 * Allows you to check if your enemy is behind a wall
 */
fun facingEnemy(enemy: Entity, range: Double, rotation: Rotation): Boolean {
    return raytraceEntity(range, rotation) { it == enemy } != null
}

fun facingEnemy(enemy: Entity, rotation: Rotation, range: Double, wallsRange: Double): Boolean {
    val entity = mc.cameraEntity ?: return false

    val cameraVec = entity.eyes
    val rotationVec = rotation.rotationVec

    val rangeSquared = range * range
    val wallsRangeSquared = wallsRange * wallsRange

    val vec3d3 = cameraVec.add(rotationVec.x * range, rotationVec.y * range, rotationVec.z * range)
    val box = entity.boundingBox.stretch(rotationVec.multiply(range)).expand(1.0, 1.0, 1.0)

    val entityHitResult = ProjectileUtil.raycast(
        entity, cameraVec, vec3d3, box, { !it.isSpectator && it.canHit() && it == enemy }, rangeSquared
    ) ?: return false

    val distance = cameraVec.squaredDistanceTo(entityHitResult.pos)

    return distance <= rangeSquared && isVisible(cameraVec, entityHitResult.pos) || distance <= wallsRangeSquared
}

/**
 * Allows you to check if a point is behind a wall
 */
fun facingBlock(eyes: Vec3d, vec3: Vec3d, blockPos: BlockPos, expectedSide: Direction? = null): Boolean {
    val searchedPos = mc.world?.raycast(
        RaycastContext(
            eyes, vec3, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player
        )
    ) ?: return false

    if (searchedPos.type != HitResult.Type.BLOCK || (expectedSide != null && searchedPos.side != expectedSide)) {
        return false
    }

    return searchedPos.blockPos == blockPos
}

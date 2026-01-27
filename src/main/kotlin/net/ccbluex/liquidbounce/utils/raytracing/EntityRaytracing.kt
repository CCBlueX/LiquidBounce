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

import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.math.sq
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntitySelector
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.phys.EntityHitResult
import java.util.function.Predicate

fun Entity.findEntityInCrosshair(
    range: Double,
    rotation: Rotation,
    predicate: Predicate<Entity>? = null,
): EntityHitResult? {
    val cameraVec = eyePosition
    val rotationVec = rotation.directionVector

    val vec3d3 = cameraVec.add(rotationVec.x * range, rotationVec.y * range, rotationVec.z * range)
    val box = boundingBox.expandTowards(rotationVec.scale(range)).inflate(1.0, 1.0, 1.0)

    val hitResult = ProjectileUtil.getEntityHitResult(
        this,
        cameraVec,
        vec3d3,
        box,
        if (predicate != null) EntitySelector.CAN_BE_PICKED.or(predicate) else EntitySelector.CAN_BE_PICKED,
        range.sq()
    )

    return hitResult
}

fun findEntityInCrosshair(
    range: Double,
    rotation: Rotation,
    predicate: Predicate<Entity>? = null,
): EntityHitResult? = mc.cameraEntity?.findEntityInCrosshair(range, rotation, predicate)

/**
 * Allows you to check if your enemy is behind a wall
 */
fun isLookingAtEntity(
    toEntity: Entity,
    range: Double,
    rotation: Rotation,
) = findEntityInCrosshair(range, rotation) { entity ->
    !entity.isSpectator && entity.isPickable && entity == toEntity
}

fun isLookingAtEntity(
    fromEntity: Entity = mc.cameraEntity!!,
    toEntity: Entity,
    rotation: Rotation,
    range: Double,
    throughWallsRange: Double,
): EntityHitResult? {
    val cameraVec = fromEntity.eyePosition
    val entityHitResult = fromEntity.findEntityInCrosshair(range, rotation) { entity ->
        entity == toEntity
    } ?: return null

    val distance = cameraVec.distanceToSqr(entityHitResult.location)

    // Either within through-walls range, or within normal range and has line of sight
    return entityHitResult.takeIf {
        distance <= throughWallsRange.sq()
            || distance <= range.sq() && hasLineOfSight(cameraVec, entityHitResult.location)
    }
}

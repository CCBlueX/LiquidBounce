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
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.math.sq
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntitySelector
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.phys.EntityHitResult
import java.util.function.Predicate

fun Entity.findEntityInCrosshair(
    range: Double,
    throughWallsRange: Double = range,
    rotation: Rotation,
    predicate: Predicate<Entity>? = null,
): EntityHitResult? {
    val cameraVec = eyePosition
    val rotationVec = rotation.directionVector

    val vec3d3 = cameraVec.add(rotationVec.x * range, rotationVec.y * range, rotationVec.z * range)
    val box = boundingBox.expandTowards(rotationVec.scale(range)).inflate(1.0, 1.0, 1.0)

    val entityHitResult = ProjectileUtil.getEntityHitResult(
        this,
        cameraVec,
        vec3d3,
        box,
        if (predicate != null) EntitySelector.CAN_BE_PICKED.or(predicate) else EntitySelector.CAN_BE_PICKED,
        range.sq()
    )

    return entityHitResult?.takeIf { hitResult ->
        val distanceSqr = cameraVec.distanceToSqr(hitResult.location)

        distanceSqr <= throughWallsRange.sq() ||
            distanceSqr <= range.sq() && hasLineOfSight(cameraVec, hitResult.location)
    }
}

fun findEntityInCrosshair(
    range: Double,
    throughWallsRange: Double = range,
    rotation: Rotation,
    predicate: Predicate<Entity>? = null,
): EntityHitResult? = mc.cameraEntity?.findEntityInCrosshair(range, throughWallsRange, rotation, predicate)

fun entitySelector(selected: Entity) = Predicate<Entity> { entity ->
    entity == selected
}

fun attackableSelector() = Predicate<Entity> { entity ->
    entity.shouldBeAttacked()
}

fun isLookingAtEntity(
    toEntity: Entity,
    range: Double,
    rotation: Rotation,
) = findEntityInCrosshair(range, range, rotation, entitySelector(toEntity))

fun isLookingAtEntity(
    fromEntity: Entity = mc.cameraEntity!!,
    toEntity: Entity,
    rotation: Rotation,
    range: Double,
    throughWallsRange: Double,
) = fromEntity.findEntityInCrosshair(range, throughWallsRange, rotation, entitySelector(toEntity))

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
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.max

fun traceFromPlayer(
    rotation: Rotation = RotationManager.currentRotation ?: player.rotation,
    range: Double = max(player.blockInteractionRange(), player.entityInteractionRange()),
    block: ClipContext.Block = ClipContext.Block.OUTLINE,
    includeFluids: Boolean = false,
    tickDelta: Float = 1f,
): BlockHitResult {
    return traceFromPoint(
        range = range,
        block = block,
        includeFluids = includeFluids,
        start = player.getEyePosition(tickDelta),
        direction = rotation.directionVector
    )
}

fun traceFromPoint(
    range: Double = max(player.blockInteractionRange(), player.entityInteractionRange()),
    block: ClipContext.Block = ClipContext.Block.OUTLINE,
    includeFluids: Boolean = false,
    start: Vec3,
    direction: Vec3,
    entity: Entity = mc.cameraEntity!!,
): BlockHitResult {
    val end = start.add(direction.x * range, direction.y * range, direction.z * range)

    return world.clip(
        ClipContext(
            start,
            end,
            block,
            if (includeFluids) ClipContext.Fluid.ANY else ClipContext.Fluid.NONE,
            entity,
        ),
    )
}

/**
 * Allows you to check if a point is behind a wall
 *
 * @see net.minecraft.world.entity.LivingEntity.hasLineOfSight
 */
fun hasLineOfSight(
    eyes: Vec3,
    vec3: Vec3,
): Boolean {
    return world.clip(
        ClipContext(
            eyes,
            vec3,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            player,
        ),
    ).type == HitResult.Type.MISS
}

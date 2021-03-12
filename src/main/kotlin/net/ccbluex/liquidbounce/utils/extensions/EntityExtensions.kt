/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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
package net.ccbluex.liquidbounce.utils.extensions

import net.ccbluex.liquidbounce.utils.mc
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.stat.Stats
import net.minecraft.util.math.Vec3d
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

val ClientPlayerEntity.moving
    get() = input.movementForward != 0.0f || input.movementSideways != 0.0f

val Entity.exactPosition
    get() = Triple(x, y, z)

val PlayerEntity.ping: Int
    get() = mc.networkHandler?.getPlayerListEntry(uuid)?.latency ?: 0

val ClientPlayerEntity.directionYaw: Float
    get() {
        var rotationYaw = yaw

        // Check if client-user tries to walk backwards (+180 to turn around)
        if (input.movementForward < 0f)
            rotationYaw += 180f

        // Check which direction the client-user tries to walk sideways
        var forward = 1f
        if (input.movementForward < 0f)
            forward = -0.5f
        else if (input.movementForward > 0f)
            forward = 0.5f

        if (input.movementSideways > 0f)
            rotationYaw -= 90f * forward
        if (input.movementSideways < 0f)
            rotationYaw += 90f * forward

        return rotationYaw
    }

val PlayerEntity.sqrtSpeed: Double
    get() = velocity.sqrtSpeed

fun ClientPlayerEntity.upwards(height: Float) {
    // Might be a jump
    if (isOnGround) {
        // Allows to bypass modern anti cheat techniques
        incrementStat(Stats.JUMP)
    }

    velocity.y = height.toDouble()
    velocityDirty = true
}

fun ClientPlayerEntity.downwards(motion: Float) {
    velocity.y = motion.toDouble()
    velocityDirty = true
}

fun ClientPlayerEntity.strafe(yaw: Float = directionYaw, speed: Double = sqrtSpeed) {
    val angle = Math.toRadians(yaw.toDouble())
    velocity.x = -sin(angle) * speed
    velocity.z = cos(angle) * speed
}

val Vec3d.sqrtSpeed: Double
    get() = sqrt(x * x + z * z)

fun Vec3d.strafe(yaw: Float, speed: Double = sqrtSpeed) {
    val angle = Math.toRadians(yaw.toDouble())
    x = -sin(angle) * speed
    z = cos(angle) * speed
}

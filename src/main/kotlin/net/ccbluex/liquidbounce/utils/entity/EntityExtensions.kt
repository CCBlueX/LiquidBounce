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
package net.ccbluex.liquidbounce.utils.entity

import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.stat.Stats
import net.minecraft.util.math.Box
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
        val options = mc.options

        // Check if client-user tries to walk backwards (+180 to turn around)
        if (options.keyBack.isPressed) {
            rotationYaw += 180f
        }

        // Check which direction the client-user tries to walk sideways
        var forward = 1f
        if (options.keyBack.isPressed) {
            forward = -0.5f
        } else if (options.keyForward.isPressed) {
            forward = 0.5f
        }

        if (options.keyLeft.isPressed) {
            rotationYaw -= 90f * forward
        }
        if (options.keyRight.isPressed) {
            rotationYaw += 90f * forward
        }

        return rotationYaw
    }

val PlayerEntity.sqrtSpeed: Double
    get() = velocity.sqrtSpeed

fun ClientPlayerEntity.upwards(height: Float, increment: Boolean = true) {
    // Might be a jump
    if (isOnGround && increment) {
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
    if (!moving) {
        velocity.x = 0.0
        velocity.z = 0.0
        return
    }

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

val ClientPlayerEntity.eyesPos: Vec3d
    get() = Vec3d(pos.x, boundingBox.minY + getEyeHeight(pose), pos.z)

/**
 * Allows to calculate the distance between the current entity and [entity] from the nearest corner of the bounding box
 */
fun Entity.boxedDistanceTo(entity: Entity): Double {
    return sqrt(squaredBoxedDistanceTo(entity))
}

fun Entity.squaredBoxedDistanceTo(entity: Entity): Double {
    val eyes = entity.getCameraPosVec(1F)
    val pos = getNearestPoint(eyes, boundingBox)

    val xDist = pos.x - eyes.x
    val yDist = pos.y - eyes.y
    val zDist = pos.z - eyes.z

    return xDist * xDist + yDist * yDist + zDist * zDist
}

fun Entity.interpolateCurrentPosition(tickDelta: Float): Vec3 {
    return Vec3(
        this.lastRenderX + (this.x - this.lastRenderX) * tickDelta,
        this.lastRenderY + (this.y - this.lastRenderY) * tickDelta,
        this.lastRenderZ + (this.z - this.lastRenderZ) * tickDelta,
    )
}

/**
 * Get the nearest point of a box. Very useful to calculate the distance of an enemy.
 */
fun getNearestPoint(eyes: Vec3d, box: Box): Vec3d {
    val origin = doubleArrayOf(eyes.x, eyes.y, eyes.z)
    val destMins = doubleArrayOf(box.minX, box.minY, box.minZ)
    val destMaxs = doubleArrayOf(box.maxX, box.maxY, box.maxZ)

    // It loops through every coordinate of the double arrays and picks the nearest point
    for (i in 0..2) {
        if (origin[i] > destMaxs[i]) {
            origin[i] = destMaxs[i]
        } else if (origin[i] < destMins[i]) {
            origin[i] = destMins[i]
        }
    }

    return Vec3d(origin[0], origin[1], origin[2])
}

fun PlayerEntity.wouldBlockHit(source: PlayerEntity): Boolean {
    if (!this.isBlocking) {
        return false
    }

    val vec3d = source.pos

    val facingVec = getRotationVec(1.0f)
    var deltaPos = vec3d.subtract(pos).normalize()

    deltaPos = Vec3d(deltaPos.x, 0.0, deltaPos.z)

    return deltaPos.dotProduct(facingVec) < 0.0
}

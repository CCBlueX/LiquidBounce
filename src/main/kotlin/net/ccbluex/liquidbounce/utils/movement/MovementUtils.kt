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
package net.ccbluex.liquidbounce.utils.movement

import net.ccbluex.fastutil.mapToArray
import net.ccbluex.fastutil.objectHashSetOf
import net.ccbluex.liquidbounce.features.addon.AddonApi
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.features.MovementCorrection
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.math.yaw
import net.ccbluex.liquidbounce.utils.math.copy
import net.ccbluex.liquidbounce.utils.math.fma
import net.ccbluex.liquidbounce.utils.math.iterator
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.rangeTo
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.entity.Pose
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.roundToInt


/**
 * Returns the yaw difference the position is from the player position
 *
 * @param positionRelativeToPlayer relative position to player
 */
fun getDegreesRelativeToView(
    positionRelativeToPlayer: Vec3,
    yaw: Float = RotationManager.currentRotation?.yaw ?: player.yRot,
): Float {
    val optimalYaw = positionRelativeToPlayer.yaw
    val currentYaw = Mth.wrapDegrees(yaw)

    return Mth.wrapDegrees(optimalYaw - currentYaw)
}

fun getDirectionalInputForDegrees(
    directionalInput: DirectionalInput,
    dgs: Float,
    deadAngle: Float = 20.0F,
): DirectionalInput {
    var forwards = directionalInput.forwards
    var backwards = directionalInput.backwards
    var left = directionalInput.left
    var right = directionalInput.right

    if (dgs > -90.0F + deadAngle && dgs < 90.0F - deadAngle) {
        forwards = true
    } else if (dgs < -90.0F - deadAngle || dgs > 90.0F + deadAngle) {
        backwards = true
    }

    if (dgs > 0.0F + deadAngle && dgs < 180.0F - deadAngle) {
        right = true
    } else if (dgs > -180.0F + deadAngle && dgs < 0.0F - deadAngle) {
        left = true
    }

    return DirectionalInput(forwards, backwards, left, right)
}

/** Keys that move towards [direction] when the input is applied relative to [frameYaw]. */
fun getDirectionalInputTowards(direction: Vec3, frameYaw: Float): DirectionalInput {
    if (direction.horizontalDistanceSqr() < 1.0E-6) {
        return DirectionalInput.NONE
    }

    return getDirectionalInputForDegrees(DirectionalInput.NONE, getDegreesRelativeToView(direction, frameYaw))
}

/** The yaw the movement keys act relative to: the rotation under strict movement correction, else the view. */
fun inputFrameYaw(): Float {
    val strict = RotationManager.activeRotationTarget?.movementCorrection == MovementCorrection.STRICT
    return if (strict && RotationManager.currentRotation != null) RotationManager.movementYaw else player.yRot
}

/** Keys that move [forward] and [strafe] relative to [RotationManager.movementYaw]. */
fun getDirectionalInputForMovementYaw(forward: Int, strafe: Int, frameYaw: Float): DirectionalInput {
    val rotation = RotationManager.currentRotation
    if (rotation == null || RotationManager.activeRotationTarget?.movementCorrection != MovementCorrection.SILENT) {
        return DirectionalInput(forward.toFloat(), strafe.toFloat())
    }

    // Silent correction reads the keys relative to the camera at frameYaw and turns them onto the rotation.
    val delta = (frameYaw - rotation.yaw) * Mth.DEG_TO_RAD
    val cos = Mth.cos(delta.toDouble())
    val sin = Mth.sin(delta.toDouble())
    for (z in -1..1) {
        for (x in -1..1) {
            val turnedX = (x * cos - z * sin).roundToInt()
            val turnedZ = (z * cos + x * sin).roundToInt()
            if (turnedX == strafe && turnedZ == forward) {
                return DirectionalInput(z.toFloat(), x.toFloat())
            }
        }
    }
    return DirectionalInput(forward.toFloat(), strafe.toFloat())
}

fun findEdgeCollision(
    from: Vec3,
    to: Vec3,
    allowedDropDown: Float = 0.5F,
): Vec3? {
    val lineVec = to - from
    if (lineVec.lengthSqr() <= 1.0E-12) {
        return null
    }

    val boundingBoxes = collectCollisionBoundingBoxes(from, to, allowedDropDown)

    var currentFrom = from

    val extendedFrom = from.fma(-1000.0, lineVec)
    val extendedTo = to.fma(1000.0, lineVec)

    val cache = objectHashSetOf<AABB>()
    while (true) {
        val boxesContainingFrom = boundingBoxes.filterTo(cache) { it.contains(currentFrom) }

        // If there is no bounding box containing from, we would fall off
        if (boxesContainingFrom.isEmpty()) {
            return currentFrom
        }

        // If there is a bounding box that contains from and to, we won't collide with an edge
        if (boxesContainingFrom.any { it.contains(to) }) {
            return null
        }

        currentFrom =
            boxesContainingFrom.mapToArray {
                val res = it.clip(extendedTo, extendedFrom)

                // This ray-cast should never fail.
                requireNotNull(res.orElse(null)) {
                    "Raycast failed. This should be impossible. AABB=$it from=$from to=$to"
                }
            }.minBy { it.distanceToSqr(to) }

        boundingBoxes.removeAll(boxesContainingFrom)
        cache.clear()
    }
}

private fun collectCollisionBoundingBoxes(
    from: Vec3,
    to: Vec3,
    allowedDropDown: Float,
): ArrayList<AABB> {
    val playerDims = mc.player!!.getDimensions(Pose.STANDING)

    val fromBox: AABB = playerDims.makeBoundingBox(from)
    val toBox: AABB = playerDims.makeBoundingBox(to)

    val unionBox = fromBox.minmax(toBox)

    val fromBlockPos =
        BlockPos.containing(
            unionBox.minX - 0.3 - 1.0E-7,
            unionBox.minY - allowedDropDown - 1.0E-7,
            unionBox.minZ - 0.3 - 1.0E-7,
        )
    val toBlockPos =
        BlockPos.containing(
            unionBox.maxX + 0.3 + 1.0E-7,
            unionBox.minY + 1.0E-7,
            unionBox.maxZ + 0.3 + 1.0E-7,
        )

    val lineVec = to.subtract(from)
    val extendedFrom = from.fma(-1000.0, lineVec)
    val extendedTo = to.fma(1000.0, lineVec)

    val foundBoxes = ArrayList<AABB>()

    val world = mc.level!!

    for (pos in fromBlockPos..toBlockPos) {
        val state = world.getBlockState(pos)

        val collisionShape = state.getCollisionShape(world, pos)

        collisionShape.forAllBoxes { minX, minY, minZ, maxX, maxY, maxZ ->
            val adjustedBox =
                AABB(
                    minX - 0.3,
                    minY - 1.0,
                    minZ - 0.3,
                    maxX + 0.3,
                    maxY + allowedDropDown + 0.05,
                    maxZ + 0.3,
                ).move(pos)

            if (adjustedBox.clip(extendedFrom, extendedTo).isEmpty) {
                return@forAllBoxes
            }

            foundBoxes.add(adjustedBox)
        }
    }

    return foundBoxes
}

inline fun LocalPlayer.setDeltaMovement(block: (Vec3) -> Vec3) {
    this.deltaMovement = block(this.deltaMovement)
}

@AddonApi
fun LocalPlayer.stopXZVelocity() {
    this.deltaMovement = this.deltaMovement.copy(x = 0.0, z = 0.0)
}

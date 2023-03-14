/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2023 CCBlueX
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

import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.event.GameTickEvent
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.PlayerVelocityStrafe
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.entity.getNearestPoint
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.kotlin.step
import net.minecraft.block.BlockState
import net.minecraft.block.ShapeContext
import net.minecraft.util.math.*
import org.apache.commons.lang3.RandomUtils
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * Configurable to configure the dynamic rotation engine
 */
class RotationsConfigurable : Configurable("Rotations") {
    val turnSpeed by curve("TurnSpeed", arrayOf(4f, 7f, 10f, 3f, 2f, 0.7f))
    val fixVelocity by boolean("FixVelocity", true)
}

/**
 * A rotation manager
 */
object RotationManager : Listenable {

    var targetRotation: Rotation? = null
    val serverRotation: Rotation
        get() = Rotation(mc.player?.lastYaw ?: 0f, mc.player?.lastPitch ?: 0f)

    // Current rotation
    var currentRotation: Rotation? = null
    var lastRotation: Rotation? = null
    var ticksUntilReset: Int = 0

    // Active configurable
    var activeConfigurable: RotationsConfigurable? = null

    // useful for something like autopot
    var deactivateManipulation = false

    fun raytraceBlock(
        eyes: Vec3d, pos: BlockPos, state: BlockState, range: Double, wallsRange: Double
    ): VecRotation? {
        val offset = Vec3d(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
        val shape = state.getOutlineShape(mc.world, pos, ShapeContext.of(mc.player))

        for (box in shape.boundingBoxes.sortedBy { -(it.maxX - it.minX) * (it.maxY - it.minY) * (it.maxZ - it.minZ) }) {
            return raytraceBox(eyes, box.offset(offset), range, wallsRange, expectedTarget = pos) ?: continue
        }

        return null
    }

    /**
     * Find the best spot of a box to aim at.
     */
    fun raytraceBox(
        eyes: Vec3d,
        box: Box,
        range: Double,
        wallsRange: Double,
        expectedTarget: BlockPos? = null,
        pattern: Pattern = GaussianPattern,
    ): VecRotation? {
        val preferredSpot = pattern.spot(box)
        val preferredRotation = makeRotation(preferredSpot, eyes)

        val rangeSquared = range * range
        val wallsRangeSquared = wallsRange * wallsRange

        var visibleRot: VecRotation? = null
        var notVisibleRot: VecRotation? = null

        // There are some spots that loops cannot detect, therefore this is used
        // since it finds the nearest spot. Should only be used when all loops have failed to find a spot.
        val nearestSpot = getNearestPoint(eyes, box)
        val nearestDistance = eyes.squaredDistanceTo(nearestSpot)

        for (x in 0.0..1.0 step 0.1) {
            for (y in 0.0..1.0 step 0.1) {
                for (z in 0.0..1.0 step 0.1) {
                    var vec3 = Vec3d(
                        box.minX + (box.maxX - box.minX) * x,
                        box.minY + (box.maxY - box.minY) * y,
                        box.minZ + (box.maxZ - box.minZ) * z
                    )

                    // skip because of out of range
                    var distance = eyes.squaredDistanceTo(vec3)

                    // if loop ended with no results, then make use of the nearest spot as last resort
                    val useNearestSpot = (x + y + z).toFloat() == 3.0f && visibleRot == null && notVisibleRot == null

                    if (distance > rangeSquared) {
                        if (useNearestSpot) {
                            vec3 = nearestSpot
                            distance = nearestDistance
                        } else {
                            continue
                        }
                    }

                    // check if target is visible to eyes
                    val visible = if (expectedTarget != null) {
                        facingBlock(eyes, vec3, expectedTarget)
                    } else {
                        isVisible(eyes, vec3)
                    }

                    // skip because not visible in range
                    if (!visible && distance > wallsRangeSquared) {
                        continue
                    }

                    val rotation = makeRotation(vec3, eyes)

                    if (visible) {
                        // Calculate next spot to preferred spot
                        if (visibleRot == null || rotationDifference(rotation, preferredRotation) < rotationDifference(
                                visibleRot.rotation, preferredRotation
                            )
                        ) {
                            visibleRot = VecRotation(rotation, vec3)
                        }
                    } else {
                        // Calculate next spot to preferred spot
                        if (notVisibleRot == null || rotationDifference(
                                rotation, preferredRotation
                            ) < rotationDifference(notVisibleRot.rotation, preferredRotation)
                        ) {
                            notVisibleRot = VecRotation(rotation, vec3)
                        }
                    }
                }
            }
        }

        return visibleRot ?: notVisibleRot
    }

    /**
     * Find the best spot of the upper side of the block
     */
    fun canSeeBlockTop(
        eyes: Vec3d, pos: BlockPos, range: Double, wallsRange: Double
    ): Boolean {
        val rangeSquared = range * range
        val wallsRangeSquared = wallsRange * wallsRange

        val minX = pos.x.toDouble()
        val y = pos.y + 0.9
        val minZ = pos.z.toDouble()

        for (x in 0.1..0.9 step 0.4) {
            for (z in 0.1..0.9 step 0.4) {
                val vec3 = Vec3d(
                    minX + x, y, minZ + z
                )

                // skip because of out of range
                val distance = eyes.squaredDistanceTo(vec3)

                if (distance > rangeSquared) {
                    continue
                }

                // check if target is visible to eyes
                val visible = facingBlock(eyes, vec3, pos, Direction.UP)

                // skip because not visible in range
                if (!visible && distance > wallsRangeSquared) {
                    continue
                }

                return true
            }

        }

        return false
    }

//    /**
//     * Find the best spot of the upper side of the block
//     */
//    fun canSeeBlockTop(
//        eyes: Vec3d,
//        pos: BlockPos,
//        range: Double,
//        wallsRange: Double
//    ): VecRotation? {
//        val rangeSquared = range * range
//        val wallsRangeSquared = wallsRange * wallsRange
//
//        var visibleRot: VecRotation? = null
//        val notVisibleRot: VecRotation? = null
//
//        val minX = pos.x.toDouble()
//        val y = pos.y.toDouble()
//        val minZ = pos.z.toDouble()
//
//        for (x in 0.1..0.9 step 0.1) {
//            for (z in 0.1..0.9 step 0.1) {
//                val vec3 = Vec3d(
//                    minX + x,
//                    y,
//                    minZ + z
//                )
//
//                // skip because of out of range
//                val distance = eyes.squaredDistanceTo(vec3)
//
//                if (distance > rangeSquared) {
//                    continue
//                }
//
//                // check if target is visible to eyes
//                val visible = facingBlock(eyes, vec3, pos)
//
//                // skip because not visible in range
//                if (!visible && distance > wallsRangeSquared) {
//                    continue
//                }
//
//                visibleRot = VecRotation(makeRotation(vec3, eyes), vec3)
//            }
//
//        }
//
//        return visibleRot ?: notVisibleRot
//    }

    fun aimAt(vec: Vec3d, eyes: Vec3d, ticks: Int = 5, configurable: RotationsConfigurable) =
        aimAt(makeRotation(vec, eyes), ticks, configurable)

    fun aimAt(rotation: Rotation, ticks: Int = 5, configurable: RotationsConfigurable) {
        if (!shouldUpdate()) {
            return
        }

        activeConfigurable = configurable
        targetRotation = rotation
        ticksUntilReset = ticks
    }

    fun makeRotation(vec: Vec3d, eyes: Vec3d): Rotation {
        val diffX = vec.x - eyes.x
        val diffY = vec.y - eyes.y
        val diffZ = vec.z - eyes.z

        return Rotation(
            MathHelper.wrapDegrees(Math.toDegrees(atan2(diffZ, diffX)).toFloat() - 90f),
            MathHelper.wrapDegrees((-Math.toDegrees(atan2(diffY, sqrt(diffX * diffX + diffZ * diffZ)))).toFloat())
        )
    }

    /**
     * Update current rotation to new rotation step
     */
    fun update() {
        // Update reset ticks
        if (ticksUntilReset > 0) {
            ticksUntilReset--
        }

        // Update patterns
        for (pattern in AIMING_PATTERNS) {
            pattern.update()
        }

        // Update rotations
        val turnSpeed = RandomUtils.nextFloat(60f, 80f) // todo: use config

        val playerRotation = mc.player?.rotation ?: return

        if (ticksUntilReset == 0 || !shouldUpdate()) {
            val threshold = 2f // todo: might use turn speed

            if (rotationDifference(currentRotation ?: serverRotation, playerRotation) < threshold) {
                ticksUntilReset = -1

                targetRotation = null
                currentRotation?.let { rotation ->
                    mc.player?.let { player ->
                        player.yaw = rotation.yaw + angleDifference(player.yaw, rotation.yaw)
                        player.renderYaw = player.yaw
                        player.lastRenderYaw = player.yaw
                    }
                }
                currentRotation = null
                lastRotation = null
                return
            }

            lastRotation = currentRotation ?: serverRotation
            currentRotation =
                limitAngleChange(currentRotation ?: serverRotation, playerRotation, turnSpeed).fixedSensitivity()
            return
        }
        targetRotation?.let { targetRotation ->
            lastRotation = currentRotation ?: playerRotation
            currentRotation =
                limitAngleChange(currentRotation ?: playerRotation, targetRotation, turnSpeed).fixedSensitivity()
        }
    }

    /**
     * Checks if it should update the server-side rotations
     */
    fun shouldUpdate() = !deactivateManipulation

    /**
     * Calculate difference between the server rotation and your rotation
     */
    fun rotationDifference(rotation: Rotation): Double {
        return rotationDifference(rotation, serverRotation)
    }

    /**
     * Calculate difference between two rotations
     */
    fun rotationDifference(a: Rotation, b: Rotation) =
        hypot(angleDifference(a.yaw, b.yaw).toDouble(), (a.pitch - b.pitch).toDouble())

    /**
     * Limit your rotations
     */
    fun limitAngleChange(currentRotation: Rotation, targetRotation: Rotation, turnSpeed: Float): Rotation {
        val yawDifference = angleDifference(targetRotation.yaw, currentRotation.yaw)
        val pitchDifference = angleDifference(targetRotation.pitch, currentRotation.pitch)

        val rotationDifference = hypot(yawDifference, pitchDifference)

        val straightLineYaw = abs(yawDifference / rotationDifference) * turnSpeed
        val straightLinePitch = abs(pitchDifference / rotationDifference) * turnSpeed

        return Rotation(
            currentRotation.yaw + yawDifference.coerceIn(-straightLineYaw, straightLineYaw),
            currentRotation.pitch + pitchDifference.coerceIn(-straightLinePitch, straightLinePitch)
        )
    }

    /**
     * Calculate difference between two angle points
     */
    private fun angleDifference(a: Float, b: Float) = MathHelper.wrapDegrees(a - b)

    val velocityHandler = handler<PlayerVelocityStrafe> { event ->
        if (activeConfigurable?.fixVelocity == true) {
            event.velocity = fixVelocity(event.velocity, event.movementInput, event.speed)
        }
    }

    val tickHandler = handler<GameTickEvent> {
        if (targetRotation == null) {
            return@handler
        }

        update()
    }

    /**
     * Fix velocity
     */
    private fun fixVelocity(currVelocity: Vec3d, movementInput: Vec3d, speed: Float): Vec3d {
        currentRotation?.let { rotation ->
            val yaw = rotation.yaw
            val d = movementInput.lengthSquared()

            return if (d < 1.0E-7) {
                Vec3d.ZERO
            } else {
                val vec3d = (if (d > 1.0) movementInput.normalize() else movementInput).multiply(speed.toDouble())

                val f = MathHelper.sin(yaw * 0.017453292f)
                val g = MathHelper.cos(yaw * 0.017453292f)

                Vec3d(
                    vec3d.x * g.toDouble() - vec3d.z * f.toDouble(),
                    vec3d.y,
                    vec3d.z * g.toDouble() + vec3d.x * f.toDouble()
                )
            }
        }

        return currVelocity
    }

}

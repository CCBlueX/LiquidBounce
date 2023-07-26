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

import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.event.GameTickEvent
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.PlayerVelocityStrafe
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.entity.getNearestPoint
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.kotlin.step
import net.minecraft.block.BlockState
import net.minecraft.block.ShapeContext
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.entity.Entity
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
    val silentRotation by boolean("SilentRotation", true)
    val turnSpeed by floatRange("TurnSpeed", 40f..60f, 0f..180f)
    val fixVelocity by boolean("FixVelocity", true)
    val threshold by float("Threshold", 2f, 0f..50f)
    val keepRotationTicks by int("KeepRotationTicks", 30, 0..300)
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
    var ticksUntilReset: Int = 0
    var ignoreOpenInventory = false

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
        val rangeSquared = range * range
        val wallsRangeSquared = wallsRange * wallsRange

        val preferredSpot = pattern.spot(box)
        val preferredRotation = makeRotation(preferredSpot, eyes)
        val preferredDistance = eyes.squaredDistanceTo(preferredSpot)

        val isPreferredSpotVisible = if (expectedTarget != null) {
            facingBlock(eyes, preferredSpot, expectedTarget)
        } else {
            preferredDistance <= rangeSquared && isVisible(eyes, preferredSpot)
        }

        // If pattern-generated spot is visible or its distance is within wall range, then return right here.
        // No need to enter the loop when we already have a result.
        if (isPreferredSpotVisible || preferredDistance <= wallsRangeSquared) {
            return VecRotation(preferredRotation, preferredSpot)
        }

        var visibleRot: VecRotation? = null
        var notVisibleRot: VecRotation? = null

        // There are some spots that loops cannot detect, therefore this is used
        // since it finds the nearest spot within the requested range.
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

                    var distance = eyes.squaredDistanceTo(vec3)

                    // Start off with the nearest spot, then see which is better
                    val useNearestSpot = visibleRot == null && notVisibleRot == null

                    // If the distance is greater than range, use the nearest spot.
                    if (distance > rangeSquared && useNearestSpot) {
                        distance = nearestDistance
                        vec3 = nearestSpot
                    }

                    val visible = if (expectedTarget != null) {
                        facingBlock(eyes, vec3, expectedTarget)
                    } else {
                        distance <= rangeSquared && isVisible(eyes, vec3)
                    }

                    // Is either spot visible or distance within wall range?
                    if (visible || distance <= wallsRangeSquared) {
                        val rotation = makeRotation(vec3, eyes)
                        val currentRotation = currentRotation ?: mc.player?.rotation ?: return null

                        if (visible) {
                            if (visibleRot == null || rotationDifference(
                                    rotation, currentRotation
                                ) < rotationDifference(visibleRot.rotation, currentRotation)
                            ) {
                                visibleRot = VecRotation(rotation, vec3)
                            }
                        } else {
                            if (notVisibleRot == null || rotationDifference(
                                    rotation, currentRotation
                                ) < rotationDifference(notVisibleRot.rotation, currentRotation)
                            ) {
                                notVisibleRot = VecRotation(rotation, vec3)
                            }
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

    fun aimAt(vec: Vec3d, eyes: Vec3d, openInventory: Boolean = false, configurable: RotationsConfigurable) =
        aimAt(makeRotation(vec, eyes), openInventory, configurable)

    fun aimAt(rotation: Rotation, openInventory: Boolean = false, configurable: RotationsConfigurable) {
        if (!shouldUpdate()) {
            return
        }

        activeConfigurable = configurable
        targetRotation = rotation
        ticksUntilReset = configurable.keepRotationTicks
        ignoreOpenInventory = openInventory
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

    val gcd: Double
        get() {
            val f = mc.options.mouseSensitivity.value * 0.6F.toDouble() + 0.2F.toDouble()
            return f * f * f * 8.0 * 0.15F
        }

    /**
     * Update current rotation to new rotation step
     */
    fun update() {
        // Prevents any rotation changes, when inventory is opened
        val canRotate =
            (mc.currentScreen !is InventoryScreen && mc.currentScreen !is GenericContainerScreen) || ignoreOpenInventory
        // Update reset ticks
        if (ticksUntilReset > 0) {
            ticksUntilReset--
        }

        // Update patterns
        for (pattern in AIMING_PATTERNS) {
            pattern.update()
        }

        // Update rotations
        val turnSpeed =
            RandomUtils.nextFloat(activeConfigurable!!.turnSpeed.start, activeConfigurable!!.turnSpeed.endInclusive)

        val playerRotation = mc.player?.rotation ?: return

        if (ticksUntilReset == 0 || !shouldUpdate() || !activeConfigurable!!.silentRotation) {

            if (rotationDifference(
                    currentRotation ?: serverRotation,
                    playerRotation
                ) < activeConfigurable!!.threshold || !activeConfigurable!!.silentRotation
            ) {
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
                return
            }

            if (canRotate) {
                limitAngleChange(currentRotation ?: serverRotation, playerRotation, turnSpeed).fixedSensitivity().let {
                    currentRotation = it
                    if (!activeConfigurable!!.silentRotation)
                        mc.player!!.applyRotation(it)
                }
            }
            return
        }
        if (canRotate) {
            targetRotation?.let { targetRotation ->
                limitAngleChange(
                    currentRotation ?: playerRotation,
                    targetRotation,
                    turnSpeed
                ).fixedSensitivity().let {
                    currentRotation = it
                    if (!activeConfigurable!!.silentRotation)
                        mc.player!!.applyRotation(it)
                }
            }
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
     * Calculate difference between an entity and your rotation
     */
    fun rotationDifference(entity: Entity): Double {
        val player = mc.player ?: return 0.0
        val eyes = player.eyes

        return rotationDifference(makeRotation(entity.box.center, eyes), player.rotation).coerceAtMost(180.0)
    }

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
    fun angleDifference(a: Float, b: Float) = MathHelper.wrapDegrees(a - b)

    val velocityHandler = handler<PlayerVelocityStrafe> { event ->
        if (activeConfigurable?.fixVelocity == true) {
            event.velocity = fixVelocity(event.velocity, event.movementInput, event.speed)
        }
    }

    val tickHandler = handler<GameTickEvent> {
        if (targetRotation == null || mc.isPaused) {
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

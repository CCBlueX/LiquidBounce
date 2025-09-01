/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.movement.autododge

import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.utils.rayTraceCollidingBlocks
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.toRadians
import net.ccbluex.liquidbounce.utils.entity.getMovementDirectionOfInput
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.movement.getDegreesRelativeToView
import net.ccbluex.liquidbounce.utils.movement.getDirectionalInputForDegrees
import net.minecraft.util.math.Vec3d
import kotlin.math.cos
import kotlin.math.sin

data class DodgePlan(
    val directionalInput: DirectionalInput,
    /**
     * Should the player jump at the next possible time?
     */
    val shouldJump: Boolean,
    val yawChange: Float?,
    val useTimer: Boolean,
)

data class DodgePlannerConfig(
    val allowRotations: Boolean,
)

fun planEvasion(
    config: DodgePlannerConfig,
    inflictedHit: ModuleAutoDodge.HitInfo,
): DodgePlan? {
    val player = mc.player!!
    val arrowLine =
        Line(
            Vec3d(inflictedHit.prevArrowPos.x, 0.0, inflictedHit.prevArrowPos.z),
            Vec3d(inflictedHit.arrowVelocity.x, 0.0, inflictedHit.arrowVelocity.z),
        )

    val playerPos2d = Vec3d(player.pos.x, 0.0, player.pos.z)
    val nearestPointOnArrowLine = arrowLine.getNearestPointTo(playerPos2d)
    val distanceToArrowLine = nearestPointOnArrowLine.distanceTo(playerPos2d)

    // Check if we are in the danger zone. If we are not in the danger zone there is no need to dodge (yet).
    if (distanceToArrowLine > DodgePlanner.SAFE_DISTANCE_WITH_PADDING) {
        return null
    }

    val optimalDodgePosition = findOptimalDodgePosition(arrowLine)

    val positionRelativeToPlayer = optimalDodgePosition.subtract(playerPos2d)

    return DodgePlanner(config, inflictedHit, distanceToArrowLine, positionRelativeToPlayer).plan()
}

class DodgePlanner(
    private val config: DodgePlannerConfig,
    private val hypotheticalHit: ModuleAutoDodge.HitInfo,
    private val distanceToArrowLine: Double,
    private val optimalDodgePosRelativeToPlayer: Vec3d,
) : MinecraftShortcuts {
    fun plan(): DodgePlan {
        val inputForEvasionWithCurrentRotation =
            getDodgeMovementWithoutAngleChange(this.optimalDodgePosRelativeToPlayer)

        val dodgePlanWithoutRotationChange =
            DodgePlan(
                directionalInput = inputForEvasionWithCurrentRotation,
                shouldJump = false,
                yawChange = null,
                useTimer = false,
            )

        // We are already out of danger zone. Don't escalate the fix.
        if (this.distanceToArrowLine > SAFE_DISTANCE) {
            return dodgePlanWithoutRotationChange
        }

        val escalatedDodgePlan = escalateIfNeeded(dodgePlanWithoutRotationChange)

        return escalatedDodgePlan ?: dodgePlanWithoutRotationChange
    }

    private fun escalateIfNeeded(dodgePlanWithoutRotationChange: DodgePlan): DodgePlan? {
        // Check if the time is sufficient to dodge and apply another fix that will do the evasion.

        val actualAngle = getMovementDirectionOfInput(player.yaw, dodgePlanWithoutRotationChange.directionalInput)

        val effectivenessLossByAngle = getEffectiveLossByInoptimalAngle(actualAngle)
        val distanceToTravel = optimalDodgePosRelativeToPlayer.length() - (SAFE_DISTANCE_WITH_PADDING - SAFE_DISTANCE)
        val travelTimeWithRelativeMovements = distanceToTravel / (effectivenessLossByAngle * 0.11)

        val actualTimeToImpact = this.hypotheticalHit.tickDelta + 1

        if (actualTimeToImpact > travelTimeWithRelativeMovements) {
            // No need to escalate the fix.
            return null
        }

        val useTimer = shouldUseTimer(distanceToTravel, actualTimeToImpact)

        return if (this.config.allowRotations) {
            planWithRotations(distanceToTravel, actualTimeToImpact, useTimer)
        } else {
            // We cannot plan with rotations, we have reached our limits. But we can still apply timer
            dodgePlanWithoutRotationChange.copy(
                useTimer = useTimer,
            )
        }
    }

    private fun planWithRotations(
        distanceToTravel: Double,
        actualTimeToImpact: Int,
        useTimer: Boolean,
    ): DodgePlan {
        // The part of the velocity that is effective for the dodge
        val effectiveVelocity = player.velocity.length() * similarity(player.velocity, optimalDodgePosRelativeToPlayer)
        // Rotations enable sprint
        val travelTimeWithRotation = distanceToTravel / (0.13)

        // If we cannot evade with just sprinting, we need to jump
        val shouldJumpIfPossible = actualTimeToImpact < travelTimeWithRotation
        // If the velocity is too low, we don't want to jump since we cannot get more momentum midair
        val isJumpEffective = effectiveVelocity > 0.11

        val rotation =
            Rotation.lookingAt(point = player.pos + optimalDodgePosRelativeToPlayer, from = player.eyePos).normalize()

        return DodgePlan(
            directionalInput = DirectionalInput.FORWARDS,
            shouldJump = shouldJumpIfPossible && isJumpEffective,
            yawChange = rotation.yaw,
            useTimer = useTimer,
        )
    }

    private fun shouldUseTimer(
        distanceToTravel: Double,
        actualTimeToImpact: Int,
    ): Boolean {
        return if (this.config.allowRotations) {
            (distanceToTravel / 0.155) / (actualTimeToImpact + 1) > 1.6
        } else {
            (distanceToTravel / 0.11) / (actualTimeToImpact + 1) > 1.6
        }
    }

    private fun getEffectiveLossByInoptimalAngle(actualAngle: Float): Double {
        // This vector represents the angle that we are currently moving in
        val angleVec = Vec3d(-sin(actualAngle.toRadians().toDouble()), 0.0, cos(actualAngle.toRadians().toDouble()))

        // Here we project the optimal dodge position onto the angle vector. This gives us the effective loss
        return similarity(angleVec, optimalDodgePosRelativeToPlayer)
    }

    private fun similarity(
        a: Vec3d,
        b: Vec3d,
    ): Double {
        return a.dotProduct(b) / (a.length() * b.length())
    }

    companion object {
        const val SAFE_DISTANCE: Double = 1.5 * 0.3 + 1.5 * 0.5
        const val SAFE_DISTANCE_WITH_PADDING: Double = 0.3 * 5
    }
}

private fun getDodgeMovementWithoutAngleChange(positionRelativeToPlayer: Vec3d): DirectionalInput {
    val dgs = getDegreesRelativeToView(positionRelativeToPlayer)

    return getDirectionalInputForDegrees(DirectionalInput.NONE, dgs, deadAngle = 20.0F)
}

fun findOptimalDodgePosition(baseLine: Line): Vec3d {
    val player = mc.player!!

    val playerPos2d = Vec3d(player.pos.x, 0.0, player.pos.z)
    // Usually it takes around two ticks to change the movement to whatever we want. In this time we will keep the
    // current velocity. So we have to account for this by integrating the player's velocity in the calculation.
    val playerPosAfterFreeMovement = playerPos2d.add(player.velocity.x * 2.0, 0.0, player.velocity.z * 2.0)

    val dangerZone = getDangerZoneBorders(baseLine, DodgePlanner.SAFE_DISTANCE_WITH_PADDING)

    val nearestPointsToDangerZoneBorders =
        dangerZone.map { it.getNearestPointTo(playerPosAfterFreeMovement) }
    val nearestPointDistancesToPlayer =
        nearestPointsToDangerZoneBorders.map { it.distanceTo(playerPosAfterFreeMovement) }

    val nearestPosToLine = baseLine.getNearestPointTo(playerPos2d)

    // Check if one direction is not viable because we would collide with a block
    when {
        getWalkableDistance(nearestPosToLine, nearestPointsToDangerZoneBorders[0]) < DodgePlanner.SAFE_DISTANCE -> {
            return nearestPointsToDangerZoneBorders[1]
        }

        getWalkableDistance(nearestPosToLine, nearestPointsToDangerZoneBorders[1]) < DodgePlanner.SAFE_DISTANCE -> {
            return nearestPointsToDangerZoneBorders[0]
        }
    }

    // Find the nearest point that is outside the danger zone
    return if (nearestPointDistancesToPlayer[0] < nearestPointDistancesToPlayer[1] - 0.05) {
        nearestPointsToDangerZoneBorders[0]
    } else {
        nearestPointsToDangerZoneBorders[1]
    }
}

fun getWalkableDistance(basePos: Vec3d, dodgePos: Vec3d): Double {
    val playerY = mc.player!!.y
    val rayYs = doubleArrayOf(0.6, 1.6)

    val worstRay =
        rayYs
            .map {
                val rayFrom = Vec3d(basePos.x, playerY + it, basePos.z)
                val rayTo = Vec3d(dodgePos.x, playerY + it, dodgePos.z)

                val realRayTo = rayTraceCollidingBlocks(rayFrom, rayTo)?.pos ?: rayTo

                Pair(rayFrom, realRayTo)
            }
            .minBy { (rayFrom, realRayTo) ->
                rayFrom.squaredDistanceTo(realRayTo)
            }

    return worstRay.first.distanceTo(worstRay.second)
}

/**
 * Returns the two lines at the border of the danger zone (in 2D)
 */
private fun getDangerZoneBorders(
    baseLine: Line,
    distanceFromBaseLine: Double,
): Array<Line> {
    val orthoVecToBaseLine = baseLine.direction.crossProduct(Vec3d(0.0, 1.0, 0.0)).normalize()

    val orthoOffsetVec = orthoVecToBaseLine.multiply(distanceFromBaseLine)

    val lineLeft = Line(baseLine.position.subtract(orthoOffsetVec), baseLine.direction)
    val lineRight = Line(baseLine.position.add(orthoOffsetVec), baseLine.direction)

    return arrayOf(lineLeft, lineRight)
}

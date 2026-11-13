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

package net.ccbluex.liquidbounce.utils.navigation

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.MouseRotationEvent
import net.ccbluex.liquidbounce.event.events.PlayerPostTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.features.MovementCorrection
import net.ccbluex.liquidbounce.utils.aiming.utils.RotationUtil
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.math.horizontalDistanceTo
import net.ccbluex.liquidbounce.utils.math.horizontalDistanceToSqr
import net.ccbluex.liquidbounce.utils.math.toDegrees
import net.ccbluex.liquidbounce.utils.math.yaw
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Turns the camera the way navigation goes, eased and capped per tick like a person looking ahead. Rotations of
 * other features stay server-side; a rotation that moves the view (ChangeLook) or a mouse turn takes over instead.
 *
 * The view only changes after the player tick, so the movement keys of the next tick are worked out relative to the
 * same yaw the player then moves with; the camera interpolates between the old and the new yaw in between.
 */
class LookAhead(owner: EventListener) : ToggleableValueGroup(owner, "LookAhead", true) {

    private val turnSpeed by float("TurnSpeed", 10f, 1f..40f, "°")

    /** How far along the route the point looked at lies. */
    val distance by float("Distance", 4f, 1f..12f, "blocks")

    /** Downwards from the horizon. */
    private val pitch by float("Pitch", 8f, -30f..45f, "°")

    private var point: Vec3? = null
    private var aimed = false
    private var yawSpeed = 0f
    private var pitchSpeed = 0f
    private var mouseHold = 0

    /** Looks towards [point] after this tick, or the way the player moves without one. Call every tick it walks. */
    fun aim(point: Vec3?) {
        this.point = point
        aimed = true
    }

    fun reset() {
        point = null
        aimed = false
        yawSpeed = 0f
        pitchSpeed = 0f
    }

    @Suppress("unused")
    private val mouseHandler = handler<MouseRotationEvent> { event ->
        if (event.cursorDeltaX != 0.0 || event.cursorDeltaY != 0.0) {
            mouseHold = MOUSE_HOLD_TICKS
        }
    }

    @Suppress("unused")
    private val tickHandler = handler<PlayerPostTickEvent> {
        val wasAimed = aimed
        val point = point
        this.point = null
        aimed = false

        if (mouseHold > 0) {
            mouseHold--
            stopTurning()
            return@handler
        }
        if (isViewTakenOver()) {
            stopTurning()
            return@handler
        }

        if (wasAimed) {
            val yaw = targetYaw(point)
            val yawDelta = if (yaw.isNaN()) 0f else Mth.wrapDegrees(yaw - player.yRot)
            turn(yawDelta, targetPitch(point) - player.xRot)
        } else if (yawSpeed != 0f || pitchSpeed != 0f) {
            // Navigation stopped: the turn slows down instead of ending at once.
            turn(0f, 0f)
        }
    }

    private fun isViewTakenOver() = RotationManager.currentRotation != null &&
        RotationManager.activeRotationTarget?.movementCorrection == MovementCorrection.CHANGE_LOOK

    /** NaN to keep the yaw: the point lies too close to tell a way, or the player stands still. */
    private fun targetYaw(point: Vec3?): Float {
        val position = player.position()
        if (point != null) {
            val near = point.horizontalDistanceToSqr(position) < MIN_POINT_DISTANCE * MIN_POINT_DISTANCE
            return if (near) Float.NaN else point.subtract(position).yaw
        }

        val motion = player.rootVehicle.deltaMovement
        return if (motion.horizontalDistanceSqr() >= MIN_MOTION * MIN_MOTION) motion.yaw else Float.NaN
    }

    /** Towards the horizon, tilted by part of the route's slope up to the point; steep up or down a ladder. */
    private fun targetPitch(point: Vec3?): Float {
        if (point == null) {
            return pitch
        }

        val position = player.position()
        val rise = point.y - position.y
        val horizontal = point.horizontalDistanceTo(position)
        if (horizontal < MIN_POINT_DISTANCE && abs(rise) < MIN_POINT_DISTANCE) {
            return pitch
        }

        val slope = atan2(rise, max(horizontal, MIN_POINT_DISTANCE)).toDegrees().toFloat()
        return (pitch - slope * SLOPE_FOLLOW).coerceIn(MIN_PITCH, MAX_PITCH)
    }

    /**
     * Eases towards the deltas: the wanted speed shrinks with the angle left and is capped at [turnSpeed], and the
     * actual speed changes by a limited amount per tick so turns start and end softly.
     */
    private fun turn(yawDelta: Float, pitchDelta: Float) {
        val maxChange = turnSpeed * ACCELERATION
        yawSpeed = approach(yawSpeed, (yawDelta * EASING).coerceIn(-turnSpeed, turnSpeed), maxChange)
        val maxPitchSpeed = turnSpeed * PITCH_SHARE
        pitchSpeed = approach(pitchSpeed, (pitchDelta * EASING).coerceIn(-maxPitchSpeed, maxPitchSpeed), maxChange)

        if (abs(yawSpeed) < REST_SPEED && abs(yawDelta) < REST_SPEED) {
            yawSpeed = 0f
        }
        if (abs(pitchSpeed) < REST_SPEED && abs(pitchDelta) < REST_SPEED) {
            pitchSpeed = 0f
        }

        // Steps in whole mouse counts, like a turn made with the mouse.
        val gcd = RotationUtil.gcd.toFloat()
        player.yRot += (yawSpeed / gcd).roundToInt() * gcd
        player.xRot = (player.xRot + (pitchSpeed / gcd).roundToInt() * gcd).coerceIn(-90f, 90f)
    }

    private fun stopTurning() {
        yawSpeed = 0f
        pitchSpeed = 0f
    }

    private fun approach(current: Float, wanted: Float, maxChange: Float) =
        current + (wanted - current).coerceIn(-maxChange, maxChange)

    private companion object {
        const val EASING = 0.2f
        const val ACCELERATION = 0.3f
        const val PITCH_SHARE = 0.5f
        const val SLOPE_FOLLOW = 0.5f
        const val MIN_PITCH = -30f
        const val MAX_PITCH = 60f
        const val REST_SPEED = 0.05f
        const val MIN_POINT_DISTANCE = 1.0
        const val MIN_MOTION = 0.05
        const val MOUSE_HOLD_TICKS = 40
    }

}

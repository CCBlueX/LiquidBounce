/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.event.StrafeEvent
import net.ccbluex.liquidbounce.utils.RotationUtils.getFixedAngleDelta
import net.ccbluex.liquidbounce.utils.RotationUtils.getFixedSensitivityAngle
import net.ccbluex.liquidbounce.utils.RotationUtils.serverRotation
import net.ccbluex.liquidbounce.utils.block.PlaceInfo
import net.ccbluex.liquidbounce.utils.extensions.toRadians
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.MathHelper
import net.minecraft.util.Vec3
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Rotations
 */
data class Rotation(var yaw: Float, var pitch: Float) : MinecraftInstance() {

    /**
     * Set rotations to [player]
     */
    fun toPlayer(player: EntityPlayer) {
        if (yaw.isNaN() || pitch.isNaN() || pitch > 90 || pitch < -90) return

        fixedSensitivity()

        player.rotationYaw = yaw
        player.rotationPitch = pitch
    }

    /**
     * Patch gcd exploit in aim
     *
     * @see net.minecraft.client.renderer.EntityRenderer.updateCameraAndRender
     */
    fun fixedSensitivity(sensitivity: Float = mc.gameSettings.mouseSensitivity): Rotation {
        // Previous implementation essentially floored the subtraction.
        // This way it returns rotations closer to the original.

        // Only calculate GCD once
        val gcd = getFixedAngleDelta(sensitivity)

        yaw = getFixedSensitivityAngle(yaw, serverRotation.yaw, gcd)
        pitch = getFixedSensitivityAngle(pitch, serverRotation.pitch, gcd).coerceIn(-90f, 90f)

        return this
    }

    /**
     * Apply strafe to player
     *
     * @author bestnub
     */
    fun applyStrafeToPlayer(event: StrafeEvent, strict: Boolean = false) {
        val player = mc.thePlayer

        val diff = ((MathHelper.wrapAngleTo180_float(player.rotationYaw - yaw - 23.5f - 135) + 180) / 45).toInt()

        val strafe = event.strafe
        val forward = event.forward
        val friction = event.friction

        var calcForward = 0f
        var calcStrafe = 0f

        if (!strict) {
            when (diff) {
                0 -> {
                    calcForward = forward
                    calcStrafe = strafe
                }

                1 -> {
                    calcForward += forward
                    calcStrafe -= forward
                    calcForward += strafe
                    calcStrafe += strafe
                }

                2 -> {
                    calcForward = strafe
                    calcStrafe = -forward
                }

                3 -> {
                    calcForward -= forward
                    calcStrafe -= forward
                    calcForward += strafe
                    calcStrafe -= strafe
                }

                4 -> {
                    calcForward = -forward
                    calcStrafe = -strafe
                }

                5 -> {
                    calcForward -= forward
                    calcStrafe += forward
                    calcForward -= strafe
                    calcStrafe -= strafe
                }

                6 -> {
                    calcForward = -strafe
                    calcStrafe = forward
                }

                7 -> {
                    calcForward += forward
                    calcStrafe += forward
                    calcForward -= strafe
                    calcStrafe += strafe
                }
            }

            if (calcForward > 1f || calcForward < 0.9f && calcForward > 0.3f || calcForward < -1f || calcForward > -0.9f && calcForward < -0.3f) {
                calcForward *= 0.5f
            }

            if (calcStrafe > 1f || calcStrafe < 0.9f && calcStrafe > 0.3f || calcStrafe < -1f || calcStrafe > -0.9f && calcStrafe < -0.3f) {
                calcStrafe *= 0.5f
            }
        } else {
            calcForward = event.forward
            calcStrafe = event.strafe
        }

        var d = calcStrafe * calcStrafe + calcForward * calcForward

        if (d >= 1.0E-4f) {
            d = friction / sqrt(d).coerceAtLeast(1f)

            calcStrafe *= d
            calcForward *= d

            val yawRad = yaw.toRadians()
            val yawSin = sin(yawRad)
            val yawCos = cos(yawRad)

            player.motionX += calcStrafe * yawCos - calcForward * yawSin
            player.motionZ += calcForward * yawCos + calcStrafe * yawSin
        }
    }
}

/**
 * Rotation with vector
 */
data class VecRotation(val vec: Vec3, val rotation: Rotation)

/**
 * Rotation with place info
 */
data class PlaceRotation(val placeInfo: PlaceInfo, val rotation: Rotation)

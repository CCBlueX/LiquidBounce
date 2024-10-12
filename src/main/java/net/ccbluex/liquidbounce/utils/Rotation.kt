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
import kotlin.math.*

/**
 * Rotations
 */
data class Rotation(var yaw: Float, var pitch: Float) : MinecraftInstance() {

    operator fun minus(other: Rotation): Rotation {
        return Rotation(yaw - other.yaw, pitch - other.pitch)
    }

    operator fun plus(other: Rotation): Rotation {
        return Rotation(yaw + other.yaw, pitch + other.pitch)
    }

    operator fun times(value: Float): Rotation {
        return Rotation(yaw * value, pitch * value)
    }

    operator fun div(value: Float): Rotation {
        return Rotation(yaw / value, pitch / value)
    }

    companion object {
        val ZERO = Rotation(0f, 0f)
    }

    /**
     * Set rotations to [player]
     */
    fun toPlayer(player: EntityPlayer = mc.thePlayer, changeYaw: Boolean = true, changePitch: Boolean = true) {
        if (yaw.isNaN() || pitch.isNaN() || pitch > 90 || pitch < -90) return

        fixedSensitivity()

        if (changeYaw) player.rotationYaw = yaw
        if (changePitch) player.rotationPitch = pitch
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
        pitch = getFixedSensitivityAngle(pitch, serverRotation.pitch, gcd)

        return this.withLimitedPitch()
    }

    /**
     * Apply strafe to player
     *
     * @author bestnub
     */
    fun applyStrafeToPlayer(event: StrafeEvent, strict: Boolean = false) {
        val player = mc.thePlayer

        val diff = (player.rotationYaw - yaw).toRadians()

        val friction = event.friction

        var calcForward: Float
        var calcStrafe: Float

        if (!strict) {
            // Remove modifier, replay the updatePlayerMoveState() logic
            val (strafe, forward) = Pair(event.strafe / 0.98f, event.forward / 0.98f)

            // Filter out previous sneak / block input modifications by rounding inputs up
            val modifiedForward = ceil(abs(forward)) * forward.sign
            val modifiedStrafe = ceil(abs(strafe)) * strafe.sign

            // Remake the rotation-based input using the modified inputs
            calcForward = round(modifiedForward * MathHelper.cos(diff) + modifiedStrafe * MathHelper.sin(diff))
            calcStrafe = round(modifiedStrafe * MathHelper.cos(diff) - modifiedForward * MathHelper.sin(diff))

            // Was the user sneaking? Blocking? Both? Neither?
            val f = if (event.forward != 0f) event.forward else event.strafe

            // Apply original modifications back
            calcForward *= abs(f)
            calcStrafe *= abs(f)
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
            val yawSin = MathHelper.sin(yawRad)
            val yawCos = MathHelper.cos(yawRad)

            player.motionX += calcStrafe * yawCos - calcForward * yawSin
            player.motionZ += calcForward * yawCos + calcStrafe * yawSin
        }
    }

    fun withLimitedPitch(value: Float = 90f): Rotation {
        pitch = pitch.coerceIn(-value, value)

        return this
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
/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.utils.extensions.stopXZ
import net.ccbluex.liquidbounce.utils.extensions.toRadiansD
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object MovementUtils : MinecraftInstance() {

    var speed
        get() = mc.thePlayer?.run { sqrt(motionX * motionX + motionZ * motionZ).toFloat() } ?: .0f
        set(value) { strafe(value) }

    val isMoving
        get() = mc.thePlayer?.movementInput?.run { moveForward != 0f || moveStrafe != 0f } ?: false

    val hasMotion
        get() = mc.thePlayer?.run { motionX != .0 && motionY != .0 && motionZ != .0 } ?: false

    fun strafe(speed: Float = this.speed, stopWhenNoInput: Boolean = false) =
        mc.thePlayer?.run {
            if (!isMoving) {
                if (stopWhenNoInput)
                    stopXZ()

                return@run
            }

            val yaw = direction
            motionX = -sin(yaw) * speed
            motionZ = cos(yaw) * speed
        }

    fun forward(distance: Double) =
        mc.thePlayer?.run {
            val yaw = rotationYaw.toRadiansD()
            setPosition(posX - sin(yaw) * distance, posY, posZ + cos(yaw) * distance)
        }

    val direction
        get() = mc.thePlayer?.run {
                var yaw = rotationYaw
                var forward = 1f

                if (moveForward < 0f) {
                    yaw += 180f
                    forward = -0.5f
                } else if (moveForward > 0f)
                    forward = 0.5f

                if (moveStrafing < 0f) yaw += 90f * forward
                else if (moveStrafing > 0f) yaw -= 90f * forward

                yaw.toRadiansD()
            } ?: 0.0

    fun isOnGround(height: Double) =
        mc.theWorld != null && mc.thePlayer != null &&
        mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, mc.thePlayer.entityBoundingBox.offset(0.0, -height, 0.0)).isNotEmpty()
}
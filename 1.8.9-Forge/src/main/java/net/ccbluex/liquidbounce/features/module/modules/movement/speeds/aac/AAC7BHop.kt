/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.aac

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils
import kotlin.math.cos
import kotlin.math.sin

class AAC7BHop : SpeedMode("AAC7BHop") {
    override fun onUpdate() {
        val thePlayer = mc.thePlayer ?: return

        if (!MovementUtils.isMoving || thePlayer.ridingEntity != null || thePlayer.hurtTime > 0)
            return

        if (thePlayer.onGround) {
            thePlayer.jump()
            thePlayer.motionY = 0.405
            thePlayer.motionX *= 1.004
            thePlayer.motionZ *= 1.004
            return
        }

        val speed = MovementUtils.speed * 1.0072
        val yaw = Math.toRadians(thePlayer.rotationYaw.toDouble())

        thePlayer.motionX = -sin(yaw) * speed
        thePlayer.motionZ = cos(yaw) * speed
    }

    override fun onMotion() {}

    override fun onMove(event: MoveEvent) {}
}
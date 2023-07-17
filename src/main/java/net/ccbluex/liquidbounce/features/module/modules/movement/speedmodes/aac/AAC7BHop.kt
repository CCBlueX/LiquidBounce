/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.speed
import net.ccbluex.liquidbounce.utils.extensions.toRadiansD
import kotlin.math.cos
import kotlin.math.sin

object AAC7BHop : SpeedMode("AAC7BHop") {
    override fun onUpdate() {
        val thePlayer = mc.thePlayer ?: return

        if (!isMoving || thePlayer.ridingEntity != null || thePlayer.hurtTime > 0)
            return

        if (thePlayer.onGround) {
            thePlayer.jump()
            thePlayer.motionY = 0.405
            thePlayer.motionX *= 1.004
            thePlayer.motionZ *= 1.004
            return
        }

        val speed = speed * 1.0072
        val yaw = thePlayer.rotationYaw.toRadiansD()

        thePlayer.motionX = -sin(yaw) * speed
        thePlayer.motionZ = cos(yaw) * speed
    }


}
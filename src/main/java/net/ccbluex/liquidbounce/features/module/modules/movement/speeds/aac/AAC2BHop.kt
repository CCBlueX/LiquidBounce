/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.aac

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils

class AAC2BHop : SpeedMode("AAC2BHop") {

    override fun onMotion() {
        val thePlayer = mc.thePlayer!!

        if (thePlayer.isInWater)
            return

        if (MovementUtils.isMoving) {
            if (thePlayer.onGround) {
                thePlayer.jump()
                thePlayer.motionX *= 1.02
                thePlayer.motionZ *= 1.02
            } else if (thePlayer.motionY > -0.2) {
                thePlayer.jumpMovementFactor = 0.08f
                thePlayer.motionY += 0.0143099999999999999999999999999
                thePlayer.jumpMovementFactor = 0.07f
            }
        } else {
            thePlayer.motionX = 0.0
            thePlayer.motionZ = 0.0
        }
    }

    override fun onUpdate() {}
    override fun onMove(event: MoveEvent) {}
}
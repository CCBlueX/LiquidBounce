/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.aac

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils

class OldAACBHop : SpeedMode("OldAACBHop") {
    override fun onMotion() {
        val thePlayer = mc.thePlayer ?: return

        if (MovementUtils.isMoving) {
            if (thePlayer.onGround) {
                MovementUtils.strafe(0.56f)
                thePlayer.motionY = 0.41999998688697815
            } else MovementUtils.strafe(MovementUtils.speed * if (thePlayer.fallDistance > 0.4f) 1.0f else 1.01f)
        } else {
            thePlayer.motionX = 0.0
            thePlayer.motionZ = 0.0
        }
    }

    override fun onUpdate() {}
    override fun onMove(event: MoveEvent) {}
}
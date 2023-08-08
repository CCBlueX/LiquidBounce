/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.speed

object MineBlazeHop : SpeedMode("MineBlazeHop") {
           
          override fun onUpdate() {
             if (mc.thePlayer.motionY > 0.003) {
                mc.thePlayer.motionX *= 1.0015
                mc.thePlayer.motionZ *= 1.0015
                mc.timer.timerSpeed = 1.06f
             }
        }
       
    }

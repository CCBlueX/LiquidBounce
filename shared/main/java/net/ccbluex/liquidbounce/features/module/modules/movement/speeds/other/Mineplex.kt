/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.other

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils

class Mineplex : SpeedMode("Mineplex") {
    
    val speed1 = 0
    val speed2 = 0
    val wfg = false
    
    override fun onUpdate() {
        val x = mc.thePlayer!!.posX - mc.thePlayer!!.prevPosX
        val z = mc.thePlayer!!.posZ - mc.thePlayer!!.prevPosZ
        val distance = Math.hypot(x, z)
    
        if (MovementUtils.isMoving && mc.thePlayer!!.onGround) {
           mc.thePlayer!!.motionY = 0.4052393
           wfg = true
           speed2 = speed1
           speed1 = 0
        } else {
            if(wfg) {
              speed1 = speed2 + 0.46532
              wfg = false
            } else {
              speed1 = (distance * 0.946)
            }
        }
        val max = 5
        MovementUtils.strafe(Math.max(Math.min(speed1, max), wfg ? 0 : 0.399900111))
    }
    
    override fun onMotion() {}
    override fun onMove(event: MoveEvent) {}
    override fun onDisable() {
        speed1 = 0
        speed2 = 0
        wfg = false
    }
}

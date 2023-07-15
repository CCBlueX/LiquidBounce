/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

object Mineplex : SpeedMode("Mineplex") {

    private var speed1 = 0f
    private var speed2 = 0f
    private var wfg = false
    private var fallDistance = 0f

    override fun onUpdate() {
        val x = mc.thePlayer.posX - mc.thePlayer.prevPosX
        val z = mc.thePlayer.posZ - mc.thePlayer.prevPosZ
        val distance = hypot(x, z)
        if (isMoving && mc.thePlayer.onGround) {
            mc.thePlayer.motionY = 0.4052393
            wfg = true
            speed2 = speed1
            speed1 = 0f
        } else {
            if(wfg) {
                speed1 = (speed2 + (0.46532f * min(fallDistance, 1f)))
                wfg = false
            } else speed1 = ((distance * 0.936f).toFloat())
            fallDistance = mc.thePlayer.fallDistance
        }
        val minimum = if (!wfg) 0.3999001f else 0f
        strafe(max(min(speed1, 2f), minimum))
    }

    override fun onMotion() {
    }

    override fun onMove(event: MoveEvent) {
    }

    override fun onDisable() {
        speed1 = 0f
        speed2 = 0f
        wfg = false
        fallDistance = 0f
    }
}

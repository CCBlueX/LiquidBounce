/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.aac

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils

class AACHop350 : SpeedMode("AACHop3.5.0"), Listenable {
    override fun onMotion() {}
    override fun onUpdate() {}
    override fun onMove(event: MoveEvent) {}

    @EventTarget
    fun onMotion(event: MotionEvent) {
        val thePlayer = mc.thePlayer ?: return

        if (event.eventState == EventState.POST && MovementUtils.isMoving && !thePlayer.isInWater && !thePlayer.isInLava) {
            thePlayer.jumpMovementFactor += 0.00208f
            if (thePlayer.fallDistance <= 1f) {
                if (thePlayer.onGround) {
                    thePlayer.jump()
                    thePlayer.motionX *= 1.0118f
                    thePlayer.motionZ *= 1.0118f
                } else {
                    thePlayer.motionY -= 0.0147f
                    thePlayer.motionX *= 1.00138f
                    thePlayer.motionZ *= 1.00138f
                }
            }
        }
    }

    override fun onEnable() {
        val thePlayer = mc.thePlayer ?: return

        if (thePlayer.onGround) {
            thePlayer.motionZ = 0.0
            thePlayer.motionX = thePlayer.motionZ
        }
    }

    override fun onDisable() {
        mc.thePlayer?.jumpMovementFactor = 0.02f
    }

    override fun handleEvents(): Boolean {
        return isActive
    }

    init {
        LiquidBounce.eventManager.registerListener(this)
    }
}
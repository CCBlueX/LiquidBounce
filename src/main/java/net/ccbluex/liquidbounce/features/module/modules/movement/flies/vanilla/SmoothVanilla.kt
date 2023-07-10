/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flies.vanilla

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode

class SmoothVanilla : FlyMode("SmoothVanilla") {
    override fun onMotion() {}

    override fun onEnable() {}

    override fun onDisable() {
        super.onDisable()
    }

    override fun onUpdate() {
        val thePlayer = mc.thePlayer
        thePlayer.capabilities.isFlying = true
        // Soon adding this as actual speed, not strafe... strafe(Fly.vanillaSpeed)
        mc.timer.timerSpeed = Fly.vanillaTimer
        //Fly.handleVanillaKickBypass()
    }

    override fun onMove(event: MoveEvent) {}
}

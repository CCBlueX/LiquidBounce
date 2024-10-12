/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.aac

import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode

object LAAC : NoFallMode("LAAC") {
    private var jumped = false

    override fun onUpdate() {
        val thePlayer = mc.thePlayer

        if (thePlayer.onGround) jumped = false

        if (thePlayer.motionY > 0) jumped = true

        if (!jumped && thePlayer.onGround && !thePlayer.isOnLadder && !thePlayer.isInWater && !thePlayer.isInWeb)
            thePlayer.motionY = -6.0
    }

    override fun onJump(event: JumpEvent) {
        jumped = true
    }

    override fun onMove(event: MoveEvent) {
        val thePlayer = mc.thePlayer

        if (!jumped && !thePlayer.onGround && !thePlayer.isOnLadder && !thePlayer.isInWater && !thePlayer.isInWeb && thePlayer.motionY < 0.0)
            event.zeroXZ()
    }
}
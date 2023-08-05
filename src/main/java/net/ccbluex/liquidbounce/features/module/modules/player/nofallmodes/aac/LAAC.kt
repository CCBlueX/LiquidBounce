package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.aac

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode

object LAAC : NoFallMode("LAAC") {
    private var jumped = false
    private val thePlayer = mc.thePlayer
    override fun onUpdate() {
        if (thePlayer.onGround) jumped = false

        if (thePlayer.motionY > 0) jumped = true

        if (!jumped && thePlayer.onGround && !thePlayer.isOnLadder && !thePlayer.isInWater && !thePlayer.isInWeb)
            thePlayer.motionY = -6.0
    }

    @EventTarget(ignoreCondition = true)
    override fun onJump(event: JumpEvent) {
        jumped = true
    }

    @EventTarget
    override fun onMove(event: MoveEvent) {
        if (!jumped && !thePlayer.onGround && !thePlayer.isOnLadder && !thePlayer.isInWater && !thePlayer.isInWeb && thePlayer.motionY < 0.0)
            event.zeroXZ()
    }
}
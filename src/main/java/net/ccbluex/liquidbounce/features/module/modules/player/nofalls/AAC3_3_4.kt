package net.ccbluex.liquidbounce.features.module.modules.player.nofalls

import net.ccbluex.liquidbounce.event.MoveEvent

class AAC3_3_4 : NoFallMode("AAC3.3.4")
{
    private var aacTicks = 0

    override fun onUpdate()
    {
        val thePlayer = mc.thePlayer ?: return
        if (!jumped && thePlayer.onGround && !thePlayer.isOnLadder && !thePlayer.isInWater && !thePlayer.isInWeb) thePlayer.motionY = -6.0
    }

    override fun onMove(event: MoveEvent)
    {
        val thePlayer = mc.thePlayer ?: return
        if (!jumped && !thePlayer.onGround && !thePlayer.isOnLadder && !thePlayer.isInWater && !thePlayer.isInWeb && thePlayer.motionY < 0.0) event.zeroXZ()
    }
}

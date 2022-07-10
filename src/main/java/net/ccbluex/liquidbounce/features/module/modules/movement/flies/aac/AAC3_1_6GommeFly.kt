package net.ccbluex.liquidbounce.features.module.modules.movement.flies.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode
import net.ccbluex.liquidbounce.utils.extensions.sendPacketWithoutEvent
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition

class AAC3_1_6GommeFly : FlyMode("AAC3.1.6-Gomme")
{
    private var metVoid = false
    private var ticks = 0

    override fun onUpdate()
    {
        val thePlayer = mc.thePlayer ?: return

        thePlayer.capabilities.isFlying = true

        if (ticks == 2) thePlayer.motionY += 0.05
        else if (ticks > 2)
        {
            thePlayer.motionY -= 0.05
            ticks = 0
        }

        ticks++

        val posY = thePlayer.posY
        if (!metVoid) mc.netHandler.networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(thePlayer.posX, posY, thePlayer.posZ, thePlayer.onGround))
        if (posY <= 0.0) metVoid = true
    }

    override fun onDisable()
    {
        metVoid = false
    }
}

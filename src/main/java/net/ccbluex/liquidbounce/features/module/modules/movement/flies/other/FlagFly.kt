package net.ccbluex.liquidbounce.features.module.modules.movement.flies.other

import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.extensions.sendPacketWithoutEvent
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook

class FlagFly : FlyMode("Flag")
{
    override fun onEnable()
    {
        if (mc.isIntegratedServerRunning)
        {
            ClientUtils.displayChatMessage(mc.thePlayer, "\u00A7c\u00A7lError: \u00A7aYou can't enable \u00A7c\u00A7l'Flag Fly' \u00A7ain SinglePlayer.")
            Fly.state = false
            return
        }
    }

    override fun onUpdate()
    {
        val thePlayer = mc.thePlayer ?: return
        val networkManager = mc.netHandler.networkManager
        val gameSettings = mc.gameSettings

        val x = thePlayer.posX
        val y = thePlayer.posY
        val z = thePlayer.posZ

        val yaw = thePlayer.rotationYaw
        val pitch = thePlayer.rotationPitch

        networkManager.sendPacketWithoutEvent(C06PacketPlayerPosLook(x + thePlayer.motionX * 999, y + (if (gameSettings.keyBindJump.isKeyDown) 1.5624 else 0.00000001) - if (gameSettings.keyBindSneak.isKeyDown) 0.0624 else 0.00000002, z + thePlayer.motionZ * 999, yaw, pitch, true))
        networkManager.sendPacketWithoutEvent(C06PacketPlayerPosLook(x + thePlayer.motionX * 999, y - 6969, z + thePlayer.motionZ * 999, yaw, pitch, true))

        thePlayer.setPosition(x + thePlayer.motionX * 11, y, z + thePlayer.motionZ * 11)
        thePlayer.motionY = 0.0
    }
}

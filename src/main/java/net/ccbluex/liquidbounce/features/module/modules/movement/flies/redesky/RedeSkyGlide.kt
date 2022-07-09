package net.ccbluex.liquidbounce.features.module.modules.movement.flies.redesky

import net.ccbluex.liquidbounce.api.minecraft.client.entity.Entity
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode
import net.ccbluex.liquidbounce.utils.extensions.forward
import net.ccbluex.liquidbounce.utils.extensions.strafe
import net.ccbluex.liquidbounce.utils.extensions.zeroXZ

class RedeSkyGlide : FlyMode("RedeSky-Glide")
{
    override fun onEnable()
    {
        val thePlayer = mc.thePlayer ?: return

        if (thePlayer.onGround) vclip(thePlayer, Fly.redeskyVClipHeight.get())
    }

    override fun onUpdate()
    {
        val thePlayer = mc.thePlayer ?: return

        mc.timer.timerSpeed = 0.3f

        packetHClip(thePlayer, 7.0)
        packetVClip(thePlayer, 10.0)

        vclip(thePlayer, -0.5f)
        thePlayer.forward(2.0)

        thePlayer.strafe(1F)

        thePlayer.motionY = -0.01
    }

    override fun onDisable()
    {
        val thePlayer = mc.thePlayer ?: return

        thePlayer.zeroXZ()
        packetHClip(thePlayer, 0.0)
    }

    private fun packetHClip(thePlayer: Entity, horizontal: Double)
    {
        val func = functions

        val playerYaw = thePlayer.rotationYaw.toRadians

        mc.netHandler.networkManager.sendPacketWithoutEvent(CPacketPlayerPosition(thePlayer.posX + horizontal * -func.sin(playerYaw), thePlayer.posY, thePlayer.posZ + horizontal * func.cos(playerYaw), false))
    }

    private fun vclip(thePlayer: Entity, vertical: Float)
    {
        thePlayer.setPosition(thePlayer.posX, thePlayer.posY + vertical, thePlayer.posZ)
    }

    private fun packetVClip(thePlayer: Entity, vertical: Double)
    {
        mc.netHandler.networkManager.sendPacketWithoutEvent(CPacketPlayerPosition(thePlayer.posX, thePlayer.posY + vertical, thePlayer.posZ, false))
    }
}

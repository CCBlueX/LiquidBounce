package net.ccbluex.liquidbounce.features.module.modules.movement.flies.redesky

import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode
import net.ccbluex.liquidbounce.utils.extensions.*
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition

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

    private fun packetHClip(thePlayer: EntityLivingBase, horizontal: Double)
    {
        val (x, z) = thePlayer.getForwardAmount(horizontal, thePlayer.rotationYaw)
        mc.netHandler.networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(x, thePlayer.posY, z, false))
    }

    private fun vclip(thePlayer: Entity, vertical: Float)
    {
        thePlayer.setPosition(thePlayer.posX, thePlayer.posY + vertical, thePlayer.posZ)
    }

    private fun packetVClip(thePlayer: Entity, vertical: Double)
    {
        mc.netHandler.networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY + vertical, thePlayer.posZ, false))
    }
}

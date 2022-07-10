package net.ccbluex.liquidbounce.features.module.modules.movement.flies.rewinside

import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode
import net.ccbluex.liquidbounce.utils.extensions.cos
import net.ccbluex.liquidbounce.utils.extensions.sendPacketWithoutEvent
import net.ccbluex.liquidbounce.utils.extensions.sin
import net.ccbluex.liquidbounce.utils.extensions.toRadians
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.util.Vec3

class TeleportRewinsideFly : FlyMode("TeleportRewinside")
{
    override fun onUpdate()
    {
        val thePlayer = mc.thePlayer ?: return
        val networkManager = mc.netHandler.networkManager

        val posY = thePlayer.posY

        val vectorStart = Vec3(thePlayer.posX, posY, thePlayer.posZ)

        val yaw = -thePlayer.rotationYaw
        val pitch = -thePlayer.rotationPitch
        val distance = 9.9 // TODO: Adjustable distance

        val yawRadians = yaw.toRadians
        val pitchRadians = pitch.toRadians
        val pitchCos = pitchRadians.cos

        val vectorEnd = Vec3(yawRadians.sin * pitchCos * distance + vectorStart.xCoord, pitchRadians.sin * distance + vectorStart.yCoord, yawRadians.cos * pitchCos * distance + vectorStart.zCoord)

        networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(vectorEnd.xCoord, posY + 2, vectorEnd.zCoord, true))
        networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(vectorStart.xCoord, posY + 2, vectorStart.zCoord, true))

        thePlayer.motionY = 0.0
    }
}

package net.ccbluex.liquidbounce.features.module.modules.movement.flies.rewinside

import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode

class TeleportRewinsideFly : FlyMode("TeleportRewinside")
{
    override fun onUpdate()
    {
        val thePlayer = mc.thePlayer ?: return
        val provider = classProvider
        val func = functions
        val networkManager = mc.netHandler.networkManager

        val posY = thePlayer.posY

        val vectorStart = WVec3(thePlayer.posX, posY, thePlayer.posZ)

        val yaw = -thePlayer.rotationYaw
        val pitch = -thePlayer.rotationPitch
        val distance = 9.9 // TODO: Adjustable distance

        val yawRadians = WMathHelper.toRadians(yaw)

        val pitchRadians = WMathHelper.toRadians(pitch)
        val pitchCos = func.cos(pitchRadians)

        val vectorEnd = WVec3(func.sin(yawRadians) * pitchCos * distance + vectorStart.xCoord, func.sin(pitchRadians) * distance + vectorStart.yCoord, func.cos(yawRadians) * pitchCos * distance + vectorStart.zCoord)

        networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(vectorEnd.xCoord, posY + 2, vectorEnd.zCoord, true))
        networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(vectorStart.xCoord, posY + 2, vectorStart.zCoord, true))

        thePlayer.motionY = 0.0
    }
}

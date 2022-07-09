package net.ccbluex.liquidbounce.features.module.modules.movement.flies.rewinside

import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper
import net.ccbluex.liquidbounce.api.minecraft.util.Vec3
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

        val vectorStart = Vec3(thePlayer.posX, posY, thePlayer.posZ)

        val yaw = -thePlayer.rotationYaw
        val pitch = -thePlayer.rotationPitch
        val distance = 9.9 // TODO: Adjustable distance

        val yawRadians = yaw.toRadians

        val pitchRadians = pitch.toRadians
        val pitchCos = func.cos(pitchRadians)

        val vectorEnd = Vec3(func.sin(yawRadians) * pitchCos * distance + vectorStart.xCoord, func.sin(pitchRadians) * distance + vectorStart.yCoord, func.cos(yawRadians) * pitchCos * distance + vectorStart.zCoord)

        networkManager.sendPacketWithoutEvent(CPacketPlayerPosition(vectorEnd.xCoord, posY + 2, vectorEnd.zCoord, true))
        networkManager.sendPacketWithoutEvent(CPacketPlayerPosition(vectorStart.xCoord, posY + 2, vectorStart.zCoord, true))

        thePlayer.motionY = 0.0
    }
}

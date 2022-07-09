package net.ccbluex.liquidbounce.features.module.modules.movement.flies.other

import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode
import net.ccbluex.liquidbounce.utils.extensions.strafe
import net.ccbluex.liquidbounce.utils.extensions.zeroXYZ

class KeepAliveFly : FlyMode("KeepAlive")
{
    override val mark: Boolean
        get() = false

    override fun onUpdate()
    {
        val thePlayer = mc.thePlayer ?: return
        val gameSettings = mc.gameSettings

        mc.netHandler.networkManager.sendPacketWithoutEvent(classProvider.createCPacketKeepAlive())

        thePlayer.capabilities.isFlying = false

        thePlayer.zeroXYZ()

        val speed = Fly.baseSpeedValue.get()

        if (gameSettings.keyBindJump.isKeyDown) thePlayer.motionY += speed
        if (gameSettings.keyBindSneak.isKeyDown) thePlayer.motionY -= speed

        thePlayer.strafe(speed)
    }
}

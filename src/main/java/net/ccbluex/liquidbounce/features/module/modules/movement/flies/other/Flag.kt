/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flies.other

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook

class Flag : FlyMode("Flag") {
    override fun onMotion() {}

    override fun onEnable() {}

    override fun onDisable() {
        super.onDisable()
    }

    override fun onUpdate() {
        val thePlayer = mc.thePlayer

        sendPackets(
            C06PacketPlayerPosLook(
                thePlayer.posX + thePlayer.motionX * 999,
                thePlayer.posY + (if (mc.gameSettings.keyBindJump.isKeyDown) 1.5624 else 0.00000001) - if (mc.gameSettings.keyBindSneak.isKeyDown) 0.0624 else 0.00000002,
                thePlayer.posZ + thePlayer.motionZ * 999,
                thePlayer.rotationYaw,
                thePlayer.rotationPitch,
                true
            ),
            C06PacketPlayerPosLook(
                thePlayer.posX + thePlayer.motionX * 999,
                thePlayer.posY - 6969,
                thePlayer.posZ + thePlayer.motionZ * 999,
                thePlayer.rotationYaw,
                thePlayer.rotationPitch,
                true
            )
        )
        thePlayer.setPosition(
            thePlayer.posX + thePlayer.motionX * 11, thePlayer.posY, thePlayer.posZ + thePlayer.motionZ * 11
        )
        thePlayer.motionY = 0.0
    }

    override fun onMove(event: MoveEvent) {}
}

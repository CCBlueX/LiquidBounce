/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flies.ncp

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.client.C03PacketPlayer
import org.lwjgl.input.Keyboard

class OldNCP : FlyMode("OldNCP") {
    override fun onMotion() {}

    override fun onEnable() {
        val thePlayer = mc.thePlayer
        val x = thePlayer.posX
        val y = thePlayer.posY
        val z = thePlayer.posZ

        if (!thePlayer.onGround) return
        repeat(4) {
            sendPackets(
                C04PacketPlayerPosition(x, y + 1.01, z, false),
                C04PacketPlayerPosition(x, y, z, false)
            )
        }

        thePlayer.jump()
        thePlayer.swingItem()
    
    }

    override fun onDisable() {
        super.onDisable()
    }

    override fun onUpdate() {
        val thePlayer = mc.thePlayer
      
        if (Fly.startY > thePlayer.posY) thePlayer.motionY = -0.000000000000000000000000000000001
        if (mc.gameSettings.keyBindSneak.isKeyDown) thePlayer.motionY = -0.2
        if (mc.gameSettings.keyBindJump.isKeyDown && thePlayer.posY < Fly.startY - 0.1) thePlayer.motionY = 0.2
        strafe()
    }

    override fun onMove(event: MoveEvent) {}
}

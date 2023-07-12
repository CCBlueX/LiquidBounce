/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flies.ncp

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.client.C03PacketPlayer
import org.lwjgl.input.Keyboard

class NCP : FlyMode("NCP") {
    override fun onMotion() {}

    override fun onEnable() {
        val thePlayer = mc.thePlayer
        val x = thePlayer.posX
        val y = thePlayer.posY
        val z = thePlayer.posZ

        if (!thePlayer.onGround) return

        repeat(65) {
            sendPackets(
                C04PacketPlayerPosition(x, y + 0.049, z, false),
                C04PacketPlayerPosition(x, y, z, false)
            )

            sendPacket(C04PacketPlayerPosition(x, y + 0.1, z, true))

            thePlayer.motionX *= 0.1
            thePlayer.motionZ *= 0.1
            thePlayer.swingItem()
        }
    }

    override fun onDisable() {
        super.onDisable()
    }

    override fun onUpdate() {
        mc.thePlayer.motionY = (-Fly.ncpMotion).toDouble()
        if (mc.gameSettings.keyBindSneak.isKeyDown) mc.thePlayer.motionY = -0.5
        strafe()
    }

    override fun onMove(event: MoveEvent) {}

    fun onPacket(event: PacketEvent) {
        if (Fly.noPacketModify) return

        if (event.packet is C03PacketPlayer) {
            event.packet.onGround = true
        }
    }
}

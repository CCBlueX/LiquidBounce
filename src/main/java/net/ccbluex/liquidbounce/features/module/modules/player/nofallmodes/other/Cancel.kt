package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.other

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.server.S08PacketPlayerPosLook

object Cancel : NoFallMode("Cancel") {

    private var isFalling = false

    /**
     * NoFall Cancel
     * NOTE: The recommended distance for falling is < 15.
     */

    override fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (event.isCancelled)
            return

        if (packet is S08PacketPlayerPosLook && isFalling) {
            sendPacket(C04PacketPlayerPosition(packet.x, packet.y, packet.z, true))
            isFalling = false
        }

        if (packet is C03PacketPlayer) {
            if (mc.thePlayer.fallDistance > 3F) {
                isFalling = true
                event.cancelEvent()
            }
        }
    }

    override fun onDisable() {
        mc.thePlayer.fallDistance = 0F
        isFalling = false
    }
}
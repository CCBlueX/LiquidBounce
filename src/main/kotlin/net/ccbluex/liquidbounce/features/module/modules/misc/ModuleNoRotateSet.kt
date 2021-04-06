package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket

object ModuleNoRotateSet : Module("NoRotateSet", Category.MISC) {

    val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is PlayerPositionLookS2CPacket) {

            packet.yaw = player.yaw
            packet.pitch = player.pitch
        }
    }
}

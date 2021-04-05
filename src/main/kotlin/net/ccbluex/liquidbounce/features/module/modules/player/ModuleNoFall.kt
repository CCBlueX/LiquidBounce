package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Choice
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.repeatable
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

object ModuleNoFall : Module("NoFall", Category.PLAYER) {

    private val modes = choices("Mode", "SpoofGround") {
        SpoofGround
        NoGround
        Packet
    }

    private object SpoofGround : Choice("SpoofGround", modes) {

        val packetHandler = handler<PacketEvent> {
            val packet = it.packet

            if (packet is PlayerMoveC2SPacket) {
                packet.onGround = true
            }

        }

    }

    private object NoGround : Choice("NoGround", modes) {

        val packetHandler = handler<PacketEvent> {
            val packet = it.packet

            if (packet is PlayerMoveC2SPacket) {
                packet.onGround = false
            }

        }

    }

    private object Packet : Choice("Packet", modes) {

        val repeatable = repeatable {
            if(player.fallDistance > 2f) {
                network.sendPacket(PlayerMoveC2SPacket(true))
            }
        }

    }


}

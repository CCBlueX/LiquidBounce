package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.event.packetHandler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

object ModuleNoFall : Module("NoFall", Category.PLAYER) {

    private val modes = choices("Mode", "SpoofGround") {
        SpoofGround
        NoGround
        Packet
    }

    private object SpoofGround : Choice("SpoofGround", modes) {
        val packetHandler = packetHandler<PlayerMoveC2SPacket> { packet.onGround = true }


    }

    private object NoGround : Choice("NoGround", modes) {

        val packetHandler = packetHandler<PlayerMoveC2SPacket> { packet.onGround = false }


    }

    private object Packet : Choice("Packet", modes) {

        val repeatable = repeatable {
            if (player.fallDistance > 2f) {
                network.sendPacket(PlayerMoveC2SPacket(true))
            }
        }

    }

}

package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.extensions.RotationManager
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket

object ModuleNoRotateSet : Module("NoRotateSet", Category.MISC) {

    private val confirm by boolean("Confirm", true)
    private val illegalRotation by boolean("ConfirmIllegalRotation", false)
    private val noZero by boolean("NoZero", false)

    val packetHandler = handler<PacketEvent> { event ->

        val packet = event.packet

        if (packet is PlayerPositionLookS2CPacket) {
            if (noZero && packet.yaw == 0F && packet.pitch == 0F)
                return@handler

            if (confirm)
                network.sendPacket(PlayerMoveC2SPacket.LookOnly(packet.yaw, packet.pitch, player.isOnGround))

            packet.yaw = when {
                illegalRotation || RotationManager.serverRotation != null && packet.yaw != RotationManager.serverRotation!!.yaw -> packet.yaw
                else -> 0f // Maybe not 0f, just player.yaw
            }
            packet.pitch = when {
                illegalRotation || packet.pitch <= 90 && packet.pitch >= -90 && RotationManager.serverRotation != null && packet.pitch != RotationManager.serverRotation!!.pitch -> packet.pitch
                else -> 0f // Maybe not 0f, just player.pitch
            }
        }
    }
}

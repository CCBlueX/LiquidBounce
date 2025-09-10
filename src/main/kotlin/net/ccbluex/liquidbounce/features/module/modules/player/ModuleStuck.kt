package net.ccbluex.liquidbounce.features.module.modules.player


import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.client.sendPacketSilently
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket

object ModuleStuck : ClientModule("Stuck", Category.PLAYER, disableOnQuit = true) {

    @Suppress("unused")
    private val movementInputEventHandler = handler<MovementInputEvent> {
        player.movement.x = 0.0
        player.movement.y = 0.0
        player.movement.z = 0.0
        it.directionalInput = DirectionalInput(
            forwards = false,
            backwards = false,
            left = false,
            right = false
        )
    }

    @Suppress("unused")
    private val packetEventHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is PlayerPositionLookS2CPacket) {
            notification(
                "Stuck",
                "Auto disable for s08 packet.",
                NotificationEvent.Severity.INFO
            )
            enabled = false
        }

        if (packet is PlayerMoveC2SPacket) {
            event.cancelEvent()
        }

        if (packet is PlayerInteractItemC2SPacket) {
            event.cancelEvent()
            sendPacketSilently(
                PlayerMoveC2SPacket.LookAndOnGround(
                    player.yaw, player.pitch, player.isOnGround, player.horizontalCollision
                )
            )
            sendPacketSilently(
                PlayerInteractItemC2SPacket(
                    packet.hand, packet.sequence, player.yaw, player.pitch
                )
            )
        }

        if (packet is PlayerInteractEntityC2SPacket) {
            event.cancelEvent()
            sendPacketSilently(
                PlayerMoveC2SPacket.LookAndOnGround(
                    player.yaw, player.pitch, player.isOnGround, player.horizontalCollision
                )
            )
            sendPacketSilently(packet)
        }

        if (packet is PlayerInteractBlockC2SPacket) {
            event.cancelEvent()
            sendPacketSilently(
                PlayerMoveC2SPacket.LookAndOnGround(
                    player.yaw, player.pitch, player.isOnGround, player.horizontalCollision
                )
            )
            sendPacketSilently(packet)
        }
    }
}

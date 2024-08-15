package net.ccbluex.liquidbounce.features.module.modules.bmw

import net.ccbluex.liquidbounce.bmw.*
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.sendPacketSilently
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket

object ModuleStuck : Module("Stuck", Category.BMW) {

    private val autoReset by boolean("AutoReset", false)
    private val resetTicks by int("ResetTicks", 20, 1..200, "ticks")

    private var stuckTicks = 0
    private var isInAir = false

    val movementInputEventHandler = handler<MovementInputEvent> {
        player.movement.x = 0.0
        player.movement.y = 0.0
        player.movement.z = 0.0
    }

    val packetEventHandler = handler<PacketEvent> { event ->
        if (!player.isOnGround) {
            isInAir = true

            if (event.packet is PlayerPositionLookS2CPacket) {
                notifyAsMessage("Stuck End for PlayerPositionLookS2CPacket")
                this.enabled = false
            }

            if (event.packet is PlayerMoveC2SPacket) {
                event.cancelEvent()
            }

            if (event.packet is PlayerInteractItemC2SPacket) {
                event.cancelEvent()
                sendPacketSilently(PlayerMoveC2SPacket.LookAndOnGround(
                    player.yaw, player.pitch, player.isOnGround
                ))
                sendPacketSilently(PlayerInteractItemC2SPacket(
                    event.packet.hand, event.packet.sequence, player.yaw, player.pitch
                ))
            }
        } else if (isInAir) {
            notifyAsMessage("Stuck End for OnGround")
            this.enabled = false
        }
    }

    val gameTickEventHandler = handler<GameTickEvent> {
        if (!autoReset) {
            return@handler
        }

        stuckTicks++
        if (stuckTicks >= resetTicks) {
            notifyAsMessage("Stuck Reset ($stuckTicks ticks)")
            this.enabled = false
            this.enabled = true
        }
    }

    override fun enable() {
        stuckTicks = 0
        isInAir = false
    }

}

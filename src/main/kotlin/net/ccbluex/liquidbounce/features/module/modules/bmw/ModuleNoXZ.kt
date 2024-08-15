package net.ccbluex.liquidbounce.features.module.modules.bmw

import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.AttackEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.isOlderThanOrEqual1_8
import net.ccbluex.liquidbounce.utils.client.protocolVersion
import net.ccbluex.liquidbounce.utils.math.Vec2i
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket
import net.minecraft.util.UseAction

object ModuleNoXZ : Module("NoXZ", Category.BMW) {

    private val xzMultiple by float("XZMultiple", 0.5f, 0f..1f)
    private val attackTimes by int("AttackTimes", 5, 0..20, "times")

    private var velocityInput = false

    val repeatHandler = repeatable {
        if (protocolVersion.version > 47) {
            if (player.hurtTime == 0) {
                velocityInput = false
            }
        } else {
            if (player.hurtTime > 0 && player.isOnGround) {
                player.addVelocity(-1.3E-10, -1.3E-10, -1.3E-10)
                player.isSprinting = false
            }
        }
    }

    val attackEventHandler = handler<AttackEvent> { event ->
        if (velocityInput
            && event.enemy.isPlayer
            && player.isAlive
            && !player.isSpectator
            && !player.isInFluid
            && !player.isHoldingOntoLadder
            && !player.isOnFire
            && player.fallDistance <= 1.5
            && player.activeItem.useAction != UseAction.EAT
            && player.activeItem.useAction != UseAction.DRINK) {

            player.isSprinting = true
            repeat(attackTimes) {
                if (isOlderThanOrEqual1_8) {
                    player.swingHand(player.activeHand)
                    PlayerInteractEntityC2SPacket.attack(event.enemy, player.isSneaking)
                } else {
                    PlayerInteractEntityC2SPacket.attack(event.enemy, player.isSneaking)
                    player.swingHand(player.activeHand)
                }
            }
            player.movement.x *= xzMultiple
            player.movement.z *= xzMultiple
        }
    }

    val packetEventHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is EntityVelocityUpdateS2CPacket
            && packet.entityId == player.id
            && Vec2i(packet.velocityX, packet.velocityZ).length() > 1000) {

            velocityInput = true

        } else if (packet is ExplosionS2CPacket && protocolVersion.version >= 755) {
            event.cancelEvent()
        }
    }

    override fun enable() {
        velocityInput = false
    }

}

package net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.until
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket

internal object NoFallMatrix : Choice("Matrix-1.18+") {
    override val parent: ChoiceConfigurable<*>
        get() = ModuleNoFall.modes

    private var shouldSendLagPacket = false
    private var shouldHandleFall = false
    private var lagConfirmed = false

    override fun enable() {
        shouldSendLagPacket = false
        shouldHandleFall = false
        lagConfirmed = false
        super.enable()
    }
    override fun disable() {
        shouldSendLagPacket = false
        shouldHandleFall = false
        lagConfirmed = false
        super.enable()
    }
    @Suppress("unused")
    private val tickHandler = tickHandler {
        if (player.isOnGround || player.fallDistance < player.getAttributeValue(
                EntityAttributes.SAFE_FALL_DISTANCE).toFloat()){
            return@tickHandler
        }

        if (!shouldHandleFall && !player.isOnGround) {
            shouldHandleFall = true
            shouldSendLagPacket = false
            lagConfirmed = false
        }
        until<PlayerNetworkMovementTickEvent> { event ->
            if (!player.isOnGround || event.state != EventState.PRE) {
                return@until false
            }
            if (shouldHandleFall) {
                if (!shouldSendLagPacket && player.fallDistance < 3f) {
                    network.sendPacket(
                        PlayerMoveC2SPacket.PositionAndOnGround(
                            player.x - 1000.0,
                            player.y,
                            player.z,
                            false,
                            player.horizontalCollision
                        )
                    )
                    shouldSendLagPacket = true
                }
            }
            true
        }
        waitUntil {
            if (player.isOnGround && lagConfirmed) {
                player.jump()
                shouldHandleFall = false
                shouldSendLagPacket = false
                lagConfirmed = false
                true
            } else {
                player.isOnGround
            }
        }
    }

    @Suppress("unused")
    private val packetHandler = until<PacketEvent> { event ->
        when (val packet = event.packet) {
            is PlayerMoveC2SPacket ->
                if (shouldHandleFall && shouldSendLagPacket) {
                    event.cancelEvent()
                }
            is PlayerPositionLookS2CPacket ->
                if (shouldHandleFall && shouldSendLagPacket) {
                    lagConfirmed = true
                }
        }
        false
    }
}

/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall.modes
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.PacketQueueManager
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

/**
 * SpoofGround mode for the NoFall module.
 * This mode spoofs the 'onGround' flag in PlayerMoveC2SPacket to prevent fall damage.
 */
internal object NoFallBlink : Choice("Blink") {

    private const val PEEK_TICKS = 2

    /**
     * Trigger fall distance should not go beyond the possible fall distance of maximum ticks
     */
    private const val MAXIMUM_TICKS = 10

    private var triggerFallDistance by float("TriggerFallDistance", 2.5f, 0.5f..3f)
    private var maximumFallDistance by float("MaximumFallDistance", 20f, 2f..50f)

    private var blinkFall = false
    var waitUntilGround = true

    /**
     * Specifies the parent configuration for this mode
     */
    override val parent: ChoiceConfigurable<*>
        get() = modes

    private val inputHandler = handler<MovementInputEvent> { event ->
        // If we are invincible, we don't need to care about fall damage
        if (player.isCreative || player.abilities.allowFlying || player.abilities.flying) {
            blinkFall = false
            return@handler
        }

        // If we are not on-ground, we do some checks in-case something goes wrong
        if (!player.isOnGround) {
            if (waitUntilGround || player.fallDistance > maximumFallDistance) {
                if (blinkFall) {
                    PacketQueueManager.rewrite<PlayerMoveC2SPacket> { packet ->
                        packet.onGround = false
                    }

                    blinkFall = false
                }
            }

            return@handler

        } else {
            // If we are on ground, we might not want to blink anymore
            blinkFall = false
            waitUntilGround = false
        }

        // Check if we fall off in the next 2 ticks
        val simulatedPlayer = SimulatedPlayer.fromClientPlayer(
            SimulatedPlayer.SimulatedPlayerInput(
                event.directionalInput,
                event.jump,
                player.isSprinting,
                true
            ))

        repeat(PEEK_TICKS) {
            simulatedPlayer.tick()
        }

        if (simulatedPlayer.onGround) {
            return@handler
        }

        simulatedPlayer.tick()

        // Continue with clean input
        simulatedPlayer.input = SimulatedPlayer.SimulatedPlayerInput(
            DirectionalInput.NONE,
            jumping = false,
            sprinting = false,
            sneaking = false
        )

        // Check if we collect fall distance above 2f in the next 10 ticks
        for (ignored in 0..MAXIMUM_TICKS) {
            simulatedPlayer.tick()

            if (simulatedPlayer.fallDistance > triggerFallDistance) {
                notification("NoFall", "Detected possible fall damage, blinking...",
                    NotificationEvent.Severity.INFO)
                blinkFall = true

                ModuleDebug.debugGeometry(ModuleNoFall, "Ground", ModuleDebug.DebuggedPoint(player.pos,
                    Color4b(0, 0, 255, 255), size = 0.2))
                break
            }
        }
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is PlayerMoveC2SPacket && blinkFall) {
            packet.onGround = true
        }
    }

    @Suppress("unused")
    private val fakeLagHandler = handler<QueuePacketEvent> { event ->
        if (event.origin == TransferOrigin.OUTGOING && blinkFall) {
            event.action = PacketQueueManager.Action.QUEUE
        }
    }

    override fun disable() {
        blinkFall = false
        super.disable()
    }

}

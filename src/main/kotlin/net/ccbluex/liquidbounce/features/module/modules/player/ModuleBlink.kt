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
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.movement.autododge.ModuleAutoDodge
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink.dummyPlayer
import net.ccbluex.liquidbounce.utils.client.PacketQueueManager
import net.ccbluex.liquidbounce.utils.client.PacketQueueManager.Action
import net.ccbluex.liquidbounce.utils.client.PacketQueueManager.positions
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.minecraft.client.network.OtherClientPlayerEntity
import net.minecraft.entity.Entity
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import java.util.*

/**
 * Blink module
 *
 * Makes it look as if you were teleporting to other players.
 */

object ModuleBlink : ClientModule("Blink", Category.PLAYER) {

    private val dummy by boolean("Dummy", false)
    private val ambush by boolean("Ambush", false)
    private val autoDisable by boolean("AutoDisable", true)

    private object AutoResetOption : ToggleableConfigurable(this, "AutoReset", false) {
        val resetAfter by int("ResetAfter", 100, 1..1000)
        val action by enumChoice("ResetAction", ResetAction.RESET)
    }

    private var dummyPlayer: OtherClientPlayerEntity? = null

    init {
        tree(AutoResetOption)
    }

    override fun onEnabled() {
        if (dummy) {
            val clone = OtherClientPlayerEntity(world, player.gameProfile)

            clone.headYaw = player.headYaw
            clone.copyPositionAndRotation(player)
            /**
             * A different UUID has to be set, to avoid [dummyPlayer] from being invisible to [player]
             * @see net.minecraft.world.entity.EntityIndex.add
             */
            clone.uuid = UUID.randomUUID()
            world.addEntity(clone)

            dummyPlayer = clone
        }
    }

    override fun onDisabled() {
        PacketQueueManager.flush { snapshot -> snapshot.origin == TransferOrigin.OUTGOING }
        removeClone()
    }

    private fun removeClone() {
        val clone = dummyPlayer ?: return

        world.removeEntity(clone.id, Entity.RemovalReason.DISCARDED)
        dummyPlayer = null
    }

    val packetHandler = handler<PacketEvent>(priority = EventPriorityConvention.MODEL_STATE) { event ->
        val packet = event.packet

        if (event.isCancelled || event.origin != TransferOrigin.OUTGOING) {
            return@handler
        }

        if (ambush && packet is PlayerInteractEntityC2SPacket) {
            enabled = false
            return@handler
        }
    }

    @Suppress("unused")
    private val tickTask = tickHandler {
        if (ModuleAutoDodge.running) {
            val playerPosition = positions.firstOrNull() ?: return@tickHandler

            if (ModuleAutoDodge.getInflictedHit(playerPosition) == null) {
                return@tickHandler
            }

            val evadingPacket = ModuleAutoDodge.findAvoidingArrowPosition()

            // We have found no packet that avoids getting hit? Then we default to blinking.
            // AutoDoge might save the situation...
            if (evadingPacket == null) {
                notification(
                    "Blink", "Unable to evade arrow. Blinking.",
                    NotificationEvent.Severity.INFO
                )
                enabled = false
            } else if (evadingPacket.ticksToImpact != null) {
                notification("Blink", "Trying to evade arrow...", NotificationEvent.Severity.INFO)
                PacketQueueManager.flush(evadingPacket.idx + 1)
            } else {
                notification("Blink", "Arrow evaded.", NotificationEvent.Severity.INFO)
                PacketQueueManager.flush(evadingPacket.idx + 1)
            }
        }
    }

    @Suppress("unused")
    private val playerMoveHandler = handler<PlayerMovementTickEvent> {
        if (AutoResetOption.enabled && positions.count() > AutoResetOption.resetAfter) {
            when (AutoResetOption.action) {
                ResetAction.RESET -> PacketQueueManager.cancel()
                ResetAction.BLINK -> {
                    PacketQueueManager.flush { snapshot -> snapshot.origin == TransferOrigin.OUTGOING }
                    dummyPlayer?.copyPositionAndRotation(player)
                }
            }

            notification("Blink", "Auto reset", NotificationEvent.Severity.INFO)
            if (autoDisable) {
                enabled = false
            }
        }
    }

    @Suppress("unused")
    private val fakeLagHandler = handler<QueuePacketEvent> { event ->
        if (event.origin == TransferOrigin.OUTGOING) {
            event.action = Action.QUEUE
        }
    }

    enum class ResetAction(override val choiceName: String) : NamedChoice {
        RESET("Reset"),
        BLINK("Blink");
    }
}

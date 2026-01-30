/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.BlinkPacketEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerMovementTickEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.blink.BlinkManager
import net.ccbluex.liquidbounce.features.blink.BlinkManager.Action
import net.ccbluex.liquidbounce.features.blink.BlinkManager.positions
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.movement.autododge.ModuleAutoDodge
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink.dummyPlayer
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.minecraft.client.player.RemotePlayer
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.entity.EntityLookup
import java.util.UUID

/**
 * Blink module
 *
 * Makes it look as if you were teleporting to other players.
 */

object ModuleBlink : ClientModule("Blink", ModuleCategories.PLAYER) {

    private val dummy by boolean("Dummy", false)
    private val ambush by boolean("Ambush", false)
    private val autoDisable by boolean("AutoDisable", true)

    private object AutoResetOption : ToggleableValueGroup(this, "AutoReset", false) {
        val resetAfter by int("ResetAfter", 100, 1..1000)
        val action by enumChoice("ResetAction", ResetAction.RESET)
    }

    private var dummyPlayer: RemotePlayer? = null

    init {
        tree(AutoResetOption)
    }

    override fun onEnabled() {
        if (dummy) {
            val clone = RemotePlayer(world, player.gameProfile)

            clone.yHeadRot = player.yHeadRot
            clone.copyPosition(player)
            /**
             * A different UUID has to be set, to avoid [dummyPlayer] from being invisible to [player]
             * @see EntityLookup.add
             */
            clone.setUUID(UUID.randomUUID())
            world.addEntity(clone)

            dummyPlayer = clone
        }
    }

    override fun onDisabled() {
        BlinkManager.flush(TransferOrigin.OUTGOING)
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

        if (ambush && packet is ServerboundInteractPacket) {
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
                BlinkManager.flush(evadingPacket.idx + 1)
            } else {
                notification("Blink", "Arrow evaded.", NotificationEvent.Severity.INFO)
                BlinkManager.flush(evadingPacket.idx + 1)
            }
        }
    }

    @Suppress("unused")
    private val playerMoveHandler = handler<PlayerMovementTickEvent> {
        if (AutoResetOption.enabled && positions.count() > AutoResetOption.resetAfter) {
            when (AutoResetOption.action) {
                ResetAction.RESET -> BlinkManager.cancel()
                ResetAction.BLINK -> {
                    BlinkManager.flush(TransferOrigin.OUTGOING)
                    dummyPlayer?.copyPosition(player)
                }
            }

            notification("Blink", "Auto reset", NotificationEvent.Severity.INFO)
            if (autoDisable) {
                enabled = false
            }
        }
    }

    @Suppress("unused")
    private val fakeLagHandler = handler<BlinkPacketEvent> { event ->
        if (event.origin == TransferOrigin.OUTGOING) {
            event.action = Action.QUEUE
        }
    }

    enum class ResetAction(override val tag: String) : Tagged {
        RESET("Reset"),
        BLINK("Blink");
    }
}

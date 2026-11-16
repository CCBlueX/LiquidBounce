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
package net.ccbluex.liquidbounce.utils.navigation.teleport

import net.ccbluex.agent.navigation.Listener
import net.ccbluex.agent.navigation.teleport.Owner
import net.ccbluex.agent.navigation.teleport.Teleports
import net.ccbluex.agent.navigation.teleport.Trip
import net.ccbluex.agent.vanilla.toAgent
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.BlockChangeEvent
import net.ccbluex.liquidbounce.event.events.ChunkDeltaUpdateEvent
import net.ccbluex.liquidbounce.event.events.DisconnectEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.navigation.LocalPlayerBody
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket

/**
 * Drives Minecraft Agent's [Teleports] for the local player: one batch of position packets per tick, and the vanilla
 * movement packets kept in line while the server has the player somewhere else.
 */
object TeleportManager : EventListener {

    private val body = LocalPlayerBody(this)
    val teleports = Teleports(body)

    /** The trips made with [trip], told about block changes. */
    private val trips = ArrayList<Trip>()

    /** A trip for [owner], which runs while it does, reporting its route to [listener]. */
    fun trip(owner: EventListener, steps: StepsValueGroup, listener: Listener = Listener.NONE) =
        Trip(owner(owner), body, teleports, steps.options(), listener).also(trips::add)

    /** The identity [owner] has towards [teleports]. */
    fun owner(owner: EventListener): Owner = owners.getOrPut(owner) { Owner { owner.running } }

    private val owners = HashMap<EventListener, Owner>()

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent>(priority = EventPriorityConvention.READ_FINAL_STATE) {
        if (mc.player != null) {
            teleports.tick()
        }
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent>(priority = EventPriorityConvention.OBJECTION_AGAINST_EVERYTHING) {
        val packet = it.packet
        if (it.origin == TransferOrigin.INCOMING) {
            if (packet is ClientboundPlayerPositionPacket) {
                teleports.setBack()
            }
        } else if (packet is ServerboundMovePlayerPacket && teleports.rewritesMovement) {
            val server = teleports.serverPosition ?: return@handler
            if (packet.hasPosition()) {
                packet.x = server.x
                packet.y = server.y
                packet.z = server.z
            }
            packet.onGround = teleports.serverOnGround
        }
    }

    @Suppress("unused")
    private val blockChangeHandler = handler<BlockChangeEvent> { event ->
        val pos = event.blockPos.toAgent()
        teleports.blockChanged(pos, event.newState.toAgent())
        trips.forEach { it.blockChanged(pos) }
    }

    @Suppress("unused")
    private val chunkDeltaHandler = handler<ChunkDeltaUpdateEvent> { event ->
        event.packet.runUpdates { pos, state ->
            val agentPos = pos.toAgent()
            teleports.blockChanged(agentPos, state.toAgent())
            trips.forEach { it.blockChanged(agentPos) }
        }
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        teleports.cancel()
    }

    @Suppress("unused")
    private val disconnectHandler = handler<DisconnectEvent> {
        teleports.cancel()
    }

}

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
package net.ccbluex.liquidbounce.utils.combat

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.events.WorldEntityRemoveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.ccbluex.liquidbounce.utils.network.isDeathProtection
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket
import net.minecraft.world.entity.EntityEvent
import net.minecraft.world.entity.player.Player
import java.util.UUID

/**
 * Counts the totems each player popped since they last died or came into view.
 */
object TotemPopTracker : EventListener {

    private val pops = Object2IntOpenHashMap<UUID>()

    /**
     * How many totems [player] popped.
     */
    fun pops(player: Player): Int = pops.getInt(player.uuid)

    // Runs first, so listeners that queue their own reaction to the packet read the new count
    @Suppress("unused")
    private val packetHandler = handler<PacketEvent>(priority = FIRST_PRIORITY) { event ->
        val packet = event.packet as? ClientboundEntityEventPacket ?: return@handler
        if (!packet.isDeathProtection && packet.eventId != EntityEvent.DEATH) {
            return@handler
        }

        mc.execute {
            val player = packet.getEntity(mc.level ?: return@execute) as? Player ?: return@execute
            if (packet.isDeathProtection) {
                pops.addTo(player.uuid, 1)
            } else {
                pops.removeInt(player.uuid)
            }
        }
    }

    @Suppress("unused")
    private val removeHandler = handler<WorldEntityRemoveEvent> { event ->
        (event.entity as? Player)?.let { pops.removeInt(it.uuid) }
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        pops.clear()
    }

}

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

package net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode

import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.QueuePacketEvent
import net.ccbluex.liquidbounce.event.events.TickPacketProcessEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.utils.client.PacketQueueManager
import net.ccbluex.liquidbounce.utils.client.PacketQueueManager.Action
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket

internal object VelocityLag : VelocityMode("Lag") {
    val lagtime by intRange("LagTime", 5..5, 1..20, "ticks")

    private var isLagging = false
    private var lagTicks = 0

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is ClientboundSetEntityMotionPacket && packet.id == player.id) {
            isLagging = true
            lagTicks = lagtime.random()
        }
    }

    @Suppress("unused")
    private val queuePacketHandler = handler<QueuePacketEvent> { event ->
        if (!isLagging || event.origin != TransferOrigin.INCOMING || event.packet is ClientboundKeepAlivePacket) {
            return@handler
        }
        event.action = Action.QUEUE
    }

    @Suppress("unused")
    private val tickHandler = tickHandler{
        if (isLagging) {
            lagTicks--
        }
    }

    @Suppress("unused")
    private val tickPacketProcessHandler = handler<TickPacketProcessEvent> {
        if (isLagging && lagTicks == 0) {
            isLagging = false
            lagTicks = 0
            PacketQueueManager.flush(TransferOrigin.INCOMING)
        }
    }


    override fun enable() {
        isLagging = false
        lagTicks = 0
        PacketQueueManager.flush(TransferOrigin.INCOMING)
    }

    override fun disable() {
        PacketQueueManager.flush(TransferOrigin.INCOMING)
    }
}

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

package net.ccbluex.liquidbounce.utils.network

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.DisconnectEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.isOlderThan26_3
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket

/**
 * 26.3 disconnects on a second position packet between two client tick ends,
 * so a tick end goes out in front of every extra one.
 */
internal object PositionPacketSeparator : EventListener {

    private var positionSinceTickEnd = false

    val packetHandler = handler<PacketEvent>(priority = EventPriorityConvention.READ_FINAL_STATE) { event ->
        if (event.origin != TransferOrigin.OUTGOING || event.isCancelled) {
            return@handler
        }

        when (event.packet) {
            is ServerboundClientTickEndPacket -> positionSinceTickEnd = false
            // By class, not hasPos: that flag can be flipped on a Rot, which still goes out as one
            is ServerboundMovePlayerPacket.Pos, is ServerboundMovePlayerPacket.PosRot -> {
                if (positionSinceTickEnd && !isOlderThan26_3) {
                    sendPacketSilently(ServerboundClientTickEndPacket.INSTANCE)
                }
                positionSinceTickEnd = true
            }
        }
    }

    @Suppress("unused")
    private val disconnectHandler = handler<DisconnectEvent> {
        positionSinceTickEnd = false
    }

}

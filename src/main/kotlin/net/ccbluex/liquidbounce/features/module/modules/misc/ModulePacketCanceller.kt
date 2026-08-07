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

package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.collection.Filter
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention

/**
 * PacketCanceller module
 *
 * Allows you to cancel selected packets.
 */

object ModulePacketCanceller : ClientModule("PacketCanceller", ModuleCategories.MISC) {

    private val filter by enumChoice("Filter", Filter.WHITELIST)
    private val clientPackets by c2sPackets("C2SPackets", sortedSetOf())
    private val serverPackets by s2cPackets("S2CPackets", sortedSetOf())

    val packetHandler = handler<PacketEvent>(priority = EventPriorityConvention.FINAL_DECISION) { event ->
        if (!running || event.isCancelled) return@handler

        val packetId = event.packet.type().id
        if (!filter(packetId, if (event.origin == TransferOrigin.INCOMING) serverPackets else clientPackets)) {
            return@handler
        }

        event.cancelEvent();
    }
}

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
package net.ccbluex.liquidbounce.features.spoofer

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.minecraft.network.Connection
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket.Action.ACCEPTED
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket.Action.DECLINED
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket.Action.DOWNLOADED
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket.Action.FAILED_DOWNLOAD
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED

/**
 * ResourcePack Spoof
 *
 * Prevents servers from forcing you to download their resource pack.
 */
object SpooferResourcePack : ToggleableValueGroup(name = "ResourceSpoofer", enabled = false) {

    /**
     * Answers [packet] with the sequence vanilla sends after a successful download, without
     * downloading anything.
     */
    fun spoof(connection: Connection, packet: ClientboundResourcePackPushPacket) {
        val id = packet.id
        connection.send(ServerboundResourcePackPacket(id, ACCEPTED))
        connection.send(ServerboundResourcePackPacket(id, DOWNLOADED))
        connection.send(ServerboundResourcePackPacket(id, SUCCESSFULLY_LOADED))
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is ServerboundResourcePackPacket && (packet.action == DECLINED ||
                packet.action == FAILED_DOWNLOAD)) {
            event.cancelEvent()
        }
    }

}

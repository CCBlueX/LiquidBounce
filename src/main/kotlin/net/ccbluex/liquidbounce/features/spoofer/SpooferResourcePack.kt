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
package net.ccbluex.liquidbounce.features.spoofer

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket.Status.*
import net.minecraft.network.packet.s2c.common.ResourcePackSendS2CPacket

/**
 * ResourcePack Spoof
 *
 * Prevents servers from forcing you to download their resource pack.
 */
object SpooferResourcePack : ToggleableConfigurable(name = "ResourceSpoofer", enabled = false) {

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet
        val network = mc.networkHandler ?: return@handler

        if (packet is ResourcePackSendS2CPacket) {
            val id = packet.id
            network.sendPacket(ResourcePackStatusC2SPacket(id, ACCEPTED))
            network.sendPacket(ResourcePackStatusC2SPacket(id, SUCCESSFULLY_LOADED))
            event.cancelEvent()
        } else if (packet is ResourcePackStatusC2SPacket && (packet.status == DECLINED ||
                packet.status == FAILED_DOWNLOAD)) {
            event.cancelEvent()
        }
    }

}

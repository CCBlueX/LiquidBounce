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
package net.ccbluex.liquidbounce.addon

import net.ccbluex.liquidbounce.utils.network.sendPacketSilently
import net.minecraft.network.protocol.Packet

/**
 * The connection to the server. To see or cancel traffic, handle
 * [net.ccbluex.liquidbounce.event.events.PacketEvent].
 */
object Network {

    /** Sends like the game does; every module sees it in [net.ccbluex.liquidbounce.event.events.PacketEvent]. */
    @JvmStatic
    fun send(packet: Packet<*>) = Game.connection.send(packet)

    /** Sends past the modules, so nothing cancels or rewrites it. */
    @JvmStatic
    fun sendSilently(packet: Packet<*>) = sendPacketSilently(packet)

    @JvmStatic
    fun sendChat(message: String) = Game.connection.sendChat(message)

    /** [command] without the leading slash. */
    @JvmStatic
    fun sendCommand(command: String) = Game.connection.sendCommand(command)

}

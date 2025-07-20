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
package net.ccbluex.liquidbounce.utils.network

import com.viaversion.viaversion.api.Via
import com.viaversion.viaversion.api.protocol.Protocol
import com.viaversion.viaversion.api.protocol.packet.PacketType
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.network.ClientCommonNetworkHandler

/**
 * A packet that is directly sent to the server over ViaVersion.
 *
 * This class can be implemented and sent over [sendPacket] to imitated behavior from older minecraft versions.
 */
interface LegacyPacket {

    /**
     * Should provide the class of the protocol from the version where the packet got removed to the version where it
     * was still present.
     */
    val protocol: Class<out Protocol<*, *, *, *>>

    /**
     * The type of the packet.
     */
    val packetType: PacketType

    /**
     * Writes the actual information to the [packetWrapper].
     */
    fun write(packetWrapper: PacketWrapper)

}

// TODO integrate into the packet logger
/**
 * Sends the [packet].
 *
 * Make sure to check if ViaFabricPlus is loaded before using this or constructing the packet.
 *
 * Keep in mind, the packet won't be caught by the packet event.
 *
 * @param onSuccess Gets executed when sending succeeds.
 * @param onFailure Gets executed when sending fails.
 */
inline fun ClientCommonNetworkHandler.sendPacket(
    packet: LegacyPacket,
    onSuccess: () -> Unit = {},
    onFailure: () -> Unit = {}
) {
    runCatching {
        val isViaFabricPlusLoaded = FabricLoader.getInstance().isModLoaded("viafabricplus")
        if (!isViaFabricPlusLoaded) {
            return
        }

        val viaConnection = Via.getManager().connectionManager.connections.firstOrNull() ?: return

        if (viaConnection.protocolInfo.pipeline.contains(packet.protocol)) {
            val clientStatus = PacketWrapper.create(packet.packetType, viaConnection)
            packet.write(clientStatus)

            runCatching {
                clientStatus.scheduleSendToServer(packet.protocol)
            }.onSuccess {
                onSuccess()
            }.onFailure {
                onFailure()
                it.printStackTrace()
            }
        }
    }
}

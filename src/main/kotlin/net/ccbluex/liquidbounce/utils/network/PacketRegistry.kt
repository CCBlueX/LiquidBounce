package net.ccbluex.liquidbounce.utils.network

import net.minecraft.network.NetworkSide
import net.minecraft.util.Identifier

/**
 * A registry for packet types, allowing registration of packet identifiers
 * for both clientbound and serverbound packets.
 * This is used to keep track of which packets are registered for each side of the network.
 *
 * Be aware that serverbound means packets sent from the client to the server,
 * and clientbound means packets sent from the server to the client.
 */
val packetRegistry = mutableMapOf<NetworkSide, MutableSet<Identifier>>()

/**
 * Registers a packet type for the given [networkSide] with the specified [id].
 *
 * @param networkSide The side of the network (clientbound or serverbound).
 * @param id The identifier of the packet type to register defined in [net.minecraft.network.packet.PacketType]
 */
fun register(networkSide: NetworkSide, id: Identifier) {
    val set = packetRegistry.getOrPut(networkSide) { mutableSetOf() }
    set.add(id)
}

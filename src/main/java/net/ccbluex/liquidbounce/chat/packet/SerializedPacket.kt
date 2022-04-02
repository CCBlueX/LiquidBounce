/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.chat.packet

import com.google.gson.annotations.SerializedName
import net.ccbluex.liquidbounce.chat.packet.packets.Packet

/**
 * Serialized packet
 *
 * @param packetName name of packet
 * @param packetContent content of packet
 */
data class SerializedPacket(
    @SerializedName("m")
    val packetName: String,

    @SerializedName("c")
    val packetContent: Packet?
)
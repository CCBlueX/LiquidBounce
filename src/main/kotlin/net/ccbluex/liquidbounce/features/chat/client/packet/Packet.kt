/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.chat.client.packet

import com.google.gson.annotations.SerializedName
import java.util.*

interface Packet

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

/**
 * A axochat user
 *
 * @param name of user
 * @param uuid of user
 */
data class User(
    @SerializedName("name")
    val name: String,
    @SerializedName("uuid")
    val uuid: UUID
)

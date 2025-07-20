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
 *
 *
 */
package net.ccbluex.liquidbounce.features.chat.packet

import com.google.gson.annotations.SerializedName
import java.util.*

/**
 * AXOCHAT PROTOCOL
 *
 * https://github.com/CCBlueX/axochat_server/blob/master/PROTOCOL.md
 *
 * The server receives Server Packets.
 */

/**
 * To log in via mojang, the client has to send a RequestMojangInfo packet.
 * The server will then send a MojangInfo to the client.
 * This packet has no body.
 */
class ServerRequestMojangInfoPacket : Packet

/**
 * After the client received a MojangInfo packet and authenticating itself with mojang,
 * it has to send a LoginMojang packet to the server.
 * After the server receives a LoginMojang packet, it will send Success if the login was successful.

 * @param name name needs to be associated with the uuid.
 * @param uuid uuid is not guaranteed to be hyphenated.
 * @param allowMessages If allow_messages is true, other clients may send private messages to this client.
 */
data class ServerLoginMojangPacket(

    @SerializedName("name")
    val name: String,

    @SerializedName("uuid")
    val uuid: UUID,

    @SerializedName("allow_messages")
    val allowMessages: Boolean

) : Packet

/**
 * To log in using a json web token, the client has to send a LoginJWT packet.
 * it will send Success if the login was successful.
 *
 * @param token can be retrieved by sending RequestJWT on an already authenticated connection.
 * @param allowMessages If allow_messages is true, other clients may send private messages to this client.
 */
data class ServerLoginJWTPacket(

    @SerializedName("token")
    val token: String,

    @SerializedName("allow_messages")
    val allowMessages: Boolean

) : Packet

/**
 * The content of this packet will be sent to every client as Message if it fits the validation scheme.
 *
 * @param content content of the message.
 */
data class ServerMessagePacket(

    @SerializedName("content")
    val content: String

) : Packet

/**
 * The content of this packet will be sent to the specified client as PrivateMessage if it fits the validation scheme.
 *
 * @param receiver receiver is an ID.
 * @param content content of the message.
 */
data class ServerPrivateMessagePacket(

    @SerializedName("receiver")
    val receiver: String,

    @SerializedName("content")
    val content: String

) : Packet

/**
 * A client can send this packet to ban other users from using this chat.
 *
 * @param user user is an ID.
 */
data class ServerBanUserPacket(

    @SerializedName("user")
    val user: String

) : Packet

/**
 * A client can send this packet to unban other users.
 *
 * @param user user is an ID.
 */
data class ServerUnbanUserPacket(

    @SerializedName("user")
    val user: String

) : Packet

/**
 * To log in using LoginJWT, a client needs to own a json web token.
 * This token can be retrieved by sending RequestJWT as an already authenticated client to the server.
 * The server will send a NewJWT packet to the client.
 *
 * This packet has no body.
 */
class ServerRequestJWTPacket : Packet

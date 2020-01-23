/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.chat.packet.packets

import com.google.gson.annotations.SerializedName
import java.util.*

/**
 * AXOCHAT PROTOCOL
 *
 * https://gitlab.com/frozo/axochat/blob/master/PROTOCOL.md
 *
 * Server Packets are received by the server.
 */

/**
 * To login via mojang, the client has to send a RequestMojangInfo packet.
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
 * To login using a json web token, the client has to send a LoginJWT packet.
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
 * @param content content of message.
 */
data class ServerMessagePacket(

        @SerializedName("content")
        val content: String

) : Packet

/**
 * The content of this packet will be sent to the specified client as PrivateMessage if it fits the validation scheme.
 *
 * @param receiver receiver is an Id.
 * @param content content of message.
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
 * @param user user is an Id.
 */
data class ServerBanUserPacket(

        @SerializedName("user")
        val user: String

) : Packet

/**
 * A client can send this packet to unban other users.
 *
 * @param user user is an Id.
 */
data class ServerUnbanUserPacket(

        @SerializedName("user")
        val user: String

) : Packet

/**
 * To login using LoginJWT, a client needs to own a json web token.
 * This token can be retrieved by sending RequestJWT as an already authenticated client to the server.
 * The server will send a NewJWT packet to the client.
 *
 * This packet has no body.
 */
class ServerRequestJWTPacket : Packet
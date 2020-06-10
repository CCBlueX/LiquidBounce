/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.chat.packet.packets

import com.google.gson.annotations.SerializedName
import net.ccbluex.liquidbounce.chat.User

/**
 * AXOCHAT PROTOCOL
 *
 * https://gitlab.com/frozo/axochat/blob/master/PROTOCOL.md
 *
 * Client Packets are received by the client.
 */

/**
 * After the client sent the server a RequestMojangInfo packet, the server will provide the client with a session_hash.
 * A session hash is synonymous with a server id in the context of authentication with Mojang.
 * The client has to send a LoginMojang packet to the server after authenticating itself with Mojang.
 *
 * @param sessionHash session_hash to authenticate with Mojang
 */
data class ClientMojangInfoPacket(

        @SerializedName("session_hash")
        val sessionHash: String

) : Packet

/**
 * After the client sent the server a RequestJWT packet, the server will provide the client with json web token.
 * This token can be used in the LoginJWT packet.
 *
 * @param token JWT token
 */
data class ClientNewJWTPacket(

        @SerializedName("token")
        val token: String

) : Packet

/**
 * This packet will be sent to every authenticated client,
 * if another client successfully sent a message to the server.
 *
 * @param id author_id is an Id.
 * @param user author_info is optional and described in detail in UserInfo.
 * @param content content is any message fitting the validation scheme of the server.
 */
data class ClientMessagePacket(

        @SerializedName("author_id")
        val id: String,

        @SerializedName("author_info")
        val user: User,

        @SerializedName("content")
        val content: String

) : Packet

/**
 * This packet will be sent to a authenticated client with allow_messages turned on,
 * if another client successfully sent a private message to the server with the id.
 *
 * @param id author_id is an Id.
 * @param user author_info is optional and described in detail in UserInfo.
 * @param content content is any message fitting the validation scheme of the server.
 */
data class ClientPrivateMessagePacket(

        @SerializedName("author_id")
        val id: String,

        @SerializedName("author_info")
        val user: User,

        @SerializedName("content")
        val content: String

) : Packet

/**
 * This packet is sent after either LoginMojang, LoginJWT, BanUser or UnbanUser were processed successfully.
 *
 * @param reason of success packet
 */
data class ClientSuccessPacket(

        @SerializedName("reason")
        val reason: String

) : Packet

/**
 * This packet may be sent at any time, but is usually a response to a failed action of the client.
 *
 * @param message Error message
 */
data class ClientErrorPacket(

        @SerializedName("message")
        val message: String

) : Packet
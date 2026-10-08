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
package net.ccbluex.liquidbounce.features.chat.packet

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import java.util.UUID

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
 * This packet does not have a body.
 */
class C2SRequestMojangInfoPacket : AxochatPacket.C2S

/**
 * After the client received a MojangInfo packet and authenticating itself with mojang,
 * it has to send a LoginMojang packet to the server.
 * After the server receives a LoginMojang packet, it will send Success if the login was successful.

 * @param name name needs to be associated with the uuid.
 * @param uuid uuid is not guaranteed to be hyphenated.
 * @param allowMessages If allow_messages is true, other clients may send private messages to this client.
 */
data class C2SLoginMojangPacket(

    @SerializedName("name")
    val name: String,

    @SerializedName("uuid")
    val uuid: UUID,

    @SerializedName("allow_messages")
    val allowMessages: Boolean

) : AxochatPacket.C2S

/**
 * To log in using a json web token, the client has to send a LoginJWT packet.
 * it will send Success if the login was successful.
 *
 * @param token can be retrieved by sending RequestJWT on an already authenticated connection.
 * @param allowMessages If allow_messages is true, other clients may send private messages to this client.
 */
data class C2SLoginJWTPacket(

    @SerializedName("token")
    val token: String,

    @SerializedName("allow_messages")
    val allowMessages: Boolean

) : AxochatPacket.C2S

/**
 * The content of this packet will be sent to every client as Message if it fits the validation scheme.
 *
 * @param content content of the message.
 */
data class C2SMessagePacket(

    @SerializedName("content")
    val content: String

) : AxochatPacket.C2S

/**
 * The content of this packet will be sent to the specified client as PrivateMessage if it fits the validation scheme.
 *
 * @param receiver receiver is an ID.
 * @param content content of the message.
 */
data class C2SPrivateMessagePacket(

    @SerializedName("receiver")
    val receiver: String,

    @SerializedName("content")
    val content: String

) : AxochatPacket.C2S

/**
 * A client can send this packet to ban other users from using this chat.
 *
 * @param user user is an ID.
 */
data class C2SBanUserPacket(

    @SerializedName("user")
    val user: String

) : AxochatPacket.C2S

/**
 * A client can send this packet to unban other users.
 *
 * @param user user is an ID.
 */
data class C2SUnbanUserPacket(

    @SerializedName("user")
    val user: String

) : AxochatPacket.C2S

/**
 * To log in using LoginJWT, a client needs to own a json web token.
 * This token can be retrieved by sending RequestJWT as an already authenticated client to the server.
 * The server will send a NewJWT packet to the client.
 *
 * This packet does not have a body.
 */
class C2SRequestJWTPacket : AxochatPacket.C2S

class C2SRequestUserCountPacket : AxochatPacket.C2S

data class C2SHelloPacket(
    @SerializedName("protocol")
    val protocol: Int,
) : AxochatPacket.C2S

data class C2SLoginAccountPacket(
    @SerializedName("token")
    val token: String,
    @SerializedName("allow_messages")
    val allowMessages: Boolean,
) : AxochatPacket.C2S

data class C2SSettingsPacket(
    @SerializedName("allow_messages")
    val allowMessages: Boolean? = null,
    @SerializedName("hide_server")
    val hideServer: Boolean? = null,
    @SerializedName("accept_friend_requests")
    val acceptFriendRequests: Boolean? = null,
    @SerializedName("server_chat")
    val serverChat: Boolean? = null,
) : AxochatPacket.C2S

data class C2SChatMessagePacket(
    @SerializedName("channel")
    val channel: String,
    @SerializedName("content")
    val content: String,
) : AxochatPacket.C2S

data class C2SFriendPacket(
    @SerializedName("action")
    val action: String,
    @SerializedName("user")
    val user: String,
) : AxochatPacket.C2S

data class C2SBlockPacket(
    @SerializedName("user")
    val user: String,
    @SerializedName("blocked")
    val blocked: Boolean,
) : AxochatPacket.C2S

data class C2SGroupPacket(
    @SerializedName("action")
    val action: String,
    @SerializedName("group")
    val group: String? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("user")
    val user: String? = null,
    @SerializedName("admin")
    val admin: Boolean? = null,
) : AxochatPacket.C2S

data class C2SPartyPacket(
    @SerializedName("action")
    val action: String,
    @SerializedName("user")
    val user: String? = null,
    @SerializedName("party")
    val party: String? = null,
    @SerializedName("admin")
    val admin: Boolean? = null,
    @SerializedName("locked")
    val locked: Boolean? = null,
    @SerializedName("muted")
    val muted: Boolean? = null,
    @SerializedName("enabled")
    val enabled: Boolean? = null,
) : AxochatPacket.C2S

data class LocationWorld(
    @SerializedName("dimension")
    val dimension: String,
    @SerializedName("seed")
    val seed: Long,
    @SerializedName("age")
    val age: Long?,
)

data class C2SLocationPacket(
    @SerializedName("server")
    val server: String?,
    @SerializedName("world")
    val world: LocationWorld?,
    @SerializedName("player")
    val player: PartyPlayer?,
) : AxochatPacket.C2S

data class C2SSightingsPacket(
    @SerializedName("entities")
    val entities: Collection<String>,
    @SerializedName("tab")
    val tab: Collection<String>,
) : AxochatPacket.C2S

data class C2SPartyStatePacket(
    @SerializedName("position")
    val position: PartyPosition? = null,
    @SerializedName("status")
    val status: JsonObject? = null,
    @SerializedName("inventory")
    val inventory: JsonObject? = null,
) : AxochatPacket.C2S

data class C2SReportPacket(
    @SerializedName("user")
    val user: String,
    @SerializedName("message")
    val message: Long?,
    @SerializedName("reason")
    val reason: String,
) : AxochatPacket.C2S

data class C2SPunishPacket(
    @SerializedName("user")
    val user: String?,
    @SerializedName("ip")
    val ip: String?,
    @SerializedName("kind")
    val kind: String,
    @SerializedName("duration")
    val duration: Long?,
    @SerializedName("reason")
    val reason: String,
    @SerializedName("include_ip")
    val includeIp: Boolean,
) : AxochatPacket.C2S

data class C2SPardonPacket(
    @SerializedName("user")
    val user: String?,
    @SerializedName("ip")
    val ip: String?,
) : AxochatPacket.C2S

data class C2SRequestPunishmentsPacket(
    @SerializedName("user")
    val user: String,
) : AxochatPacket.C2S

class C2SRequestReportsPacket : AxochatPacket.C2S

data class C2SResolveReportPacket(
    @SerializedName("id")
    val id: String,
) : AxochatPacket.C2S

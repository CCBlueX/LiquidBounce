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

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import java.util.UUID

/**
 * AXOCHAT PROTOCOL
 *
 * https://github.com/CCBlueX/axochat_server/blob/master/PROTOCOL.md
 *
 * The client receives client Packets.
 */

/**
 * After the client sent the server a RequestMojangInfo packet, the server will provide the client with a session_hash.
 * A session hash is synonymous with a server id in the context of authentication with Mojang.
 * The client has to send a LoginMojang packet to the server after authenticating itself with Mojang.
 *
 * @param sessionHash session_hash to authenticate with Mojang
 */
data class S2CMojangInfoPacket(

    @SerializedName("session_hash")
    val sessionHash: String

) : AxochatPacket.S2C

/**
 * This packet will be sent to every authenticated client
 * if another client successfully sent a message to the server.
 *
 * @param id author_id is an ID.
 * @param user author_info is optional and described in detail in UserInfo.
 * @param content content is any message fitting the validation scheme of the server.
 */
data class S2CMessagePacket(

    @SerializedName("author_id")
    val id: String,

    @SerializedName("author_info")
    val user: AxoUser,

    @SerializedName("content")
    val content: String

) : AxochatPacket.S2C

/**
 * This packet will be sent to an authenticated client with allow_messages turned on,
 * if another client successfully sent a private message to the server with the id.
 *
 * @param id author_id is an ID.
 * @param user author_info is optional and described in detail in UserInfo.
 * @param content content is any message fitting the validation scheme of the server.
 */
data class S2CPrivateMessagePacket(

    @SerializedName("author_id")
    val id: String,

    @SerializedName("author_info")
    val user: AxoUser,

    @SerializedName("content")
    val content: String

) : AxochatPacket.S2C

/**
 * This packet is sent after a login or an action was processed successfully.
 *
 * @param reason of success packet
 */
data class S2CSuccessPacket(

    @SerializedName("reason")
    val reason: String

) : AxochatPacket.S2C

/**
 * This packet may be sent at any time, but is usually a response to a failed action of the client.
 *
 * @param message error code; older servers sent `InvalidCharacter` as `{"InvalidCharacter": "x"}`
 * @param detail extra information about the error (v2)
 */
data class S2CErrorPacket(

    @SerializedName("message")
    val message: JsonElement?,

    @SerializedName("detail")
    val detail: String?

) : AxochatPacket.S2C {

    val code: String
        get() = when {
            message == null || message.isJsonNull -> "Internal"
            message.isJsonPrimitive -> message.asString
            message.isJsonObject -> message.asJsonObject.keySet().firstOrNull() ?: "Internal"
            else -> message.toString()
        }

    val details: String?
        get() = detail ?: message?.takeIf { it.isJsonObject }?.asJsonObject?.entrySet()?.firstOrNull()
            ?.value?.takeIf { it.isJsonPrimitive }?.asString

}

data class S2CHelloPacket(
    @SerializedName("protocol")
    val protocol: Int,
) : AxochatPacket.S2C

data class S2CWelcomePacket(
    @SerializedName("user")
    val user: ChatAuthor,
    @SerializedName("staff")
    val staff: Boolean,
) : AxochatPacket.S2C

data class S2CSettingsPacket(
    @SerializedName("allow_messages")
    val allowMessages: Boolean,
    @SerializedName("hide_server")
    val hideServer: Boolean,
    @SerializedName("accept_friend_requests")
    val acceptFriendRequests: Boolean,
    @SerializedName("server_chat")
    val serverChat: Boolean,
) : AxochatPacket.S2C

data class S2CChatMessagePacket(
    @SerializedName("channel")
    val channel: String,
    @SerializedName("id")
    val id: Long,
    @SerializedName("time")
    val time: Long,
    @SerializedName("author")
    val author: ChatAuthor,
    @SerializedName("content")
    val content: String,
) : AxochatPacket.S2C

data class ChatFriend(
    @SerializedName("user")
    val user: ChatUserRef,
    @SerializedName("since")
    val since: Long,
    @SerializedName("online")
    val online: Boolean,
    @SerializedName("server")
    val server: String?,
)

data class S2CFriendsPacket(
    @SerializedName("friends")
    val friends: List<ChatFriend>?,
    @SerializedName("incoming")
    val incoming: List<ChatUserRef>?,
    @SerializedName("outgoing")
    val outgoing: List<ChatUserRef>?,
) : AxochatPacket.S2C

data class S2CPresencePacket(
    @SerializedName("user")
    val user: String,
    @SerializedName("online")
    val online: Boolean,
    @SerializedName("server")
    val server: String?,
) : AxochatPacket.S2C

data class S2CBlocksPacket(
    @SerializedName("users")
    val users: List<ChatUserRef>?,
) : AxochatPacket.S2C

data class ChatGroupMember(
    @SerializedName("user")
    val user: ChatUserRef,
    @SerializedName("role")
    val role: String,
    @SerializedName("online")
    val online: Boolean,
)

data class ChatGroup(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("role")
    val role: String,
    @SerializedName("members")
    val members: List<ChatGroupMember>?,
)

data class S2CGroupsPacket(
    @SerializedName("groups")
    val groups: List<ChatGroup>?,
) : AxochatPacket.S2C

data class PartyMember(
    @SerializedName("user")
    val user: ChatUserRef,
    @SerializedName("role")
    val role: String,
    @SerializedName("online")
    val online: Boolean,
    @SerializedName("muted")
    val muted: Boolean,
    @SerializedName("relation")
    val relation: String,
    @SerializedName("player")
    val player: ChatPlayer?,
    @SerializedName("server")
    val server: String?,
)

data class PartyInfo(
    @SerializedName("id")
    val id: String,
    @SerializedName("leader")
    val leader: String,
    @SerializedName("locked")
    val locked: Boolean,
    @SerializedName("pvp")
    val pvp: Boolean,
    @SerializedName("members")
    val members: List<PartyMember>?,
)

data class S2CPartyPacket(
    @SerializedName("party")
    val party: PartyInfo?,
) : AxochatPacket.S2C

data class S2CPartyInvitePacket(
    @SerializedName("party")
    val party: String,
    @SerializedName("from")
    val from: ChatUserRef,
    @SerializedName("expires")
    val expires: Long,
) : AxochatPacket.S2C

data class S2CPartyWarpPacket(
    @SerializedName("from")
    val from: ChatUserRef,
    @SerializedName("server")
    val server: String,
) : AxochatPacket.S2C

data class PartyPosition(
    @SerializedName("x")
    val x: Double,
    @SerializedName("y")
    val y: Double,
    @SerializedName("z")
    val z: Double,
    @SerializedName("yaw")
    val yaw: Float,
    @SerializedName("pitch")
    val pitch: Float,
    @SerializedName("dimension")
    val dimension: String,
)

data class S2CPartyMemberStatePacket(
    @SerializedName("member")
    val member: String,
    @SerializedName("position")
    val position: PartyPosition?,
    @SerializedName("status")
    val status: JsonObject?,
    @SerializedName("inventory")
    val inventory: JsonObject?,
) : AxochatPacket.S2C

data class S2CPunishedPacket(
    @SerializedName("kind")
    val kind: String,
    @SerializedName("reason")
    val reason: String,
    @SerializedName("expires")
    val expires: Long?,
) : AxochatPacket.S2C

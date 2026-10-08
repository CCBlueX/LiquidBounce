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


package net.ccbluex.liquidbounce.features.chat

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.ClientChatPacketEvent
import net.ccbluex.liquidbounce.event.events.ClientChatStateChange
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.chat.packet.ChatAuthor
import net.ccbluex.liquidbounce.features.chat.packet.ChatFriend
import net.ccbluex.liquidbounce.features.chat.packet.ChatGroup
import net.ccbluex.liquidbounce.features.chat.packet.ChatUserRef
import net.ccbluex.liquidbounce.features.chat.packet.S2CBlocksPacket
import net.ccbluex.liquidbounce.features.chat.packet.S2CChatMessagePacket
import net.ccbluex.liquidbounce.features.chat.packet.S2CFriendsPacket
import net.ccbluex.liquidbounce.features.chat.packet.S2CGroupsPacket
import net.ccbluex.liquidbounce.features.chat.packet.S2CPresencePacket
import net.ccbluex.liquidbounce.features.chat.packet.S2CSettingsPacket
import net.ccbluex.liquidbounce.features.chat.packet.S2CWelcomePacket
import net.ccbluex.liquidbounce.features.global.GlobalSettingsClientChat
import java.util.concurrent.ConcurrentHashMap

object ChatSession : EventListener {

    const val GLOBAL = "global"
    const val SERVER = "server"
    const val PARTY = "party"
    const val GROUP_PREFIX = "group/"
    const val USER_PREFIX = "user/"

    private const val REMEMBERED_MESSAGES = 256

    @Volatile
    var self: ChatAuthor? = null
        private set

    @Volatile
    var isStaff = false
        private set

    @Volatile
    var settings: S2CSettingsPacket? = null
        private set

    /**
     * Channel `.chat` writes to.
     */
    @Volatile
    var channel = GLOBAL

    @Volatile
    var friends: List<ChatFriend> = emptyList()
        private set

    @Volatile
    var incomingRequests: List<ChatUserRef> = emptyList()
        private set

    @Volatile
    var outgoingRequests: List<ChatUserRef> = emptyList()
        private set

    @Volatile
    var blocks: List<ChatUserRef> = emptyList()
        private set

    @Volatile
    var groups: List<ChatGroup> = emptyList()
        private set

    private val names = ConcurrentHashMap<String, String>()

    private val recentMessages = object : LinkedHashMap<Long, String>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, String>) = size > REMEMBERED_MESSAGES
    }

    fun isSelf(id: String) = self?.id == id

    fun nameOf(id: String): String = names[id] ?: id.take(8)

    fun channelName(channel: String): String {
        val id = channel.removePrefix(GROUP_PREFIX)
        return groups.firstOrNull { it.id == id }?.name ?: id.take(8)
    }

    fun findGroup(reference: String): ChatGroup? =
        groups.firstOrNull { it.id == reference } ?: groups.firstOrNull { it.name.equals(reference, true) }

    fun findUserId(reference: String): String? =
        reference.takeIf(names::containsKey) ?: names.entries.firstOrNull { it.value.equals(reference, true) }?.key

    fun knownNames(): Collection<String> = names.values.toSortedSet(String.CASE_INSENSITIVE_ORDER)

    fun findFriend(reference: String): ChatFriend? =
        friends.firstOrNull { it.user.id == reference } ?: friends.firstOrNull { it.user.name.equals(reference, true) }

    fun lastMessageOf(userId: String): Long? = synchronized(recentMessages) {
        recentMessages.entries.lastOrNull { it.value == userId }?.key
    }

    private fun remember(users: Iterable<ChatUserRef>) = users.forEach { names[it.id] = it.name }

    @Suppress("unused")
    private val packetHandler = handler<ClientChatPacketEvent> { event ->
        when (val packet = event.packet) {
            is S2CWelcomePacket -> {
                self = packet.user
                isStaff = packet.staff
                names[packet.user.id] = packet.user.name
            }

            is S2CSettingsPacket -> settings = packet
            is S2CChatMessagePacket -> {
                names[packet.author.id] = packet.author.name
                synchronized(recentMessages) {
                    recentMessages[packet.id] = packet.author.id
                }
            }

            is S2CFriendsPacket -> {
                friends = packet.friends.orEmpty()
                incomingRequests = packet.incoming.orEmpty()
                outgoingRequests = packet.outgoing.orEmpty()
                remember(friends.map(ChatFriend::user) + incomingRequests + outgoingRequests)
            }

            is S2CPresencePacket -> friends = friends.map { friend ->
                if (friend.user.id == packet.user) {
                    friend.copy(online = packet.online, server = packet.server)
                } else {
                    friend
                }
            }

            is S2CBlocksPacket -> {
                blocks = packet.users.orEmpty()
                remember(blocks)
            }

            is S2CGroupsPacket -> {
                groups = packet.groups.orEmpty()
                remember(groups.flatMap { it.members.orEmpty() }.map { it.user })
            }

            else -> {}
        }
    }

    @Suppress("unused")
    private val stateHandler = handler<ClientChatStateChange> { event ->
        if (event.state == ClientChatStateChange.State.DISCONNECTED) {
            self = null
            isStaff = false
            settings = null
            channel = GLOBAL
            friends = emptyList()
            incomingRequests = emptyList()
            outgoingRequests = emptyList()
            blocks = emptyList()
            groups = emptyList()
            synchronized(recentMessages) {
                recentMessages.clear()
            }
        }
    }

    override fun parent() = GlobalSettingsClientChat

}

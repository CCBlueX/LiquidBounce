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
import net.ccbluex.liquidbounce.features.chat.packet.ChatUserRef
import net.ccbluex.liquidbounce.features.chat.packet.S2CChatMessagePacket
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

    private val names = ConcurrentHashMap<String, String>()

    private val recentMessages = object : LinkedHashMap<Long, String>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, String>) = size > REMEMBERED_MESSAGES
    }

    fun isSelf(id: String) = self?.id == id

    fun nameOf(id: String): String = names[id] ?: id.take(8)

    fun channelName(channel: String): String = channel.removePrefix(GROUP_PREFIX).take(8)

    fun remember(user: ChatUserRef) {
        names[user.id] = user.name
    }

    fun lastMessageOf(userId: String): Long? = synchronized(recentMessages) {
        recentMessages.entries.lastOrNull { it.value == userId }?.key
    }

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
            synchronized(recentMessages) {
                recentMessages.clear()
            }
        }
    }

    override fun parent() = GlobalSettingsClientChat

}

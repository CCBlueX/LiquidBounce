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

import net.ccbluex.axochat.protocol.Clientbound
import net.ccbluex.axochat.user.Author
import net.ccbluex.axochat.user.UserRef
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.ClientChatPacketEvent
import net.ccbluex.liquidbounce.event.events.ClientChatStateChange
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.global.GlobalSettingsClientChat
import java.util.concurrent.ConcurrentHashMap

object ChatSession : EventListener {

    const val GLOBAL = "global"
    const val SERVER = "server"
    const val USER_PREFIX = "user/"
    const val WATCHED_PREFIX = "server/"

    private const val REMEMBERED_MESSAGES = 256

    @Volatile
    var self: Author? = null
        private set

    @Volatile
    var settings: Clientbound.Settings? = null
        private set

    private val users = ConcurrentHashMap<String, UserRef>()

    private val recentMessages = object : LinkedHashMap<Long, String>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, String>) = size > REMEMBERED_MESSAGES
    }

    fun isSelf(id: String) = self?.id == id

    val isAccount get() = self?.isAccount == true

    fun nameOf(id: String): String = users[id]?.name ?: id.take(8)

    fun userOf(id: String): UserRef? = users[id]

    fun findUser(reference: String): UserRef? =
        users[reference] ?: users.values.firstOrNull { it.name.equals(reference, true) }

    fun knownNames(accounts: Boolean = false): Collection<String> = users.values
        .filter { !isSelf(it.id) && (!accounts || it.isAccount) }
        .mapTo(sortedSetOf(String.CASE_INSENSITIVE_ORDER)) { it.name }

    fun lastMessageOf(userId: String): Long? = synchronized(recentMessages) {
        recentMessages.entries.lastOrNull { it.value == userId }?.key
    }

    @Suppress("unused")
    private val packetHandler = handler<ClientChatPacketEvent> { event ->
        when (val packet = event.packet) {
            is Clientbound.Welcome -> {
                self = packet.user
                users[packet.user.id] = packet.user.toUserRef()
            }

            is Clientbound.Settings -> settings = packet
            is Clientbound.ChatMessage -> {
                users[packet.author.id] = packet.author.toUserRef()
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
            settings = null
            synchronized(recentMessages) {
                recentMessages.clear()
            }
        }
    }

    override fun parent() = GlobalSettingsClientChat

}

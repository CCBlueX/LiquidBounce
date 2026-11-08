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
import java.util.concurrent.ConcurrentHashMap

object ChatSession {

    private const val REMEMBERED_MESSAGES = 256

    @Volatile
    var self: Author? = null
        private set

    private val users = ConcurrentHashMap<String, Author>()

    private val recentMessages = object : LinkedHashMap<Long, String>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, String>) = size > REMEMBERED_MESSAGES
    }

    private val removedMessages = object : LinkedHashMap<Long, Unit>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, Unit>) = size > REMEMBERED_MESSAGES
    }

    fun isSelf(id: String) = self?.id == id

    val isAccount get() = self?.isAccount == true

    fun nameOf(id: String): String = users[id]?.name ?: id.take(8)

    fun userOf(id: String): Author? = users[id]

    fun findUser(reference: String): Author? =
        users[reference] ?: users.values.firstOrNull { it.name.equals(reference, true) }

    fun knownNames(accounts: Boolean = false): Collection<String> = users.values
        .filter { !isSelf(it.id) && (!accounts || it.isAccount) }
        .mapTo(sortedSetOf(String.CASE_INSENSITIVE_ORDER)) { it.name }

    fun lastMessageOf(userId: String): Long? = synchronized(recentMessages) {
        recentMessages.entries.lastOrNull { it.value == userId }?.key
    }

    internal fun update(packet: Clientbound) {
        when (packet) {
            is Clientbound.Welcome -> {
                self = packet.user
                users[packet.user.id] = packet.user
            }

            is Clientbound.ChatMessage -> {
                users[packet.author.id] = packet.author
                synchronized(recentMessages) {
                    recentMessages[packet.id] = packet.author.id
                }
            }

            is Clientbound.MessageRemoved -> synchronized(removedMessages) {
                removedMessages[packet.id] = Unit
            }

            else -> {}
        }
    }

    fun isRemoved(id: Long): Boolean = synchronized(removedMessages) { id in removedMessages }

    internal fun reset() {
        self = null
        synchronized(recentMessages) {
            recentMessages.clear()
        }
        synchronized(removedMessages) {
            removedMessages.clear()
        }
    }

}

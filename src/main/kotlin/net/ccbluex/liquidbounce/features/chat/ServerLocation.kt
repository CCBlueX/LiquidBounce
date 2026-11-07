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

import net.ccbluex.axochat.protocol.Serverbound
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.ClientChatStateChange
import net.ccbluex.liquidbounce.event.events.DisconnectEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.global.GlobalSettingsClientChat
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.text.isSensitiveAddress

/**
 * Tells the chat server which Minecraft server the player is on, for the server channel.
 */
object ServerLocation : EventListener {

    @Volatile
    private var pending = true

    private var sent: Serverbound.Location? = null

    @Suppress("unused")
    private val worldHandler = handler<WorldChangeEvent> {
        pending = true
    }

    @Suppress("unused")
    private val disconnectHandler = handler<DisconnectEvent> {
        pending = true
    }

    @Suppress("unused")
    private val stateHandler = handler<ClientChatStateChange> { event ->
        when (event.state) {
            ClientChatStateChange.State.LOGGED_IN -> {
                sent = null
                pending = true
            }

            else -> {}
        }
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        val session = LiquidChatClient.session
        if (!pending || session == null || !LiquidChatClient.isLoggedIn || !session.isModern) {
            return@handler
        }

        pending = false
        val packet = Serverbound.Location(currentServer())
        if (packet != sent) {
            sent = packet
            session.send(packet)
        }
    }

    /**
     * A LiquidProxy route works as its owner's subscription, so it never leaves the client.
     */
    private fun currentServer(): String? {
        if (mc.level == null || mc.hasSingleplayerServer()) {
            return null
        }

        return mc.currentServer?.ip?.takeUnless { it.isSensitiveAddress() }
    }

    override fun parent() = GlobalSettingsClientChat

}

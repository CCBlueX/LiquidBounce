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

package net.ccbluex.liquidbounce.features.module.modules.player.autoqueue.trigger

import net.ccbluex.liquidbounce.event.events.ChatReceiveEvent
import net.ccbluex.liquidbounce.event.handler

/**
 * Can be used for different server that use paper to join a game
 */
object AutoQueueTriggerMessage : AutoQueueTrigger("Message") {

    override var isTriggered: Boolean = false
        get() = field.apply { field = false }

    private val text by text("Text", "Новая игра")

    @Suppress("unused")
    private val chatReceive = handler<ChatReceiveEvent> { event ->
        val message = event.message

        if (event.type != ChatReceiveEvent.ChatType.GAME_MESSAGE) {
            return@handler
        }

        if (event.message.contains(text)) {
            isTriggered = true
        }
    }

}

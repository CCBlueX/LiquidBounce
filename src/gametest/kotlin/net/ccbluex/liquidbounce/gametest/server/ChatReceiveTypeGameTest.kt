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
package net.ccbluex.liquidbounce.gametest.server

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.ChatReceiveEvent
import net.ccbluex.liquidbounce.gametest.harness.LiquidBounceGameTest
import java.util.concurrent.CopyOnWriteArrayList

private const val MESSAGE = "A message from the server"

/**
 * Server messages must reach chat handlers as game messages; AutoChatGame and AutoQueue only read those.
 */
class ChatReceiveTypeGameTest : LiquidBounceGameTest({
    val types = CopyOnWriteArrayList<ChatReceiveEvent.ChatType>()
    val listener = object : EventListener {}

    listener.on(ChatReceiveEvent::class.java) { event ->
        if (event.message == MESSAGE) {
            types += event.type
        }
    }.use {
        run("tellraw $playerName \"$MESSAGE\"")
        awaitClient("the message") { types.isNotEmpty() }
    }

    check(types.single() == ChatReceiveEvent.ChatType.GAME_MESSAGE) { "a server message arrived as $types" }
})
